package me.jibai.workbench.listener;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.menu.MenuSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 生命周期相关监听：关闭菜单、玩家退出。
 *
 * <p>玩家退出时清理其返回历史，避免会话数据残留（易错点 18）。</p>
 *
 * @author 即白
 */
public class PlayerListener implements Listener {

    private final JibaiWorkbenchPlugin plugin;

    public PlayerListener(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MenuSession && event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            MenuSession session = (MenuSession) holder;
            // 播放关闭音效（安全）
            plugin.menus().playSound(player, session.getMenu().getCloseSound());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 清理返回历史，防止内存泄漏与会话污染
        plugin.menus().clearHistory(event.getPlayer().getUniqueId());
    }

    /** 手持菜单快捷物品右键时打开对应菜单（快捷物品通过 PDC 绑定菜单 ID）。 */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        String menuId = plugin.menuItemBinding().read(item);
        if (menuId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!plugin.menus().open(player, menuId)) {
            plugin.messages().send(player, "menu-not-found",
                    me.jibai.workbench.config.MessageManager.vars("menu", menuId));
        }
    }
}
