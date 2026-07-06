package me.jibai.workbench.condition;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 条件判断器。用于按钮显示条件（view-condition）与点击条件（click-condition）。
 *
 * <p>支持的条件（字符串 "类型: 参数"）：</p>
 * <ul>
 *   <li>permission: &lt;节点&gt;      —— 玩家拥有该权限</li>
 *   <li>not-permission: &lt;节点&gt;  —— 玩家不拥有该权限</li>
 *   <li>money: &lt;数量&gt;           —— 金币不少于（需 Vault）</li>
 *   <li>cooldown: &lt;键&gt;          —— 指定冷却已结束</li>
 *   <li>reward-unclaimed: &lt;键&gt;  —— 奖励尚未领取</li>
 * </ul>
 *
 * <p>未知条件类型默认判为 true（不阻挡），并记录 debug 日志，避免误伤。</p>
 *
 * @author 即白
 */
public class ConditionEvaluator {

    private final JibaiWorkbenchPlugin plugin;

    public ConditionEvaluator(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /** 所有条件都满足才返回 true（AND 语义）。空列表视为满足。 */
    public boolean matchesAll(Player player, List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (String raw : conditions) {
            if (!matches(player, raw)) {
                return false;
            }
        }
        return true;
    }

    /** 判断单条条件。 */
    public boolean matches(Player player, String raw) {
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

        UUID uuid = player.getUniqueId();
        switch (type) {
            case "permission":
                return player.hasPermission(arg);
            case "not-permission":
                return !player.hasPermission(arg);
            case "money":
                try {
                    double need = Double.parseDouble(arg);
                    return plugin.hooks().vault().isAvailable()
                            && plugin.hooks().vault().has(player, need);
                } catch (NumberFormatException e) {
                    LogUtil.debug("money 条件参数非法：" + arg);
                    return false;
                }
            case "cooldown":
                return plugin.storage().getCooldownUntil(uuid, arg) <= System.currentTimeMillis();
            case "reward-unclaimed":
                return !plugin.storage().isRewardClaimed(uuid, arg);
            default:
                LogUtil.debug("未知条件类型：" + type + "（默认放行）");
                return true;
        }
    }
}
