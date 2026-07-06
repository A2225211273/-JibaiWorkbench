package me.jibai.workbench.template;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 模板管理：内置模板列表、首次释放默认菜单、从模板创建菜单、复制菜单。
 *
 * <p>模板文件打包在 jar 的 templates/ 目录，同时会释放到插件数据目录的 templates/
 * 下供服主查看参考。</p>
 *
 * @author 即白
 */
public class TemplateManager {

    /** 内置模板 ID 列表（对应 resources/templates/*.yml）。 */
    public static final List<String> TEMPLATES = Arrays.asList(
            "main", "shop", "activity", "vip", "reward", "guide", "teleport", "rules");

    private final JibaiWorkbenchPlugin plugin;

    public TemplateManager(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    public int getTemplateCount() {
        return TEMPLATES.size();
    }

    public boolean isTemplate(String id) {
        return TEMPLATES.contains(id.toLowerCase());
    }

    /** 把 jar 内模板释放到 数据目录/templates/，方便服主查看（已存在不覆盖）。 */
    public void extractTemplates() {
        File dir = new File(plugin.getDataFolder(), "templates");
        if (!dir.exists() && !dir.mkdirs()) {
            LogUtil.warn("无法创建 templates 目录。");
        }
        for (String tpl : TEMPLATES) {
            File out = new File(dir, tpl + ".yml");
            if (!out.exists()) {
                try {
                    plugin.saveResource("templates/" + tpl + ".yml", false);
                } catch (Throwable t) {
                    LogUtil.warn("释放模板失败：" + tpl + " -> " + t.getMessage());
                }
            }
        }
    }

    /**
     * 首次启动 menus/ 为空时，释放默认菜单。默认释放 main / shop / activity /
     * vip / reward / guide / teleport / rules 全套，让服主开箱即用。
     */
    public void generateDefaultMenus() {
        for (String tpl : TEMPLATES) {
            createFromTemplate(tpl, tpl); // 菜单 id 与模板同名
        }
    }

    /**
     * 从模板创建一个新菜单文件到 menus/ 目录。
     *
     * @param template 模板 ID
     * @param menuId   新菜单 ID
     * @return 是否成功创建（目标已存在或模板不存在返回 false）
     */
    public boolean createFromTemplate(String template, String menuId) {
        if (!isTemplate(template)) {
            return false;
        }
        File menusDir = new File(plugin.getDataFolder(), "menus");
        if (!menusDir.exists() && !menusDir.mkdirs()) {
            LogUtil.warn("无法创建 menus 目录。");
            return false;
        }
        File target = new File(menusDir, menuId + ".yml");
        if (target.exists()) {
            return false;
        }

        // 从 jar 读取模板内容
        try (InputStream in = plugin.getResource("templates/" + template + ".yml")) {
            if (in == null) {
                LogUtil.warn("jar 中缺少模板：" + template);
                return false;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            // 改写 id 为新菜单 ID
            yaml.set("id", menuId);
            yaml.save(target);
            LogUtil.debug("已从模板 " + template + " 创建菜单：" + menuId);
            return true;
        } catch (IOException e) {
            LogUtil.error("从模板创建菜单失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 复制已有菜单为新菜单。
     *
     * @return 是否成功（源不存在或目标已存在返回 false）
     */
    public boolean copyMenu(String source, String target) {
        File menusDir = new File(plugin.getDataFolder(), "menus");
        File src = new File(menusDir, source + ".yml");
        File dst = new File(menusDir, target + ".yml");
        if (!src.exists() || dst.exists()) {
            return false;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(src);
            yaml.set("id", target);
            yaml.save(dst);
            LogUtil.debug("已复制菜单 " + source + " -> " + target);
            return true;
        } catch (IOException e) {
            LogUtil.error("复制菜单失败：" + e.getMessage());
            return false;
        }
    }

    public List<String> listMenuFiles() {
        List<String> result = new ArrayList<>();
        File menusDir = new File(plugin.getDataFolder(), "menus");
        File[] files = menusDir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                result.add(name.substring(0, name.length() - 4));
            }
        }
        return result;
    }
}
