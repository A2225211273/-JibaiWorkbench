package me.jibai.workbench.menu;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.config.ValidationResult;
import me.jibai.workbench.util.ItemBuilder;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 菜单中枢：加载 menus/ 下所有菜单、渲染并为玩家打开、管理会话与返回历史。
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li>用 {@link MenuSession}（InventoryHolder）识别菜单归属，不靠标题（易错点 3/13）；</li>
 *   <li>每次打开新建会话，多人互不干扰（易错点 4）；</li>
 *   <li>reload 时先关闭所有在线玩家的本插件菜单并清空缓存（易错点 5/14）；</li>
 *   <li>返回历史用每玩家一个栈，限制深度防止死循环/溢出（易错点 19）。</li>
 * </ul>
 *
 * @author 即白
 */
public class MenuManager {

    private final JibaiWorkbenchPlugin plugin;
    private final MenuLoader loader = new MenuLoader();

    /** 已加载菜单：id -> Menu。 */
    private final Map<String, Menu> menus = new LinkedHashMap<>();

    /** 每玩家的返回历史栈（菜单 id）。 */
    private final Map<UUID, Deque<String>> history = new HashMap<>();

    /** 返回历史最大深度，超过则丢弃最早的，防止无限增长。 */
    private static final int MAX_HISTORY = 16;

