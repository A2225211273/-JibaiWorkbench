package me.jibai.workbench.hook;

import me.jibai.workbench.config.PluginConfig;

/**
 * 统一管理三个软依赖 Hook，reload 时集中重新挂接。
 *
 * @author 即白
 */
public class HookManager {

    private final VaultHook vault = new VaultHook();
    private final PapiHook papi = new PapiHook();
    private final LuckPermsHook luckPerms = new LuckPermsHook();

    /** 根据配置重新挂接所有依赖。 */
    public void setup(PluginConfig config) {
        vault.setup(config.isVaultEnabled());
        papi.setup(config.isPapiEnabled());
        luckPerms.setup(config.isLuckpermsEnabled());
    }

    public VaultHook vault() {
        return vault;
    }

    public PapiHook papi() {
        return papi;
    }

    public LuckPermsHook luckPerms() {
        return luckPerms;
    }
}
