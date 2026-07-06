package me.jibai.workbench.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 菜单会话，同时充当 {@link InventoryHolder}。
 *
 * <p>通过自定义 InventoryHolder 判断某个 Inventory 是否属于本插件（对应文档易错点 3、13、20），
 * 不靠标题字符串判断。每次为玩家打开菜单都会新建一个会话，多个玩家互不干扰
 * （对应文档易错点 4）。</p>
 *
 * @author 即白
 */
public class MenuSession implements InventoryHolder {

    private final UUID playerId;
    private final Menu menu;

    /** 当前 Inventory 中每个槽位对应的按钮，用于点击时快速定位。 */
    private final Map<Integer, MenuButton> slotButtons = new HashMap<>();

    private Inventory inventory;

    public MenuSession(Player player, Menu menu) {
        this.playerId = player.getUniqueId();
        this.menu = menu;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Menu getMenu() {
        return menu;
    }

    public Map<Integer, MenuButton> getSlotButtons() {
        return slotButtons;
    }

    public MenuButton getButtonAt(int slot) {
        return slotButtons.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
