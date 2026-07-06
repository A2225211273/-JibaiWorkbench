package me.jibai.workbench.hook;

import me.jibai.workbench.util.LogUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * LuckPerms 对接（可选）。软依赖：未安装时 {@link #getPrimaryGroup(Player)} 退化为
 * 「default」或空，权限判断仍走 Bukkit 原生 {@link Player#hasPermission(String)}。
 *
 * @author 即白
 */
public class LuckPermsHook {

    private LuckPerms api;
    private boolean available;

    public void setup(boolean wantEnabled) {
        this.api = null;
        this.available = false;
        if (!wantEnabled) {
            LogUtil.debug("config 中已关闭 LuckPerms 对接。");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            LogUtil.debug("未检测到 LuckPerms，权限组显示退化为默认值。");
            return;
        }
        try {
            this.api = LuckPermsProvider.get();
            this.available = this.api != null;
            if (available) {
                LogUtil.info("已挂接 LuckPerms，可显示玩家权限组。");
            }
        } catch (Throwable t) {
            LogUtil.warn("挂接 LuckPerms 失败：" + t.getMessage());
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return available && api != null;
    }

    /** 取玩家主要权限组名，不可用时返回「default」。 */
    public String getPrimaryGroup(Player player) {
        if (!isAvailable()) {
            return "default";
        }
        try {
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return user.getPrimaryGroup();
            }
        } catch (Throwable t) {
            LogUtil.debug("读取 LuckPerms 权限组失败：" + t.getMessage());
        }
        return "default";
    }
}
