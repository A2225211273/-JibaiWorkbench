package me.jibai.workbench.config;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * config.yml 的强类型封装，集中读取所有开关与数值。
 *
 * <p>每次 reload 时重新构造，避免使用过期缓存。</p>
 *
 * @author 即白
 */
public class PluginConfig {

    private final String prefix;
    private final String language;

    private final boolean vaultEnabled;
    private final boolean papiEnabled;
    private final boolean luckpermsEnabled;

    private final String defaultMenu;
    private final long openCooldownMs;

    private final boolean allowConsoleCommand;
    private final boolean allowServerJump;

    private final String storageType;

    private final boolean validationStrict;
    private final boolean debug;

    public PluginConfig(JibaiWorkbenchPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        this.prefix = c.getString("prefix", "&8[&b即白工作台&8] ");
        this.language = c.getString("language", "zh_CN");

        this.vaultEnabled = c.getBoolean("hooks.vault", true);
        this.papiEnabled = c.getBoolean("hooks.placeholderapi", true);
        this.luckpermsEnabled = c.getBoolean("hooks.luckperms", true);

        this.defaultMenu = c.getString("menu.default-menu", "main");
        this.openCooldownMs = Math.max(0, c.getLong("menu.open-cooldown-ms", 300));

        this.allowConsoleCommand = c.getBoolean("actions.allow-console-command", true);
        this.allowServerJump = c.getBoolean("actions.allow-server-jump", false);

        this.storageType = c.getString("storage.type", "yaml").toLowerCase();

        this.validationStrict = c.getBoolean("validation.strict", false);
        this.debug = c.getBoolean("debug", false);

        LogUtil.setDebug(this.debug);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    public boolean isLuckpermsEnabled() {
        return luckpermsEnabled;
    }

    public String getDefaultMenu() {
        return defaultMenu;
    }

    public long getOpenCooldownMs() {
        return openCooldownMs;
    }

    public boolean isAllowConsoleCommand() {
        return allowConsoleCommand;
    }

    public boolean isAllowServerJump() {
        return allowServerJump;
    }

    public String getStorageType() {
        return storageType;
    }

    public boolean isValidationStrict() {
        return validationStrict;
    }

    public boolean isDebug() {
        return debug;
    }
}
