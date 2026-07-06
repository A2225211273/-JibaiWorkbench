package me.jibai.workbench.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI 对接。软依赖：未安装时 {@link #apply(Player, String)} 原样返回文本，
 * 绝不调用 PAPI API 导致报错（对应文档易错点 7）。
 *
 * @author 即白
 */
public class PapiHook {

    private boolean available;

    public void setup(boolean wantEnabled) {
        this.available = false;
        if (!wantEnabled) {
            LogUtil.debug("config 中已关闭 PlaceholderAPI 对接。");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.available = true;
            LogUtil.info("已挂接 PlaceholderAPI，菜单文本将解析 PAPI 变量。");
        } else {
            LogUtil.debug("未检测到 PlaceholderAPI，PAPI 变量将保持原文本。");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 解析文本中的 PAPI 变量。未安装 PAPI 时原样返回。
     *
     * @param player 上下文玩家，可为 null（则只解析无玩家变量）
     * @param text   原始文本
     * @return 解析后的文本
     */
    public String apply(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!available) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            LogUtil.debug("PAPI 解析失败，返回原文本：" + t.getMessage());
            return text;
        }
    }
}
