package me.jibai.workbench;

import me.jibai.workbench.action.ActionExecutor;
import me.jibai.workbench.command.MenuItemBinding;
import me.jibai.workbench.command.WorkbenchCommand;
import me.jibai.workbench.condition.ConditionEvaluator;
import me.jibai.workbench.config.MessageManager;
import me.jibai.workbench.config.PluginConfig;
import me.jibai.workbench.config.ValidationResult;
import me.jibai.workbench.hook.HookManager;
import me.jibai.workbench.listener.MenuListener;
import me.jibai.workbench.listener.PlayerListener;
import me.jibai.workbench.menu.MenuManager;
import me.jibai.workbench.menu.PlaceholderResolver;
import me.jibai.workbench.reward.RewardService;
import me.jibai.workbench.shop.ConfirmationManager;
import me.jibai.workbench.shop.ShopService;
import me.jibai.workbench.storage.MySqlStorage;
import me.jibai.workbench.storage.SqliteStorage;
import me.jibai.workbench.storage.Storage;
import me.jibai.workbench.storage.YamlStorage;
import me.jibai.workbench.template.TemplateManager;
import me.jibai.workbench.util.BannerUtil;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * JibaiWorkbench 主类 —— 即白服务器交互工作台。
 *
 * <p>生命周期遵循即白的 Paper 开发规范：onEnable 中注册配置/事件/指令/服务，
 * 不在构造器做初始化；onDisable 保存数据、清理会话。</p>
 *
 * <p>全核心兼容：编译期只用 spigot-api，不碰 Paper 专属 API。</p>
 *
 * @author 即白
 * @since 1.0.0
 */
public class JibaiWorkbenchPlugin extends JavaPlugin {

    private PluginConfig config;
    private MessageManager messages;
    private HookManager hooks;
    private Storage storage;

    private TemplateManager templates;
    private MenuManager menus;
    private PlaceholderResolver placeholders;
    private ConditionEvaluator conditions;
    private ActionExecutor actions;
    private ShopService shop;
    private ConfirmationManager confirmations;
    private RewardService reward;
    private MenuItemBinding menuItemBinding;

    @Override
    public void onEnable() {
        LogUtil.init(this);

        // 1. 释放默认配置
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        // 2. 读取配置
        this.config = new PluginConfig(this);
        this.messages = new MessageManager(this);
        this.messages.load(config.getPrefix());

        // 3. 挂接软依赖
        this.hooks = new HookManager();
        this.hooks.setup(config);

        // 4. 存储（预留 SQLite/MySQL，未实现则回退 YAML）
        this.storage = createStorage();
        this.storage.init();

        // 5. 模板与菜单管理
        this.templates = new TemplateManager(this);
        this.templates.extractTemplates();

        this.placeholders = new PlaceholderResolver(this);
        this.conditions = new ConditionEvaluator(this);
        this.actions = new ActionExecutor(this);
        this.shop = new ShopService(this);
        this.confirmations = new ConfirmationManager(this);
        this.reward = new RewardService(this);
        this.menuItemBinding = new MenuItemBinding(this);

        this.menus = new MenuManager(this);
        ValidationResult result = new ValidationResult();
        this.menus.loadAll(result);
        logValidationToConsole(result);

        // 6. 注册指令
        registerCommand();

        // 7. 注册事件
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 8. 注册 BungeeCord 通道（用于跳服动作）
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        } catch (Throwable t) {
            LogUtil.debug("注册 BungeeCord 通道失败（不影响主要功能）：" + t.getMessage());
        }

        // 9. 彩色启动横幅
        BannerUtil.printEnable(
                getDescription().getVersion(),
                getServer().getName() + " " + getServer().getBukkitVersion(),
                menus.getMenus().size(),
                templates.getTemplateCount(),
                hooks.vault().isAvailable(),
                hooks.papi().isAvailable(),
                hooks.luckPerms().isAvailable());
    }

    @Override
    public void onDisable() {
        // 关闭所有菜单会话，防止关服瞬间点击残留菜单
        if (menus != null) {
            menus.closeAllAndClear();
        }
        // 保存并关闭存储
        if (storage != null) {
            storage.close();
        }
        BannerUtil.printDisable();
    }

    /** 根据 config.storage.type 选择存储实现，预留实现未就绪则回退 YAML。 */
    private Storage createStorage() {
        String type = config.getStorageType();
        switch (type) {
            case "sqlite":
                if (SqliteStorage.isImplemented()) {
                    return new SqliteStorage();
                }
                LogUtil.warn("storage.type=sqlite 尚未实现，已回退为 YAML 存储。");
                return new YamlStorage(this);
            case "mysql":
                if (MySqlStorage.isImplemented()) {
                    return new MySqlStorage();
                }
                LogUtil.warn("storage.type=mysql 尚未实现，已回退为 YAML 存储。");
                return new YamlStorage(this);
            case "yaml":
            default:
                return new YamlStorage(this);
        }
    }

    private void registerCommand() {
        WorkbenchCommand handler = new WorkbenchCommand(this);
        PluginCommand cmd = getCommand("jworkbench");
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        } else {
            LogUtil.error("无法注册指令 jworkbench，请检查 plugin.yml。");
        }
    }

    /**
     * 重载配置、消息、依赖与菜单。会先关闭所有旧菜单会话，避免旧菜单继续执行错误动作
     * （易错点 5/14）。
     *
     * @return 本次加载的校验结果
     */
    public ValidationResult reloadEverything() {
        // 先关掉所有在线玩家的菜单
        menus.closeAllAndClear();

        reloadConfig();
        this.config = new PluginConfig(this);
        this.messages.load(config.getPrefix());
        this.hooks.setup(config);

        // 存储类型变了则重建
        // （简单起见：始终保持已初始化的存储；类型切换需重启，避免运行期数据错乱）

        ValidationResult result = new ValidationResult();
        this.menus.loadAll(result);
        logValidationToConsole(result);
        return result;
    }

    private void logValidationToConsole(ValidationResult result) {
        if (result.isClean()) {
            return;
        }
        if (result.hasErrors()) {
            LogUtil.warn("菜单校验发现 " + result.getErrors().size() + " 个错误、"
                    + result.getWarnings().size() + " 个警告：");
            for (ValidationResult.Issue e : result.getErrors()) {
                LogUtil.warn("  [错误] " + e.file + " > " + e.menuId
                        + (e.buttonId != null ? " > " + e.buttonId : "") + " : " + e.message);
            }
        }
        for (ValidationResult.Issue w : result.getWarnings()) {
            LogUtil.debug("  [警告] " + w.file + " > " + w.menuId
                    + (w.buttonId != null ? " > " + w.buttonId : "") + " : " + w.message);
        }
    }

    private void saveResourceIfMissing(String name) {
        java.io.File file = new java.io.File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    // ===== 各服务的访问器（供各模块互相调用） =====

    public PluginConfig config() {
        return config;
    }

    public MessageManager messages() {
        return messages;
    }

    public HookManager hooks() {
        return hooks;
    }

    public Storage storage() {
        return storage;
    }

    public TemplateManager templates() {
        return templates;
    }

    public MenuManager menus() {
        return menus;
    }

    public PlaceholderResolver placeholders() {
        return placeholders;
    }

    public ConditionEvaluator conditions() {
        return conditions;
    }

    public ActionExecutor actions() {
        return actions;
    }

    public ShopService shop() {
        return shop;
    }

    public ConfirmationManager confirmations() {
        return confirmations;
    }

    public RewardService reward() {
        return reward;
    }

    public MenuItemBinding menuItemBinding() {
        return menuItemBinding;
    }
}
