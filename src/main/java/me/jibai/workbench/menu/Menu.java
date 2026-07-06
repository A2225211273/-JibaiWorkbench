package me.jibai.workbench.menu;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 菜单模型，对应 menus/ 下的一个 YAML 文件。
 *
 * @author 即白
 */
public class Menu {

    private String id;
    private String title = "菜单";
    private int rows = 3;
    private String permission;         // 打开权限，可空
    private String openSound;
    private String closeSound;

    private Material fillMaterial;      // 背景填充物品，可空
    private String fillName = " ";

    private boolean allowDrag;
    private boolean allowTake;

    /** 源文件名（不含路径），用于 debug / validate 输出。 */
    private String sourceFile;

    /** 按钮：id -> 按钮。用 LinkedHashMap 保留定义顺序。 */
    private final Map<String, MenuButton> buttons = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getSize() {
        return rows * 9;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getOpenSound() {
        return openSound;
    }

    public void setOpenSound(String openSound) {
        this.openSound = openSound;
    }

    public String getCloseSound() {
        return closeSound;
    }

    public void setCloseSound(String closeSound) {
        this.closeSound = closeSound;
    }

    public Material getFillMaterial() {
        return fillMaterial;
    }

    public void setFillMaterial(Material fillMaterial) {
        this.fillMaterial = fillMaterial;
    }

    public String getFillName() {
        return fillName;
    }

    public void setFillName(String fillName) {
        this.fillName = fillName;
    }

    public boolean isAllowDrag() {
        return allowDrag;
    }

    public void setAllowDrag(boolean allowDrag) {
        this.allowDrag = allowDrag;
    }

    public boolean isAllowTake() {
        return allowTake;
    }

    public void setAllowTake(boolean allowTake) {
        this.allowTake = allowTake;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Map<String, MenuButton> getButtons() {
        return buttons;
    }
}
