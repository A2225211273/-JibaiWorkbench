package me.jibai.workbench.shop;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.config.MessageManager;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.menu.ShopData;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 商店购买 / 出售逻辑。
 *
 * <p>关键安全点：</p>
 * <ul>
 *   <li>先校验（权限/经济/限购/冷却/库存/背包空间），全部通过后「先扣款再发货」，
 *       发货失败立即退款，避免玩家损失金币（易错点 9）；</li>
 *   <li>用 {@link #processing} 集合防止确认页连点造成重复扣款（易错点 10）；</li>
 *   <li>Vault 不可用时给友好提示，绝不 NPE（易错点 8）。</li>
 * </ul>
 *
 * @author 即白
 */
public class ShopService {

    private final JibaiWorkbenchPlugin plugin;

    /** 正在处理购买的玩家+按钮键，防止连点重复扣款。 */
    private final java.util.Set<String> processing = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public ShopService(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    private String shopKey(MenuButton button) {
        // 用按钮 id 作为商店记录键；同一菜单内按钮 id 唯一
        return button.getId();
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    /**
     * 处理购买。confirmed=false 且商品需要确认时，仅提示并返回（由确认页再次调用 confirmed=true）。
     */
    public void buy(Player player, MenuButton button, boolean confirmed) {
        ShopData shop = button.getShop();
        if (shop == null || !shop.canBuy()) {
            return;
        }
        if (!player.hasPermission("jibaiworkbench.shop.buy")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        UUID uuid = player.getUniqueId();
        String key = shopKey(button);
        String lock = uuid + "#" + key;

        // 防连点
        if (!processing.add(lock)) {
            return;
        }
        try {
            boolean needEconomy = shop.getBuyPrice() > 0;
            // Vault：仅当价格 > 0 时才需要经济插件；0 元商品（免费领取）无需 Vault
            if (needEconomy && !plugin.hooks().vault().isAvailable()) {
                plugin.messages().send(player, "economy-unavailable");
                return;
            }
            // 商品权限
            if (shop.getPermission() != null && !player.hasPermission(shop.getPermission())) {
                plugin.messages().send(player, "shop-buy-failed",
                        MessageManager.vars("reason", "你没有购买该商品的权限"));
                return;
            }
            // 一次性
            if (shop.isOnce() && plugin.storage().isPurchasedOnce(uuid, key)) {
                plugin.messages().send(player, "shop-limit-reached");
                return;
            }
            // 每日限购
            if (shop.getDailyLimit() > 0) {
                int count = plugin.storage().getPurchaseCount(uuid, key, todayKey());
                if (count >= shop.getDailyLimit()) {
                    plugin.messages().send(player, "shop-limit-reached");
                    return;
                }
            }
            // 购买冷却
            if (shop.getCooldownSec() > 0) {
                long until = plugin.storage().getCooldownUntil(uuid, "shop_" + key);
                long now = System.currentTimeMillis();
                if (until > now) {
                    plugin.messages().send(player, "cooldown-active",
                            MessageManager.vars("time",
                                    me.jibai.workbench.util.TimeUtil.formatRemaining(until - now)));
                    return;
                }
            }
            // 需要确认页
            if (shop.isConfirm() && !confirmed) {
                plugin.confirmations().request(player, button);
                plugin.messages().send(player, "shop-confirm-open");
                return;
            }
            // 背包空间（仅当发实体物品时）
            if (shop.getGiveItem() != null && !hasSpace(player, shop.getGiveItem())) {
                plugin.messages().send(player, "shop-inventory-full");
                return;
            }
            // 余额
            if (!plugin.hooks().vault().has(player, shop.getBuyPrice())) {
                plugin.messages().send(player, "economy-not-enough", MessageManager.vars(
                        "cost", plugin.hooks().vault().format(shop.getBuyPrice()),
                        "balance", plugin.hooks().vault().format(plugin.hooks().vault().getBalance(player))));
                return;
            }

            // ===== 先扣款 =====
            if (!plugin.hooks().vault().withdraw(player, shop.getBuyPrice())) {
                plugin.messages().send(player, "shop-buy-failed",
                        MessageManager.vars("reason", "扣款失败"));
                return;
            }
            // ===== 再发货，失败则退款 =====
            boolean delivered = deliver(player, shop);
            if (!delivered) {
                plugin.hooks().vault().deposit(player, shop.getBuyPrice()); // 退款
                plugin.messages().send(player, "shop-buy-failed",
                        MessageManager.vars("reason", "发货失败，已退款"));
                LogUtil.warn("商品发货失败已退款：" + key);
                return;
            }

            // 记录限购 / 冷却：任一写入失败则退款并提示联系管理员
            // （物品可能已发放，无法收回，退款是「尽量」补偿，同时记录严重日志）
            boolean recordOk = true;
            if (shop.isOnce()) {
                recordOk = plugin.storage().setPurchasedOnce(uuid, key);
            }
            if (recordOk && shop.getDailyLimit() > 0) {
                recordOk = plugin.storage().addPurchaseCount(uuid, key, todayKey());
            }
            if (recordOk && shop.getCooldownSec() > 0) {
                recordOk = plugin.storage().setCooldownUntil(uuid, "shop_" + key,
                        System.currentTimeMillis() + shop.getCooldownSec() * 1000L);
            }
            if (!recordOk) {
                // 尽量退款；发放的物品无法收回，记录严重日志供管理员核对
                if (shop.getBuyPrice() > 0) {
                    plugin.hooks().vault().deposit(player, shop.getBuyPrice());
                }
                LogUtil.error("购买记录保存失败！已尝试退款，但发放的物品无法自动收回，请管理员核对："
                        + "玩家 " + player.getName() + " 商品 " + key);
                plugin.messages().send(player, "shop-buy-failed",
                        MessageManager.vars("reason", "购买记录保存失败，已尝试退款，请联系管理员"));
                return;
            }

            String itemName = button.getName() == null ? key
                    : me.jibai.workbench.util.ColorUtil.stripColor(button.getName());
            plugin.messages().send(player, "shop-buy-success", MessageManager.vars(
                    "item", itemName,
                    "cost", plugin.hooks().vault().format(shop.getBuyPrice())));
        } finally {
            processing.remove(lock);
        }
    }

    /**
     * 发货：给物品 + 跑控制台指令。
     *
     * <ul>
     *   <li>配置了 give-item 但发放失败（材质无效/数量非法）→ 立即返回 false，由调用方退款；</li>
     *   <li>控制台指令 dispatchCommand 返回 false → 记录警告；</li>
     *   <li>若该商品只有命令发货（无 give-item）且命令失败 → 视为发货失败返回 false；</li>
     *   <li>既无物品也无命令（如免费标记类）→ 视为成功。</li>
     * </ul>
     */
    private boolean deliver(Player player, ShopData shop) {
        boolean hasItem = shop.getGiveItem() != null;
        boolean hasCommands = shop.getCommands() != null && !shop.getCommands().isEmpty();
        if (!hasItem && !hasCommands) {
            return true;
        }
        // 1. 物品发货：失败直接判定发货失败
        if (hasItem) {
            if (!plugin.actions().giveItem(player, shop.getGiveItem())) {
                LogUtil.warn("商品发货失败（give-item 无法发放）：" + shop.getGiveItem());
                return false;
            }
        }
        // 2. 命令发货
        boolean allCommandsOk = true;
        if (hasCommands) {
            for (String cmd : shop.getCommands()) {
                boolean ok;
                try {
                    ok = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            cmd.replace("{player}", player.getName()));
                } catch (Throwable t) {
                    ok = false;
                    LogUtil.error("发货命令执行异常：" + cmd + " -> " + t.getMessage());
                }
                if (!ok) {
                    allCommandsOk = false;
                    LogUtil.warn("发货命令执行返回失败：" + cmd);
                }
            }
        }
        // 3. 只有命令发货且命令失败 → 视为发货失败
        if (!hasItem && hasCommands && !allCommandsOk) {
            return false;
        }
        return true;
    }

    /** 出售：把玩家背包中对应材质扣掉，给钱。这里出售按钮物品的材质。 */
    public void sell(Player player, MenuButton button) {
        ShopData shop = button.getShop();
        if (shop == null || !shop.canSell()) {
            return;
        }
        if (!player.hasPermission("jibaiworkbench.shop.sell")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        if (!plugin.hooks().vault().isAvailable()) {
            plugin.messages().send(player, "economy-unavailable");
            return;
        }
        Material mat = button.getMaterial();
        int need = 1;
        // 出售数量按 give-item 里的数量，若无则按 1
        if (shop.getGiveItem() != null) {
            String[] parts = shop.getGiveItem().split(":");
            if (parts.length >= 2) {
                try {
                    need = Math.max(1, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!player.getInventory().containsAtLeast(new ItemStack(mat), need)) {
            plugin.messages().send(player, "shop-sell-failed",
                    MessageManager.vars("reason", "背包中没有足够的物品"));
            return;
        }
        // 先扣物品再给钱
        player.getInventory().removeItem(new ItemStack(mat, need));
        if (!plugin.hooks().vault().deposit(player, shop.getSellPrice())) {
            // 给钱失败，退回物品
            player.getInventory().addItem(new ItemStack(mat, need));
            plugin.messages().send(player, "shop-sell-failed",
                    MessageManager.vars("reason", "结算失败，已退回物品"));
            return;
        }
        String itemName = button.getName() == null ? mat.name()
                : me.jibai.workbench.util.ColorUtil.stripColor(button.getName());
        plugin.messages().send(player, "shop-sell-success", MessageManager.vars(
                "item", itemName,
                "price", plugin.hooks().vault().format(shop.getSellPrice())));
    }

    /** 粗略判断背包是否放得下将要给予的物品。 */
    private boolean hasSpace(Player player, String giveItem) {
        String[] parts = giveItem.split(":");
        Material mat = me.jibai.workbench.menu.MenuLoader.matchMaterial(parts[0].trim());
        if (mat == null) {
            return true;
        }
        // 只要有一个空槽就认为放得下（addItem 会自行处理堆叠/溢出）
        return player.getInventory().firstEmpty() != -1;
    }
}
