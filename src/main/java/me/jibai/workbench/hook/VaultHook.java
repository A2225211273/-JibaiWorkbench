package me.jibai.workbench.hook;

import me.jibai.workbench.util.LogUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 经济对接。软依赖：Vault 缺失或未启用时 {@link #isAvailable()} 返回 false，
 * 所有经济调用被安全跳过，绝不抛出 NPE（对应文档易错点 8）。
 *
 * @author 即白
 */
public class VaultHook {

    private Economy economy;
    private boolean available;

    /**
     * 尝试挂接 Vault 经济。
     *
     * @param wantEnabled config 中是否开启 vault
     */
    public void setup(boolean wantEnabled) {
        this.economy = null;
        this.available = false;
        if (!wantEnabled) {
            LogUtil.debug("config 中已关闭 Vault 对接。");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            LogUtil.debug("未检测到 Vault 插件，经济功能不可用。");
            return;
        }
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                LogUtil.warn("检测到 Vault，但没有经济插件注册经济服务，经济功能不可用。");
                return;
            }
            this.economy = rsp.getProvider();
            this.available = this.economy != null;
            if (available) {
                LogUtil.info("已挂接 Vault 经济：" + economy.getName());
            }
        } catch (Throwable t) {
            // 类不存在等情况：安全降级
            LogUtil.warn("挂接 Vault 失败，经济功能不可用：" + t.getMessage());
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return available && economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            return 0.0;
        }
        try {
            return economy.getBalance(player);
        } catch (Throwable t) {
            LogUtil.warn("读取余额失败：" + t.getMessage());
            return 0.0;
        }
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (amount == 0) {
            return true;
        }
        return isAvailable() && economy.has(player, amount);
    }

    /** 扣款，成功返回 true。amount==0 视为成功（免费）；失败（余额不足 / 异常）返回 false 且不扣款。 */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount == 0) {
            return true;
        }
        if (!isAvailable() || amount < 0) {
            return false;
        }
        try {
            EconomyResponse r = economy.withdrawPlayer(player, amount);
            return r != null && r.transactionSuccess();
        } catch (Throwable t) {
            LogUtil.warn("扣款失败：" + t.getMessage());
            return false;
        }
    }

    /** 给钱，成功返回 true。amount==0 视为成功（无需操作）。 */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount == 0) {
            return true;
        }
        if (!isAvailable() || amount < 0) {
            return false;
        }
        try {
            EconomyResponse r = economy.depositPlayer(player, amount);
            return r != null && r.transactionSuccess();
        } catch (Throwable t) {
            LogUtil.warn("给钱失败：" + t.getMessage());
            return false;
        }
    }

    /** 格式化金额，Vault 不可用时退化为原始数字。 */
    public String format(double amount) {
        if (isAvailable()) {
            try {
                return economy.format(amount);
            } catch (Throwable ignored) {
            }
        }
        return String.valueOf(amount);
    }
}
