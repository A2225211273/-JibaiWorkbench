package me.jibai.workbench.menu;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单按钮模型。一个按钮可占多个槽位，可带显示条件、点击条件、
 * 分点击类型的动作，以及可选的 shop / reward 数据。
 *
 * @author 即白
 */
public class MenuButton {

    private String id;
    private final List<Integer> slots = new ArrayList<>();
    private Material material = Material.STONE;
    private String name;
    private List<String> lore = new ArrayList<>();
    private List<String> loreLocked = new ArrayList<>(); // 无权限/未满足时显示的替代 lore
    private int amount = 1;
    private boolean glow;
    private int customModelData;
    private String permission;

    private final List<String> viewConditions = new ArrayList<>();
    private final List<String> clickConditions = new ArrayList<>();

    // 分点击类型的动作
    private final List<String> defaultActions = new ArrayList<>();
    private final List<String> leftActions = new ArrayList<>();
    private final List<String> rightActions = new ArrayList<>();
    private final List<String> shiftLeftActions = new ArrayList<>();
    private final List<String> shiftRightActions = new ArrayList<>();

    private ShopData shop;
    private RewardData reward;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public List<String> getLoreLocked() {
        return loreLocked;
    }

    public void setLoreLocked(List<String> loreLocked) {
        this.loreLocked = loreLocked;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getViewConditions() {
        return viewConditions;
    }

    public List<String> getClickConditions() {
        return clickConditions;
    }

    public List<String> getDefaultActions() {
        return defaultActions;
    }

    public List<String> getLeftActions() {
        return leftActions;
    }

    public List<String> getRightActions() {
        return rightActions;
    }

    public List<String> getShiftLeftActions() {
        return shiftLeftActions;
    }

    public List<String> getShiftRightActions() {
        return shiftRightActions;
    }

    public ShopData getShop() {
        return shop;
    }

    public void setShop(ShopData shop) {
        this.shop = shop;
    }

    public RewardData getReward() {
        return reward;
    }

    public void setReward(RewardData reward) {
        this.reward = reward;
    }
}
