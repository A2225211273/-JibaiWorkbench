package me.jibai.workbench.shop;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.util.ColorUtil;
import me.jibai.workbench.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 购买确认页管理器。为需要 confirm 的商品弹出一个独立的确认 GUI，
 * 玩家点击「确认」才真正购买，点击「取消」返回。
 *
 * <p>用独立 InventoryHolder {@link ConfirmHolder} 识别确认页，
 * 记录每个玩家正在确认的按钮，避免连点重复（配合 ShopService 的 processing 锁）。</p>
 *
 * @author 即白
 */
public class ConfirmationManager {

    /** 确认页的 InventoryHolder，携带待确认按钮。 */
    public static class ConfirmHolder implements InventoryHolder {
        private final MenuButton button;
        private Inventory inventory;

        public ConfirmHolder(MenuButton button) {
            this.button = button;
        }

        public MenuButton getButton() {
            return button;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final JibaiWorkbenchPlugin plugin;

    public ConfirmationManager(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /** 弹出确认页。 */
    public void request(Player player, MenuButton button) {
        ConfirmHolder holder = new ConfirmHolder(button);
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.colorize("&8确认购买？"));
        holder.setInventory(inv);

        ItemStack pane = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE, 1).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        double price = button.getShop() != null ? button.getShop().getBuyPrice() : 0;
        String itemName = button.getName() == null ? button.getId() : button.getName();
        inv.setItem(13, ItemBuilder.of(button.getMaterial(), 1)
                .name(itemName)
                .lore(Arrays.asList("&7价格：&e" + plugin.hooks().vault().format(price)))
                .glow(button.isGlow())
                .hideAttributes()
                .build());

        inv.setItem(CONFIRM_SLOT, ItemBuilder.of(Material.LIME_WOOL, 1)
                .name("&a&l确认购买")
                .lore(Arrays.asList("&7点击完成购买"))
                .build());
        inv.setItem(CANCEL_SLOT, ItemBuilder.of(Material.RED_WOOL, 1)
                .name("&c&l取消")
                .lore(Arrays.asList("&7点击取消并关闭"))
                .build());

        player.openInventory(inv);
    }

    /**
     * 处理确认页点击。
     *
     * @return 是否已处理（属于确认页）
     */
    public boolean handleClick(Player player, InventoryHolder holder, int slot) {
        if (!(holder instanceof ConfirmHolder)) {
            return false;
        }
        ConfirmHolder ch = (ConfirmHolder) holder;
        if (slot == CONFIRM_SLOT) {
            MenuButton button = ch.getButton();
            player.closeInventory();
            // 确认购买（confirmed=true 跳过再次确认）
            plugin.shop().buy(player, button, true);
        } else if (slot == CANCEL_SLOT) {
            player.closeInventory();
        }
        return true;
    }
}
