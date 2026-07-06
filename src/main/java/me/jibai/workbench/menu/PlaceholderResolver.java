package me.jibai.workbench.menu;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 菜单文本变量解析：先替换内置 {xxx} 变量，再交给 PlaceholderAPI 解析 %xxx% 变量，
 * 最后上色。PAPI 未安装时 %xxx% 原样保留（对应文档易错点 7）。
 *
 * <p>内置变量：{player} {world} {online} {max_players} {balance} {group}
 * {date} {time}。</p>
 *
 * @author 即白
 */
public class PlaceholderResolver {

    private final JibaiWorkbenchPlugin plugin;

    public PlaceholderResolver(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 解析并上色一段文本。
     *
     * @param player 上下文玩家
     * @param text   原始文本
     * @return 已解析、已上色的文本
     */
    public String resolve(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String s = text;
        // 内置变量
        if (s.indexOf('{') >= 0) {
            s = s.replace("{player}", player.getName())
                    .replace("{world}", player.getWorld() == null ? "" : player.getWorld().getName())
                    .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()))
                    .replace("{balance}", plugin.hooks().vault().isAvailable()
                            ? plugin.hooks().vault().format(plugin.hooks().vault().getBalance(player))
                            : "N/A")
                    .replace("{group}", plugin.hooks().luckPerms().getPrimaryGroup(player))
                    .replace("{date}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                    .replace("{time}", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        }
        // PAPI 变量（%xxx%）
        s = plugin.hooks().papi().apply(player, s);
        // 上色
        return ColorUtil.colorize(s);
    }
}