    public MenuManager(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 从 menus/ 目录加载全部菜单到正式缓存（会替换当前 menus）。
     * 仅在启动 / reload 时调用。
     *
     * @param result 校验结果收集器
     */
    public void loadAll(ValidationResult result) {
        Map<String, Menu> loaded = new LinkedHashMap<>();
        scanInto(loaded, result, true);
        menus.clear();
        menus.putAll(loaded);
        LogUtil.info("共加载 " + menus.size() + " 个菜单。");
    }

    /**
     * 只读校验：扫描并校验 menus/ 下所有文件，<b>不改动</b>当前已加载的 menus 缓存，
     * 也不影响玩家正在打开的菜单（对应 /jwb validate 的副作用修复）。
     *
     * @param result 校验结果收集器
     * @return 本次扫描到的合法菜单数量
     */
    public int validateAll(ValidationResult result) {
        Map<String, Menu> preview = new LinkedHashMap<>();
        scanInto(preview, result, false);
        return preview.size();
    }

    /**
     * 扫描 menus/ 目录并把合法菜单放入 target。
     *
     * @param target        接收菜单的映射（正式缓存或临时预览映射）
     * @param result        校验结果收集器
     * @param allowGenerate 目录为空时是否允许释放默认菜单（仅正式加载允许，只读校验不生成文件）
     */
    private void scanInto(Map<String, Menu> target, ValidationResult result, boolean allowGenerate) {
        File dir = new File(plugin.getDataFolder(), "menus");
        if (!dir.exists() && !dir.mkdirs()) {
            LogUtil.warn("无法创建 menus 目录。");
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if ((files == null || files.length == 0) && allowGenerate) {
            LogUtil.info("menus/ 为空，正在释放默认菜单（从模板生成 main / shop / reward / vip 等）。");
            plugin.templates().generateDefaultMenus();
            files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        }

        if (files == null) {
            return;
        }
        for (File f : files) {
            Menu menu = loader.load(f, result);
            if (menu == null) {
                continue;
            }
            if (target.containsKey(menu.getId())) {
                result.error(f.getName(), menu.getId(), null, "菜单 ID 重复：" + menu.getId(),
                        "确保每个菜单的 id 唯一");
                continue;
            }
            target.put(menu.getId(), menu);
            LogUtil.debug("已加载菜单：" + menu.getId() + "（" + f.getName() + "）");
        }

        // 交叉校验：open 动作的目标菜单是否存在（在本次扫描集合内判断）
        crossValidate(target, result);
    }

    /** 检查所有 open 动作指向的菜单是否在给定集合内存在（易错点相关）。 */
    private void crossValidate(Map<String, Menu> scope, ValidationResult result) {
        for (Menu menu : scope.values()) {
            for (MenuButton b : menu.getButtons().values()) {
                for (String action : allActions(b)) {
                    String lower = action.toLowerCase().trim();
                    if (lower.startsWith("open:")) {
                        String openTarget = action.substring(action.indexOf(':') + 1).trim();
                        if (!scope.containsKey(openTarget)) {
                            result.warn(menu.getSourceFile(), menu.getId(), b.getId(),
                                    "打开的目标菜单不存在：" + openTarget,
                                    "创建该菜单或修正 open 目标");
                        }
                    }
                }
            }
        }
    }

    private Iterable<String> allActions(MenuButton b) {
        java.util.List<String> all = new java.util.ArrayList<>();
        all.addAll(b.getDefaultActions());
        all.addAll(b.getLeftActions());
        all.addAll(b.getRightActions());
        all.addAll(b.getShiftLeftActions());
        all.addAll(b.getShiftRightActions());
        return all;
    }

    public Menu getMenu(String id) {
        return id == null ? null : menus.get(id);
    }

    public boolean exists(String id) {
        return menus.containsKey(id);
    }

    public Map<String, Menu> getMenus() {
        return menus;
    }

    // ===== 打开菜单 =====

    /**
     * 为玩家打开菜单（记录返回历史）。
     *
     * @return 是否成功打开
     */
    public boolean open(Player player, String menuId) {
        return open(player, menuId, true);
    }

    /**
     * 为玩家打开菜单。
     *
     * @param recordHistory 是否把当前菜单压入返回栈（back 动作时传 false）
     */
    public boolean open(Player player, String menuId, boolean recordHistory) {
        Menu menu = getMenu(menuId);
        if (menu == null) {
            return false;
        }
        // 打开权限
        if (menu.getPermission() != null && !player.hasPermission(menu.getPermission())) {
            plugin.messages().send(player, "menu-no-open-permission");
            return false;
        }

        if (recordHistory) {
            pushHistory(player, menuId);
        }

        MenuSession session = new MenuSession(player, menu);
        String title = plugin.placeholders().resolve(player, menu.getTitle());
        // 标题长度保护（部分客户端上限 32 字符，超出截断避免异常）
        if (title.length() > 60) {
            title = title.substring(0, 60);
        }
        Inventory inv = Bukkit.createInventory(session, menu.getSize(), title);
        session.setInventory(inv);

        render(player, menu, session, inv);

        player.openInventory(inv);
        playSound(player, menu.getOpenSound());
        return true;
    }

    /**
     * 渲染菜单内容到 inventory，并把槽位->按钮映射写入会话。
     * 显示条件不满足的按钮不渲染；权限不足的按钮显示 lore-locked（若配置）或直接不渲染。
     */
    public void render(Player player, Menu menu, MenuSession session, Inventory inv) {
        session.getSlotButtons().clear();
        inv.clear();

        // 背景填充
        if (menu.getFillMaterial() != null) {
            ItemStack fill = ItemBuilder.of(menu.getFillMaterial(), 1)
                    .name(menu.getFillName())
                    .hideAttributes()
                    .build();
            for (int i = 0; i < menu.getSize(); i++) {
                inv.setItem(i, fill);
            }
        }

        for (MenuButton b : menu.getButtons().values()) {
            // 显示条件
            if (!plugin.conditions().matchesAll(player, b.getViewConditions())) {
                continue;
            }
            boolean locked = false;
            // 权限：无权限时，若配置了 lore-locked 则显示锁定态，否则隐藏
            if (b.getPermission() != null && !player.hasPermission(b.getPermission())) {
                if (b.getLoreLocked() == null || b.getLoreLocked().isEmpty()) {
                    continue;
                }
                locked = true;
            }

            ItemStack item = buildButtonItem(player, b, locked);
            for (int slot : b.getSlots()) {
                if (slot >= 0 && slot < menu.getSize()) {
                    inv.setItem(slot, item);
                    session.getSlotButtons().put(slot, b);
                }
            }
        }
    }

    private ItemStack buildButtonItem(Player player, MenuButton b, boolean locked) {
        java.util.List<String> loreSource = locked ? b.getLoreLocked() : b.getLore();
        java.util.List<String> resolvedLore = new java.util.ArrayList<>();
        if (loreSource != null) {
            for (String line : loreSource) {
                resolvedLore.add(plugin.placeholders().resolve(player, line));
            }
        }
        String name = b.getName() == null ? null : plugin.placeholders().resolve(player, b.getName());
        return ItemBuilder.of(b.getMaterial(), b.getAmount())
                .name(name)
                .lore(resolvedLore)
                .glow(b.isGlow())
                .customModelData(b.getCustomModelData())
                .hideAttributes()
                .build();
    }

    /** 刷新玩家当前打开的本插件菜单（用于领奖后更新状态）。 */
    public void refresh(Player player) {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof MenuSession) {
            MenuSession session = (MenuSession) holder;
            render(player, session.getMenu(), session, session.getInventory());
        }
    }

    // ===== 返回历史 =====

    private void pushHistory(Player player, String menuId) {
        Deque<String> stack = history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        // 避免连续重复压栈
        if (stack.peek() == null || !stack.peek().equals(menuId)) {
            stack.push(menuId);
        }
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    /**
     * 返回上一个菜单。当前菜单先出栈，再打开新的栈顶。
     *
     * @return 是否成功返回
     */
    public boolean back(Player player) {
        Deque<String> stack = history.get(player.getUniqueId());
        if (stack == null || stack.size() < 2) {
            return false;
        }
        stack.pop(); // 弹出当前
        String prev = stack.peek();
        if (prev == null) {
            return false;
        }
        return open(player, prev, false);
    }

    public void clearHistory(UUID player) {
        history.remove(player);
    }

    // ===== reload / 清理 =====

    /** 关闭所有在线玩家打开的本插件菜单，并清理会话缓存（reload / disable 时调用）。 */
    public void closeAllAndClear() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof MenuSession) {
                p.closeInventory();
            }
        }
        history.clear();
    }

    // ===== 音效 =====

    /** 安全播放音效：名称写错不报错，仅 debug 记录（易错点 16）。 */
    public void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            LogUtil.debug("未知音效名：" + soundName);
        }
    }
}
