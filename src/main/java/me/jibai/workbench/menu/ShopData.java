package me.jibai.workbench.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * 按钮上的商店数据，由菜单 YAML 的 {@code shop} 段落解析而来。
 * 购买 / 出售逻辑由 {@link me.jibai.workbench.shop.ShopService} 处理。
 *
 * @author 即白
 */
public class ShopData {

    private double buyPrice = -1;      // <0 表示不可购买
    private double sellPrice = -1;     // <0 表示不可出售
    private String giveItem;           // 材质:数量，可空
    private final List<String> commands = new ArrayList<>();
    private String permission;         // 购买所需权限，可空
    private boolean once;              // 一次性购买
    private int dailyLimit;            // 每日限购，0=不限
    private int cooldownSec;           // 购买冷却（秒）
    private int stock = -1;            // 全服库存，-1=无限
    private boolean confirm;           // 是否需要确认页

    public boolean canBuy() {
        return buyPrice >= 0;
    }

    public boolean canSell() {
        return sellPrice >= 0;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
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

    public boolean isOnce() {
        return once;
    }

    public void setOnce(boolean once) {
        this.once = once;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getCooldownSec() {
        return cooldownSec;
    }

    public void setCooldownSec(int cooldownSec) {
        this.cooldownSec = cooldownSec;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isConfirm() {
        return confirm;
    }

    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }
}
