package me.jibai.workbench.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

/**
 * 启动 / 关闭横幅工具。
 *
 * <p>横幅通过控制台 {@link ConsoleCommandSender} 发送，使用 Bukkit {@link org.bukkit.ChatColor}
 * 上色，避免使用复杂 ANSI 控制码导致部分控制台乱码（详见即白踩坑记录）。</p>
 *
 * @author 即白
 */
public final class BannerUtil {

    private BannerUtil() {
    }

    /**
     * 打印彩色启动横幅。
     *
     * @param version    版本号
     * @param serverCore 服务端核心名（Paper / Spigot / Purpur ...）
     * @param menuCount  已加载菜单数
     * @param tplCount   已加载模板数
     * @param vault      Vault 状态
     * @param papi       PlaceholderAPI 状态
     * @param luckperms  LuckPerms 状态
     */
    public static void printEnable(String version, String serverCore, int menuCount, int tplCount,
                                   boolean vault, boolean papi, boolean luckperms) {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        send(c, "&b&m----------------------------------------------");
        send(c, "&f  &b&lJibai&f&lWorkbench   &7即白服务器交互工作台");
        send(c, "");
        send(c, "&7  作者    &f即白");
        send(c, "&7  邮箱    &f jibai0517@gamil.com");
        send(c, "&7  版本    &f" + version);
        send(c, "&7  核心    &f" + serverCore);
        send(c, "&7  菜单    &a" + menuCount + " &7个   模板 &a" + tplCount + " &7个");
        send(c, "&7  依赖    Vault " + status(vault)
                + " &7| PAPI " + status(papi)
                + " &7| LuckPerms " + status(luckperms));
        send(c, "");
        send(c, "&a  ✔ 插件已启用，祝你玩得愉快！输入 /jwb help 查看指令。");
        send(c, "&b&m----------------------------------------------");
    }

    /** 打印安全关闭提示。 */
    public static void printDisable() {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        send(c, "&b&m----------------------------------------------");
        send(c, "&e  JibaiWorkbench 正在安全关闭...");
        send(c, "&7  已保存数据、清理菜单会话、取消任务。");
        send(c, "&a  ✔ 已安全关闭，感谢使用！ —— 即白");
        send(c, "&b&m----------------------------------------------");
    }

    private static String status(boolean ok) {
        return ok ? "&a已启用" : "&c未启用";
    }

    private static void send(ConsoleCommandSender c, String msg) {
        c.sendMessage(ColorUtil.colorize(msg));
    }
}
