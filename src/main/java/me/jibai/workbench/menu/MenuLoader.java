package me.jibai.workbench.menu;

import me.jibai.workbench.config.ValidationResult;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单文件加载器：把一个 menus/*.yml 解析成 {@link Menu}，并在解析过程中收集校验问题。
 *
 * <p>核心原则（对应文档易错点 6）：单个文件出错不会抛异常中断整体加载，
 * 而是记录到 {@link ValidationResult}，尽最大努力返回一个可用的 Menu。</p>
 *
 * @author 即白
 */
public class MenuLoader {

    /**
     * 加载单个菜单文件。
     *
     * @param file   菜单 YAML 文件
     * @param result 校验结果收集器
     * @return 解析出的 Menu；若文件完全无法读取则返回 null
     */
    public Menu load(File file, ValidationResult result) {
        String fileName = file.getName();
        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (Throwable t) {
            result.error(fileName, "?", null, "YAML 无法读取：" + t.getMessage(),
                    "检查缩进与语法，确保是合法 YAML 且为 UTF-8 编码");
            return null;
        }

        Menu menu = new Menu();
        menu.setSourceFile(fileName);

        // id：缺省用文件名（去扩展名）
        String id = yaml.getString("id", stripExt(fileName));
        menu.setId(id);

        // title
        menu.setTitle(yaml.getString("title", "&f菜单"));

        // rows 1-6
        int rows = yaml.getInt("rows", 3);
        if (rows < 1 || rows > 6) {
            result.error(fileName, id, null, "行数 rows=" + rows + " 超出范围",
                    "rows 必须在 1-6 之间，已回退为 3");
            rows = 3;
        }
        menu.setRows(rows);

        // permission
        String perm = yaml.getString("permission", "");
        menu.setPermission(perm == null || perm.isEmpty() ? null : perm);

        // sounds（写错不报错，仅记录警告，运行时安全跳过）
        menu.setOpenSound(emptyToNull(yaml.getString("open-sound")));
        menu.setCloseSound(emptyToNull(yaml.getString("close-sound")));

        menu.setAllowDrag(yaml.getBoolean("allow-drag", false));
        menu.setAllowTake(yaml.getBoolean("allow-take", false));

        // fill-item
        ConfigurationSection fill = yaml.getConfigurationSection("fill-item");
        if (fill != null) {
            String fillMat = fill.getString("material", "");
            Material mat = matchMaterial(fillMat);
            if (mat == null) {
                result.warn(fileName, id, "fill-item", "填充物 material 无效：" + fillMat,
                        "使用合法 Material 名，已忽略背景填充");
            } else {
                menu.setFillMaterial(mat);
                menu.setFillName(fill.getString("name", " "));
            }
        }

        // buttons
        ConfigurationSection buttons = yaml.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String key : buttons.getKeys(false)) {
                ConfigurationSection bs = buttons.getConfigurationSection(key);
                if (bs == null) {
                    continue;
                }
                MenuButton button = parseButton(fileName, id, key, bs, menu.getSize(), result);
                if (button != null && !button.getSlots().isEmpty()) {
                    menu.getButtons().put(key, button);
                }
            }
        } else {
            result.warn(fileName, id, null, "菜单没有任何按钮", "在 buttons 段落下添加按钮");
        }

        return menu;
    }

    private MenuButton parseButton(String file, String menuId, String buttonId,
                                   ConfigurationSection bs, int menuSize, ValidationResult result) {
        MenuButton b = new MenuButton();
        b.setId(buttonId);

        // slot / slots
        List<Integer> slots = new ArrayList<>();
        if (bs.isInt("slot")) {
            slots.add(bs.getInt("slot"));
        }
        if (bs.isList("slots")) {
            slots.addAll(bs.getIntegerList("slots"));
        }
        if (slots.isEmpty()) {
            result.error(file, menuId, buttonId, "按钮没有配置 slot / slots",
                    "至少配置一个 slot（0 到 " + (menuSize - 1) + "）");
            return null;
        }
        for (int s : slots) {
            if (s < 0 || s >= menuSize) {
                result.error(file, menuId, buttonId, "slot=" + s + " 超出菜单范围(0-" + (menuSize - 1) + ")",
                        "调整 slot 或增大菜单 rows");
            } else {
                b.getSlots().add(s);
            }
        }
        if (b.getSlots().isEmpty()) {
            return null;
        }

        // material
        String matName = bs.getString("material", "STONE");
        Material mat = matchMaterial(matName);
        if (mat == null) {
            result.error(file, menuId, buttonId, "material 无效：" + matName,
                    "使用合法 Material 名（不同版本名称可能不同），已回退为 STONE");
            mat = Material.STONE;
        }
        b.setMaterial(mat);

        b.setName(bs.getString("name"));
        b.setLore(bs.getStringList("lore"));
        b.setLoreLocked(bs.getStringList("lore-locked"));
        b.setAmount(Math.max(1, bs.getInt("amount", 1)));
        b.setGlow(bs.getBoolean("glow", false));
        b.setCustomModelData(bs.getInt("custom-model-data", 0));

        String perm = bs.getString("permission", "");
        b.setPermission(perm == null || perm.isEmpty() ? null : perm);

        b.getViewConditions().addAll(bs.getStringList("view-condition"));
        b.getClickConditions().addAll(bs.getStringList("click-condition"));

        b.getDefaultActions().addAll(bs.getStringList("actions"));
        b.getLeftActions().addAll(bs.getStringList("left-actions"));
        b.getRightActions().addAll(bs.getStringList("right-actions"));
        b.getShiftLeftActions().addAll(bs.getStringList("shift-left-actions"));
        b.getShiftRightActions().addAll(bs.getStringList("shift-right-actions"));

        // shop 段
        ConfigurationSection shopSec = bs.getConfigurationSection("shop");
        if (shopSec != null) {
            b.setShop(parseShop(file, menuId, buttonId, shopSec, result));
        }

        // reward 段
        ConfigurationSection rewardSec = bs.getConfigurationSection("reward");
        if (rewardSec != null) {
            b.setReward(parseReward(file, menuId, buttonId, rewardSec, result));
        }

        return b;
    }

    private ShopData parseShop(String file, String menuId, String buttonId,
                               ConfigurationSection s, ValidationResult result) {
        ShopData shop = new ShopData();
        double buy = s.getDouble("buy-price", -1);
        double sell = s.getDouble("sell-price", -1);
        if (buy < 0 && sell < 0) {
            result.warn(file, menuId, buttonId, "商店按钮既不能买也不能卖",
                    "至少配置 buy-price 或 sell-price");
        }
        if (buy < 0 && s.contains("buy-price")) {
            result.error(file, menuId, buttonId, "buy-price 为负数：" + buy, "价格必须 >=0");
            buy = -1;
        }
        if (sell < 0 && s.contains("sell-price")) {
            result.error(file, menuId, buttonId, "sell-price 为负数：" + sell, "价格必须 >=0");
            sell = -1;
        }
        shop.setBuyPrice(buy);
        shop.setSellPrice(sell);
        shop.setGiveItem(emptyToNull(s.getString("give-item")));
        shop.getCommands().addAll(s.getStringList("commands"));
        shop.setPermission(emptyToNull(s.getString("permission")));
        shop.setOnce(s.getBoolean("once", false));
        int dailyLimit = s.getInt("daily-limit", 0);
        if (dailyLimit < 0) {
            result.error(file, menuId, buttonId, "daily-limit 为负数", "限购次数必须 >=0，0 表示不限");
            dailyLimit = 0;
        }
        shop.setDailyLimit(dailyLimit);
        int cd = s.getInt("cooldown-sec", 0);
        if (cd < 0) {
            result.error(file, menuId, buttonId, "cooldown-sec 为负数", "冷却秒数必须 >=0");
            cd = 0;
        }
        shop.setCooldownSec(cd);
        shop.setStock(s.getInt("stock", -1));
        shop.setConfirm(s.getBoolean("confirm", false));
        return shop;
    }

    private RewardData parseReward(String file, String menuId, String buttonId,
                                   ConfigurationSection s, ValidationResult result) {
        RewardData reward = new RewardData();
        String key = s.getString("key", "");
        if (key == null || key.isEmpty()) {
            result.error(file, menuId, buttonId, "奖励缺少 key", "为奖励设置唯一 key");
        }
        reward.setKey(key);
        RewardData.Type type = RewardData.Type.from(s.getString("type"));
        if (type == RewardData.Type.UNKNOWN) {
            result.error(file, menuId, buttonId, "奖励 type 无效：" + s.getString("type"),
                    "type 必须为 daily / weekly / once / playtime");
        }
        reward.setType(type);
        reward.setGiveItem(emptyToNull(s.getString("give-item")));
        reward.getCommands().addAll(s.getStringList("commands"));
        reward.setPermission(emptyToNull(s.getString("permission")));
        reward.setPlaytimeMin(s.getInt("playtime-min", 0));
        return reward;
    }

    // ===== 工具 =====

    /** 兼容大小写与旧名，匹配 Material。 */
    public static Material matchMaterial(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Material m = Material.matchMaterial(name);
        if (m == null) {
            try {
                m = Material.valueOf(name.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                m = null;
            }
        }
        return m;
    }

    private static String stripExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
