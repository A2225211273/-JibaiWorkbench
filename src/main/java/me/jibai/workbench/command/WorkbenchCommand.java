package me.jibai.workbench.command;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.config.MessageManager;
import me.jibai.workbench.config.ValidationResult;
import me.jibai.workbench.menu.Menu;
import me.jibai.workbench.menu.MenuButton;
import me.jibai.workbench.template.TemplateManager;
import me.jibai.workbench.util.ColorUtil;
import me.jibai.workbench.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /jworkbench (/jwb) 指令处理与 Tab 补全。
 *
 * <p>Tab 补全只暴露有权限、且真实存在的菜单（易错点 20）。</p>
 *
 * @author 即白
 */
public class WorkbenchCommand implements CommandExecutor, TabCompleter {

    private final JibaiWorkbenchPlugin plugin;

    public WorkbenchCommand(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                help(sender);
                return true;
            case "open":
                return handleOpen(sender, args);
            case "list":
                return handleList(sender);
            case "create":
                return handleCreate(sender, args);
            case "copy":
                return handleCopy(sender, args);
            case "reload":
                return handleReload(sender);
            case "validate":
                return handleValidate(sender);
            case "preview":
                return handlePreview(sender, args);
            case "debug":
                return handleDebug(sender, args);
            case "giveitem":
                return handleGiveItem(sender, args);
            default:
                plugin.messages().send(sender, "unknown-command");
                return true;
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(ColorUtil.colorize("&b&m----------- &r&b即白工作台 帮助 &b&m-----------"));
        s.sendMessage(ColorUtil.colorize("&e/jwb open <菜单> [玩家] &7- 打开菜单"));
        s.sendMessage(ColorUtil.colorize("&e/jwb list &7- 列出所有菜单"));
        if (s.hasPermission("jibaiworkbench.create")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb create <模板> <菜单ID> &7- 从模板创建菜单"));
        }
        if (s.hasPermission("jibaiworkbench.copy")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb copy <源> <目标> &7- 复制菜单"));
        }
        if (s.hasPermission("jibaiworkbench.reload")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb reload &7- 重载配置与菜单"));
        }
        if (s.hasPermission("jibaiworkbench.validate")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb validate &7- 校验所有菜单配置"));
        }
        if (s.hasPermission("jibaiworkbench.preview")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb preview <菜单> &7- 预览菜单"));
        }
        if (s.hasPermission("jibaiworkbench.debug")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb debug <菜单> &7- 查看菜单调试信息"));
        }
        if (s.hasPermission("jibaiworkbench.giveitem")) {
            s.sendMessage(ColorUtil.colorize("&e/jwb giveitem <玩家> <菜单> &7- 给玩家菜单快捷物品"));
        }
        s.sendMessage(ColorUtil.colorize("&7可用模板：&f" + String.join(", ", TemplateManager.TEMPLATES)));
        s.sendMessage(ColorUtil.colorize("&8作者 即白 | jibai0517@gamil.com"));
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb open <菜单> [玩家]"));
            return true;
        }
        String menuId = args[1];
        if (!plugin.menus().exists(menuId)) {
            plugin.messages().send(sender, "menu-not-found", MessageManager.vars("menu", menuId));
            return true;
        }
        // 为他人打开
        if (args.length >= 3) {
            if (!sender.hasPermission("jibaiworkbench.open.others")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            Player target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) {
                plugin.messages().send(sender, "target-player-offline", MessageManager.vars("player", args[2]));
                return true;
            }
            if (plugin.menus().open(target, menuId)) {
                plugin.messages().send(sender, "menu-opened-other",
                        MessageManager.vars("player", target.getName(), "menu", menuId));
            }
            return true;
        }
        // 给自己打开
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("jibaiworkbench.open")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (plugin.menus().open(player, menuId)) {
            plugin.messages().send(sender, "menu-opened", MessageManager.vars("menu", menuId));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        java.util.Set<String> ids = plugin.menus().getMenus().keySet();
        sender.sendMessage(ColorUtil.colorize("&b已加载 " + ids.size() + " 个菜单："));
        sender.sendMessage(ColorUtil.colorize("&f" + String.join(", ", ids)));
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jibaiworkbench.create")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb create <模板> <菜单ID>"));
            return true;
        }
        String template = args[1].toLowerCase();
        String menuId = args[2];
        if (!plugin.templates().isTemplate(template)) {
            plugin.messages().send(sender, "template-not-found",
                    MessageManager.vars("template", template, "list", String.join(", ", TemplateManager.TEMPLATES)));
            return true;
        }
        if (plugin.menus().exists(menuId) || fileExists(menuId)) {
            plugin.messages().send(sender, "menu-already-exists", MessageManager.vars("menu", menuId));
            return true;
        }
        if (plugin.templates().createFromTemplate(template, menuId)) {
            plugin.reloadEverything();
            plugin.messages().send(sender, "menu-created",
                    MessageManager.vars("template", template, "menu", menuId));
        } else {
            plugin.messages().send(sender, "menu-already-exists", MessageManager.vars("menu", menuId));
        }
        return true;
    }

    private boolean handleCopy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jibaiworkbench.copy")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb copy <源> <目标>"));
            return true;
        }
        String source = args[1];
        String target = args[2];
        if (!fileExists(source)) {
            plugin.messages().send(sender, "menu-copy-source-missing", MessageManager.vars("source", source));
            return true;
        }
        if (fileExists(target)) {
            plugin.messages().send(sender, "menu-already-exists", MessageManager.vars("menu", target));
            return true;
        }
        if (plugin.templates().copyMenu(source, target)) {
            plugin.reloadEverything();
            plugin.messages().send(sender, "menu-copied", MessageManager.vars("source", source, "target", target));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("jibaiworkbench.reload")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.messages().send(sender, "reload-start");
        ValidationResult result = plugin.reloadEverything();
        plugin.messages().send(sender, "reload-done", MessageManager.vars(
                "menus", String.valueOf(plugin.menus().getMenus().size()),
                "templates", String.valueOf(plugin.templates().getTemplateCount())));
        if (result.hasErrors()) {
            plugin.messages().send(sender, "validate-summary", MessageManager.vars(
                    "errors", String.valueOf(result.getErrors().size()),
                    "warnings", String.valueOf(result.getWarnings().size())));
        }
        return true;
    }

    private boolean handleValidate(CommandSender sender) {
        if (!sender.hasPermission("jibaiworkbench.validate")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.messages().send(sender, "validate-start");
        ValidationResult result = new ValidationResult();
        // 只读校验：扫描文件，不替换当前已加载菜单缓存、不影响玩家正在打开的菜单
        int scanned = plugin.menus().validateAll(result);
        printValidation(sender, result, scanned);
        return true;
    }

    private void printValidation(CommandSender sender, ValidationResult result, int scannedCount) {
        if (result.isClean()) {
            plugin.messages().send(sender, "validate-clean",
                    MessageManager.vars("menus", String.valueOf(scannedCount)));
            return;
        }
        for (ValidationResult.Issue e : result.getErrors()) {
            sender.sendMessage(ColorUtil.colorize("&c[错误] &f" + e.file + " &7> &e" + e.menuId
                    + (e.buttonId != null ? " &7> &b" + e.buttonId : "")
                    + " &7: &f" + e.message + " &8(建议: " + e.suggest + ")"));
        }
        for (ValidationResult.Issue w : result.getWarnings()) {
            sender.sendMessage(ColorUtil.colorize("&6[警告] &f" + w.file + " &7> &e" + w.menuId
                    + (w.buttonId != null ? " &7> &b" + w.buttonId : "")
                    + " &7: &f" + w.message + " &8(建议: " + w.suggest + ")"));
        }
        plugin.messages().send(sender, "validate-summary", MessageManager.vars(
                "errors", String.valueOf(result.getErrors().size()),
                "warnings", String.valueOf(result.getWarnings().size())));
    }

    private boolean handlePreview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jibaiworkbench.preview")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb preview <菜单>"));
            return true;
        }
        String menuId = args[1];
        if (!plugin.menus().exists(menuId)) {
            plugin.messages().send(sender, "menu-not-found", MessageManager.vars("menu", menuId));
            return true;
        }
        // 预览 = 直接打开（管理员绕过打开权限限制由 open.* 处理）
        plugin.menus().open((Player) sender, menuId);
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jibaiworkbench.debug")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb debug <菜单>"));
            return true;
        }
        Menu menu = plugin.menus().getMenu(args[1]);
        if (menu == null) {
            plugin.messages().send(sender, "menu-not-found", MessageManager.vars("menu", args[1]));
            return true;
        }
        sender.sendMessage(ColorUtil.colorize("&b===== 菜单调试：" + menu.getId() + " ====="));
        sender.sendMessage(ColorUtil.colorize("&7文件：&f" + menu.getSourceFile()));
        sender.sendMessage(ColorUtil.colorize("&7标题：&f" + menu.getTitle()));
        sender.sendMessage(ColorUtil.colorize("&7行数：&f" + menu.getRows() + " &7(大小 " + menu.getSize() + ")"));
        sender.sendMessage(ColorUtil.colorize("&7打开权限：&f" + (menu.getPermission() == null ? "无" : menu.getPermission())));
        sender.sendMessage(ColorUtil.colorize("&7允许拿取：&f" + menu.isAllowTake() + " &7允许拖拽：&f" + menu.isAllowDrag()));
        sender.sendMessage(ColorUtil.colorize("&7按钮数：&f" + menu.getButtons().size()));
        for (MenuButton b : menu.getButtons().values()) {
            String type = b.getShop() != null ? "&a[商店]" : b.getReward() != null ? "&6[奖励]" : "&f[普通]";
            sender.sendMessage(ColorUtil.colorize("  " + type + " &b" + b.getId()
                    + " &7槽位" + b.getSlots() + " &7材质:" + b.getMaterial()));
        }
        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jibaiworkbench.giveitem")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "usage-error", MessageManager.vars("usage", "/jwb giveitem <玩家> <菜单>"));
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "target-player-offline", MessageManager.vars("player", args[1]));
            return true;
        }
        String menuId = args[2];
        if (!plugin.menus().exists(menuId)) {
            plugin.messages().send(sender, "menu-not-found", MessageManager.vars("menu", menuId));
            return true;
        }
        ItemStack item = ItemBuilder.of(Material.COMPASS, 1)
                .name("&b菜单快捷物品 &7- " + menuId)
                .lore(Arrays.asList("&7右键打开菜单：&e" + menuId))
                .glow(true)
                .build();
        item = plugin.menuItemBinding().tag(item, menuId);
        target.getInventory().addItem(item);
        plugin.messages().send(sender, "giveitem-done",
                MessageManager.vars("player", target.getName(), "menu", menuId));
        plugin.messages().send(target, "giveitem-received");
        return true;
    }

    private boolean fileExists(String menuId) {
        return plugin.templates().listMenuFiles().contains(menuId);
    }

    // ===== Tab 补全 =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "open", "list"));
            addIf(subs, sender, "jibaiworkbench.create", "create");
            addIf(subs, sender, "jibaiworkbench.copy", "copy");
            addIf(subs, sender, "jibaiworkbench.reload", "reload");
            addIf(subs, sender, "jibaiworkbench.validate", "validate");
            addIf(subs, sender, "jibaiworkbench.preview", "preview");
            addIf(subs, sender, "jibaiworkbench.debug", "debug");
            addIf(subs, sender, "jibaiworkbench.giveitem", "giveitem");
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            switch (sub) {
                case "open":
                case "preview":
                case "debug":
                case "copy":
                    // 只暴露有权限打开的现有菜单（易错点 20）
                    return filter(accessibleMenus(sender), args[1]);
                case "create":
                    return filter(TemplateManager.TEMPLATES, args[1]);
                case "giveitem":
                    return filter(onlinePlayerNames(), args[1]);
                default:
                    return java.util.Collections.emptyList();
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "open":
                    return filter(onlinePlayerNames(), args[2]);
                case "giveitem":
                    return filter(accessibleMenus(sender), args[2]);
                default:
                    return java.util.Collections.emptyList();
            }
        }
        return java.util.Collections.emptyList();
    }

    private List<String> accessibleMenus(CommandSender sender) {
        List<String> result = new ArrayList<>();
        for (Menu m : plugin.menus().getMenus().values()) {
            if (m.getPermission() == null || sender.hasPermission(m.getPermission())
                    || sender.hasPermission("jibaiworkbench.open.*")) {
                result.add(m.getId());
            }
        }
        return result;
    }

    private List<String> onlinePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName).collect(Collectors.toList());
    }

    private void addIf(List<String> list, CommandSender sender, String perm, String value) {
        if (sender.hasPermission(perm)) {
            list.add(value);
        }
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(p))
                .collect(Collectors.toList());
    }
}
