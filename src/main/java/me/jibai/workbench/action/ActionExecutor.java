package me.jibai.workbench.action;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.config.MessageManager;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.util.ColorUtil;
import me.jibai.workbench.util.LogUtil;
import me.jibai.workbench.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 点击动作执行器：按顺序执行一个按钮的动作列表。
 *
 * <p>安全策略：</p>
 * <ul>
 *   <li>控制台指令受 config {@code actions.allow-console-command} 开关控制（易错点 11）；</li>
 *   <li>跳服受 {@code actions.allow-server-jump} 开关控制；</li>
 *   <li>经济动作在 Vault 不可用时安全跳过并提示（易错点 8）；</li>
 *   <li>所有动作在主线程执行（由调用方 InventoryClickEvent 保证），不碰异步。</li>
 * </ul>
 *
 * @author 即白
 */
public class ActionExecutor {

    private final JibaiWorkbenchPlugin plugin;

    public ActionExecutor(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /** 根据点击类型选择动作列表并执行。返回是否有任一动作执行。 */
    public void runForClick(Player player, MenuButton button, boolean left, boolean shift) {
        List<String> actions;
        if (shift && left && !button.getShiftLeftActions().isEmpty()) {
            actions = button.getShiftLeftActions();
        } else if (shift && !left && !button.getShiftRightActions().isEmpty()) {
            actions = button.getShiftRightActions();
        } else if (!shift && left && !button.getLeftActions().isEmpty()) {
            actions = button.getLeftActions();
        } else if (!shift && !left && !button.getRightActions().isEmpty()) {
            actions = button.getRightActions();
        } else {
            actions = button.getDefaultActions();
        }
        run(player, actions);
    }

    /** 顺序执行一组动作。 */
    public void run(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        for (String raw : actions) {
            try {
                if (!execute(player, raw)) {
                    // 返回 false 表示需要中断后续动作（如 close / back / open）
                    break;
                }
            } catch (Throwable t) {
                LogUtil.warn("执行动作出错：" + raw + " -> " + t.getMessage());
                plugin.messages().send(player, "action-failed");
            }
        }
    }

    /**
     * 执行单个动作。
     *
     * @return true=继续执行后续动作；false=中断（用于会切换/关闭界面的动作）
     */
    private boolean execute(Player player, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String type;
        String arg;
        int idx = raw.indexOf(':');
        if (idx < 0) {
            type = raw.trim().toLowerCase();
            arg = "";
        } else {
            type = raw.substring(0, idx).trim().toLowerCase();
            arg = raw.substring(idx + 1).trim();
        }

        switch (type) {
            case "open":
                plugin.menus().open(player, arg);
                return false; // 切换了界面，停止后续
            case "close":
                player.closeInventory();
                return false;
            case "back":
                if (!plugin.menus().back(player)) {
                    player.closeInventory();
                }
                return false;
            case "player-command":
                player.performCommand(applyVars(player, arg));
                return true;
            case "console-command":
                if (!plugin.config().isAllowConsoleCommand()) {
                    plugin.messages().send(player, "console-command-disabled");
                    LogUtil.warn("控制台指令动作被禁用，已跳过：" + arg);
                    return true;
                }
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        applyVars(player, arg));
                return true;
            case "message":
                player.sendMessage(plugin.placeholders().resolve(player, arg));
                return true;
            case "title":
                sendTitle(player, arg);
                return true;
            case "actionbar":
                sendActionBar(player, plugin.placeholders().resolve(player, arg));
                return true;
            case "sound":
                playSound(player, arg);
                return true;
            case "take-money":
                return handleTakeMoney(player, arg);
            case "give-money":
                return handleGiveMoney(player, arg);
            case "give-item":
                // 作为独立动作时，发放失败仅记录警告并提示，不中断后续动作
                if (!giveItem(player, arg)) {
                    LogUtil.warn("give-item 动作发放失败：" + arg);
                    plugin.messages().send(player, "action-failed");
                }
                return true;
            case "teleport":
                teleport(player, arg);
                return true;
            case "server":
                jumpServer(player, arg);
                return true;
            case "cooldown":
                setCooldown(player, arg);
                return true;
            case "mark-reward":
                if (!arg.isEmpty()) {
                    if (!plugin.storage().setRewardClaimed(player.getUniqueId(), arg, System.currentTimeMillis())) {
                        LogUtil.warn("mark-reward 记录保存失败：" + arg);
                    }
                }
                return true;
            default:
                LogUtil.debug("未知动作类型：" + type);
                return true;
        }
    }

    private String applyVars(Player player, String s) {
        return s.replace("{player}", player.getName());
    }

    private void sendTitle(Player player, String arg) {
        String main;
        String sub = "";
        int bar = arg.indexOf('|');
        if (bar >= 0) {
            main = arg.substring(0, bar);
            sub = arg.substring(bar + 1);
        } else {
            main = arg;
        }
        player.sendTitle(
                plugin.placeholders().resolve(player, main),
                plugin.placeholders().resolve(player, sub),
                10, 60, 10);
    }

