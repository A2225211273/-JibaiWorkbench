package me.jibai.workbench.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * 按钮上的奖励数据，由菜单 YAML 的 {@code reward} 段落解析而来。
 * 领取逻辑由 {@link me.jibai.workbench.reward.RewardService} 处理。
 *
 * @author 即白
 */
public class RewardData {

    /** 奖励类型。 */
    public enum Type {
        DAILY, WEEKLY, ONCE, PLAYTIME, UNKNOWN;

        public static Type from(String s) {
            if (s == null) {
                return UNKNOWN;
            }
            switch (s.toLowerCase()) {
                case "daily":
                    return DAILY;
                case "weekly":
                    return WEEKLY;
                case "once":
                    return ONCE;
                case "playtime":
                    return PLAYTIME;
                default:
                    return UNKNOWN;
            }
        }
    }

    private String key;
    private Type type = Type.UNKNOWN;
    private String giveItem;               // 材质:数量
    private final List<String> commands = new ArrayList<>();
    private String permission;
    private int playtimeMin;               // type=playtime 时要求的在线分钟

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getGiveItem() {
        return giveItem;
    }

    public void setGiveItem(String giveItem) {
        this.giveItem = giveItem;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getPlaytimeMin() {
        return playtimeMin;
    }

    public void setPlaytimeMin(int playtimeMin) {
        this.playtimeMin = playtimeMin;
    }
}
