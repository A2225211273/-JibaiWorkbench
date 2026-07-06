package me.jibai.workbench.command;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 菜单快捷物品的绑定工具。使用 PDC（Persistent Data Container）写入菜单 ID，
 * 不靠物品名或 lore 判断（符合即白的 Paper 开发规范）。
 *
 * <p>PDC 自 Bukkit 1.14 起可用，全核心 1.20+ 均支持。</p>
 *
 * @author 即白
 */
public final class MenuItemBinding {

    private final NamespacedKey key;

    public MenuItemBinding(JibaiWorkbenchPlugin plugin) {
        this.key = new NamespacedKey(plugin, "menu_id");
    }

    /** 给物品写入菜单绑定。 */
    public ItemStack tag(ItemStack item, String menuId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.STRING, menuId);
        item.setItemMeta(meta);
        return item;
    }

    /** 读取物品绑定的菜单 ID，无绑定返回 null。 */
    public String read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(key, PersistentDataType.STRING);
    }
}