    /** 发送 ActionBar，使用 Spigot 通用 API，兼容全核心。 */
    private void sendActionBar(Player player, String text) {
        try {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
        } catch (Throwable t) {
            // 极老版本无 ActionBar，退化为普通消息
            player.sendMessage(text);
        }
    }

    private void playSound(Player player, String arg) {
        // 支持 名称:音量:音调
        String[] parts = arg.split(":");
        String name = parts[0].trim();
        float volume = 1.0f;
        float pitch = 1.0f;
        try {
            if (parts.length >= 2) {
                volume = Float.parseFloat(parts[1].trim());
            }
            if (parts.length >= 3) {
                pitch = Float.parseFloat(parts[2].trim());
            }
        } catch (NumberFormatException ignored) {
        }
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(name.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            LogUtil.debug("未知音效：" + name);
        }
    }

    private boolean handleTakeMoney(Player player, String arg) {
        if (!plugin.hooks().vault().isAvailable()) {
            plugin.messages().send(player, "economy-unavailable");
            return true;
        }
        try {
            double amount = Double.parseDouble(arg);
            if (!plugin.hooks().vault().withdraw(player, amount)) {
                plugin.messages().send(player, "economy-not-enough", MessageManager.vars(
                        "cost", plugin.hooks().vault().format(amount),
                        "balance", plugin.hooks().vault().format(plugin.hooks().vault().getBalance(player))));
            }
        } catch (NumberFormatException e) {
            LogUtil.debug("take-money 参数非法：" + arg);
        }
        return true;
    }

    private boolean handleGiveMoney(Player player, String arg) {
        if (!plugin.hooks().vault().isAvailable()) {
            plugin.messages().send(player, "economy-unavailable");
            return true;
        }
        try {
            double amount = Double.parseDouble(arg);
            plugin.hooks().vault().deposit(player, amount);
        } catch (NumberFormatException e) {
            LogUtil.debug("give-money 参数非法：" + arg);
        }
        return true;
    }

    /**
     * 给予物品 材质:数量。背包满时掉落到脚下，不丢失。
     *
     * @return true=已成功发放（含溢出掉落）；false=材质无效 / 数量非法 / 完全无法发放
     */
    public boolean giveItem(Player player, String arg) {
        if (arg == null || arg.trim().isEmpty()) {
            LogUtil.warn("give-item 参数为空");
            return false;
        }
        String[] parts = arg.split(":");
        Material mat = me.jibai.workbench.menu.MenuLoader.matchMaterial(parts[0].trim());
        if (mat == null || mat == Material.AIR) {
            LogUtil.warn("give-item 材质无效：" + parts[0]);
            return false;
        }
        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                LogUtil.warn("give-item 数量非法：" + parts[1]);
                return false;
            }
            if (amount <= 0) {
                LogUtil.warn("give-item 数量必须为正数：" + amount);
                return false;
            }
        }
        try {
            ItemStack stack = new ItemStack(mat, amount);
            java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
            for (ItemStack overflow : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            return true;
        } catch (Throwable t) {
            LogUtil.warn("give-item 发放异常：" + arg + " -> " + t.getMessage());
            return false;
        }
    }

    private void teleport(Player player, String arg) {
        // 世界,x,y,z[,yaw,pitch]
        String[] parts = arg.split(",");
        if (parts.length < 4) {
            LogUtil.debug("teleport 参数不足：" + arg);
            return;
        }
        World world = plugin.getServer().getWorld(parts[0].trim());
        if (world == null) {
            LogUtil.debug("teleport 世界不存在：" + parts[0]);
            plugin.messages().send(player, "action-failed");
            return;
        }
        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : player.getLocation().getYaw();
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5].trim()) : player.getLocation().getPitch();
            player.teleport(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException e) {
            LogUtil.debug("teleport 坐标非法：" + arg);
        }
    }

    /** 跳服（BungeeCord / Velocity）。受开关控制，未启用则提示。 */
    private void jumpServer(Player player, String serverName) {
        if (!plugin.config().isAllowServerJump()) {
            plugin.messages().send(player, "server-jump-disabled");
            return;
        }
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            LogUtil.warn("跳服失败：" + e.getMessage());
        }
    }

    private void setCooldown(Player player, String arg) {
        // 键:秒
        int colon = arg.lastIndexOf(':');
        if (colon < 0) {
            LogUtil.debug("cooldown 参数格式应为 键:秒 -> " + arg);
            return;
        }
        String key = arg.substring(0, colon).trim();
        try {
            long seconds = Long.parseLong(arg.substring(colon + 1).trim());
            long until = System.currentTimeMillis() + seconds * 1000L;
            if (!plugin.storage().setCooldownUntil(player.getUniqueId(), key, until)) {
                LogUtil.warn("cooldown 记录保存失败：" + arg);
            }
        } catch (NumberFormatException e) {
            LogUtil.debug("cooldown 秒数非法：" + arg);
        }
    }
}
