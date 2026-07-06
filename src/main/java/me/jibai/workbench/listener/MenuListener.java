package me.jibai.workbench.listener;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.menu.MenuSession;
import me.jibai.workbench.shop.ConfirmationManager;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 菜单交互监听：点击、拖拽。
 *
 * <p>安全要点：</p>
 * <ul>
 *   <li>只处理 holder 属于本插件的界面（MenuSession / ConfirmHolder），靠 InventoryHolder
 *       判断归属，不看标题（易错点 3/13）；</li>
 *   <li>对本插件界面一律 setCancelled(true)，玩家拿不走、放不进物品（易错点 1）；</li>
 *   <li>拖拽事件同样取消，若拖拽范围涉及菜单区域（易错点 2）；</li>
 *   <li>allow-take / allow-drag 为 true 时才放行对应操作。</li>
 * </ul>
 *
 * @author 即白
 */
public class MenuListener implements Listener {

    private final JibaiWorkbenchPlugin plugin;

    public MenuListener(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();

        // 确认页
        if (holder instanceof ConfirmationManager.ConfirmHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            // 只响应点击顶部界面
            if (event.getClickedInventory() == top) {
                plugin.confirmations().handleClick(player, holder, event.getRawSlot());
            }
            return;
        }

        // 本插件菜单
        if (!(holder instanceof MenuSession)) {
            return;
        }
        MenuSession session = (MenuSession) holder;

        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        Player player = (Player) event.getWhoClicked();

        boolean allowTake = session.getMenu().isAllowTake();

        // 默认取消，禁止拿取菜单物品；除非菜单允许拿取
        if (!allowTake) {
            event.setCancelled(true);
        }

        // shift 点击可能把玩家背包物品塞进菜单，需额外拦截（除非允许拖拽/拿取）
        if (event.isShiftClick() && !allowTake) {
            event.setCancelled(true);
        }

        // 只处理点击了菜单本身的情况
        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();
        MenuButton button = session.getButtonAt(slot);
        if (button == null) {
            return; // 点了空槽 / 填充物
        }

        // 点击条件
        if (!plugin.conditions().matchesAll(player, button.getClickConditions())) {
            plugin.messages().send(player, "cooldown-active",
                    me.jibai.workbench.config.MessageManager.vars("time", "条件不满足"));
            return;
        }

        boolean left = event.isLeftClick();
        boolean shift = event.isShiftClick();

        try {
            // 商店按钮
            if (button.getShop() != null) {
                if (left) {
                    plugin.shop().buy(player, button, false);
                } else {
                    plugin.shop().sell(player, button);
                }
                // 商店操作后不再执行普通动作
                return;
            }
            // 奖励按钮
            if (button.getReward() != null) {
                plugin.reward().claim(player, button);
                return;
            }
            // 普通动作
            plugin.actions().runForClick(player, button, left, shift);
        } catch (Throwable t) {
            LogUtil.warn("处理菜单点击异常：" + t.getMessage());
            plugin.messages().send(player, "action-failed");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        boolean ourMenu = holder instanceof MenuSession;
        boolean confirm = holder instanceof ConfirmationManager.ConfirmHolder;
        if (!ourMenu && !confirm) {
            return;
        }
        if (confirm) {
            event.setCancelled(true);
            return;
        }
        MenuSession session = (MenuSession) holder;
        if (session.getMenu().isAllowDrag()) {
            return;
        }
        // 若拖拽涉及顶部菜单区域，取消（易错点 2）
        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
