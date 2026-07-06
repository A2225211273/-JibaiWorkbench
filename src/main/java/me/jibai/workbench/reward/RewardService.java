package me.jibai.workbench.reward;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.menu.RewardData;
import me.jibai.workbench.util.LogUtil;
import me.jibai.workbench.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.UUID;

/**
 * 奖励领取逻辑。
 *
 * <p>领取流程：校验权限 → 校验领取资格（按 type 判断周期/一次性/在线时长）→
 * 先发放 → 发放成功后才记录领取时间（避免记录成功但实际没发，或发了没记；对应易错点 15）。</p>
 *
 * @author 即白
 */
public class RewardService {

    private final JibaiWorkbenchPlugin plugin;

    public RewardService(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /** 尝试领取奖励。 */
    public void claim(Player player, MenuButton button) {
        RewardData reward = button.getReward();
        if (reward == null || reward.getKey() == null || reward.getKey().isEmpty()) {
            return;
        }
        UUID uuid = player.getUniqueId();

        // 领取权限
        if (reward.getPermission() != null && !player.hasPermission(reward.getPermission())) {
            plugin.messages().send(player, "reward-no-permission");
            return;
        }
        if (!player.hasPermission("jibaiworkbench.reward.claim")) {
            plugin.messages().send(player, "reward-no-permission");
            return;
        }

        long now = System.currentTimeMillis();
        long lastClaim = plugin.storage().getRewardClaimTime(uuid, reward.getKey());

        switch (reward.getType()) {
            case ONCE:
                if (lastClaim > 0) {
                    plugin.messages().send(player, "reward-already-claimed");
                    return;
                }
                break;
            case DAILY:
                if (isSamePeriod(lastClaim, now, Calendar.DAY_OF_YEAR)) {
                    plugin.messages().send(player, "reward-already-claimed");
                    return;
                }
                break;
            case WEEKLY:
                if (isSamePeriod(lastClaim, now, Calendar.WEEK_OF_YEAR)) {
                    plugin.messages().send(player, "reward-already-claimed");
                    return;
                }
                break;
            case PLAYTIME:
                // 玩家在线时长（ticks -> 分钟）。使用统计数据，兼容全核心。
                int minutes = getPlayedMinutes(player);
                if (minutes < reward.getPlaytimeMin()) {
                    long remainMs = (reward.getPlaytimeMin() - minutes) * 60_000L;
                    plugin.messages().send(player, "reward-not-ready",
                            me.jibai.workbench.config.MessageManager.vars(
                                    "time", TimeUtil.formatRemaining(remainMs)));
                    return;
                }
                // playtime 类奖励默认也只领一次
                if (lastClaim > 0) {
                    plugin.messages().send(player, "reward-already-claimed");
                    return;
                }
                break;
            default:
                LogUtil.debug("未知奖励类型，拒绝领取：" + reward.getKey());
                plugin.messages().send(player, "reward-not-ready",
                        me.jibai.workbench.config.MessageManager.vars("time", "未知"));
                return;
        }

        // ===== 先发放 =====
        boolean granted = grant(player, reward);
        if (!granted) {
            plugin.messages().send(player, "action-failed");
            LogUtil.warn("奖励发放失败，未记录领取：" + reward.getKey());
            return;
        }
        // ===== 发放成功再记录；记录（落盘）失败则不提示成功 =====
        if (!plugin.storage().setRewardClaimed(uuid, reward.getKey(), now)) {
            LogUtil.error("奖励已发放但领取记录保存失败，玩家可能可重复领取：" + reward.getKey()
                    + "（玩家 " + player.getName() + "）");
            plugin.messages().send(player, "action-failed");
            return;
        }
        plugin.messages().send(player, "reward-claimed");
        plugin.menus().playSound(player, "ENTITY_PLAYER_LEVELUP");
        // 刷新菜单显示领取状态
        plugin.menus().refresh(player);
    }

    /**
     * 发放奖励：给物品 + 跑控制台指令。
     *
     * <ul>
     *   <li>配置了 give-item 但发放失败（材质无效/数量非法）→ 返回 false，不写入领取记录；</li>
     *   <li>控制台指令 dispatchCommand 返回 false → 记录警告；</li>
     *   <li>只有命令奖励（无 give-item）且所有命令都失败 → 返回 false；</li>
     *   <li>保守策略：同时有 give-item 与命令时，只要有任一命令失败也返回 false，避免误报完全成功。</li>
     * </ul>
     *
     * @return true=发放成功（可写入领取记录）；false=发放失败，不得记录
     */
    private boolean grant(Player player, RewardData reward) {
        boolean hasItem = reward.getGiveItem() != null;
        java.util.List<String> commands = reward.getCommands();
        boolean hasCommands = commands != null && !commands.isEmpty();
        // 1. 物品发放：失败直接判定发放失败，不记录
        if (hasItem) {
            boolean ok;
            try {
                ok = plugin.actions().giveItem(player, reward.getGiveItem());
            } catch (Throwable t) {
                ok = false;
                LogUtil.error("奖励物品发放异常：" + reward.getGiveItem() + " -> " + t.getMessage());
            }
            if (!ok) {
                LogUtil.warn("奖励物品发放失败（give-item 无法发放）：" + reward.getGiveItem());
                return false;
            }
        }
        // 2. 命令发放
        boolean anyCommandFailed = false;
        if (hasCommands) {
            for (String cmd : commands) {
                boolean ok;
                try {
                    ok = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            cmd.replace("{player}", player.getName()));
                } catch (Throwable t) {
                    ok = false;
                    LogUtil.error("奖励命令执行异常：" + cmd + " -> " + t.getMessage());
                }
                if (!ok) {
                    anyCommandFailed = true;
                    LogUtil.warn("奖励命令执行返回失败：" + cmd);
                }
            }
        }
        // 3. 命令失败的处理：
        //    - 只有命令、无物品，且所有命令都失败 → 返回 false
        //    - 有物品也有命令，命令有任一失败 → 保守起见也返回 false，避免误报完全成功
        if (hasCommands && anyCommandFailed) {
            return false;
        }
        return true;
    }

    /** 判断两个时间戳是否处于同一个日期/周期。lastClaim=0 视为从未领取，返回 false。 */
    private boolean isSamePeriod(long lastClaim, long now, int calendarField) {
        if (lastClaim <= 0) {
            return false;
        }
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(lastClaim);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(now);
        if (a.get(Calendar.YEAR) != b.get(Calendar.YEAR)) {
            return false;
        }
        return a.get(calendarField) == b.get(calendarField);
    }

    /** 玩家总在线时间（分钟），使用 Bukkit 统计，兼容全核心。 */
    private int getPlayedMinutes(Player player) {
        try {
            // PLAY_ONE_MINUTE 实为以 tick 计（20 tick = 1 秒）
            int ticks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
            return ticks / (20 * 60);
        } catch (Throwable t) {
            return 0;
        }
    }
}
