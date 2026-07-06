package me.jibai.workbench.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品构建工具，用于把菜单配置转成 {@link ItemStack}。
 *
 * <p>只用 Bukkit 通用 API：名称 / lore 用字符串（已上色），发光用附魔 + 隐藏附魔标志实现，
 * CustomModelData 通过反射调用避免低版本无该方法时编译/运行报错。</p>
 *
 * @author 即白
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    /** 设置显示名称（传入原始文本，内部上色）。 */
    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        return this;
    }

    /** 设置 lore（传入原始文本列表，内部逐行上色）。 */
    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) {
                colored.add(ColorUtil.colorize(line == null ? "" : line));
            }
            meta.setLore(colored);
        }
        return this;
    }

    /** 发光效果：附一个隐藏的附魔并隐藏附魔标签。 */
    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            try {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignored) {
                // 个别核心附魔名不同，失败则忽略发光效果，不影响物品本身
            }
        }
        return this;
    }

    /** CustomModelData，通过反射兼容不支持该方法的旧版本。 */
    public ItemBuilder customModelData(int data) {
        if (meta != null && data > 0) {
            try {
                meta.getClass().getMethod("setCustomModelData", Integer.class)
                        .invoke(meta, data);
            } catch (Throwable ignored) {
                // 旧版本不支持 CustomModelData，忽略即可
            }
        }
        return this;
    }

    /** 隐藏物品属性（攻击力等），让菜单展示更干净。 */
    public ItemBuilder hideAttributes() {
        if (meta != null) {
            try {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            } catch (Throwable ignored) {
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
