package me.jibai.workbench.config;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.ColorUtil;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * messages.yml 读取与消息发送。
 *
 * <p>所有玩家可见文本都从这里取，支持 {@code {prefix}} 与任意 {@code {key}} 变量替换，
 * 并统一上色。找不到的键返回带键名的占位串，方便定位漏配。</p>
 *
 * @author 即白
 */
public class MessageManager {

    private final JibaiWorkbenchPlugin plugin;
    private FileConfiguration messages;
    private String prefix = "";

    public MessageManager(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
    }

    /** 加载 / 重载 messages.yml，若不存在则从 jar 释放默认文件。 */
    public void load(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        LogUtil.debug("messages.yml 已加载，共 " + messages.getKeys(false).size() + " 条消息键。");
    }

    /** 取一条消息（已上色，已替换 {prefix}），无额外变量。 */
    public String get(String key) {
        return get(key, new HashMap<>());
    }

    /** 取一条消息并替换变量。 */
    public String get(String key, Map<String, String> placeholders) {
        String raw = messages == null ? null : messages.getString(key);
        if (raw == null) {
            return ColorUtil.colorize("&c[缺少消息: " + key + "]");
        }
        raw = raw.replace("{prefix}", prefix);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return ColorUtil.colorize(raw);
    }

    /** 直接发送一条消息给接收者。 */
    public void send(CommandSender to, String key) {
        to.sendMessage(get(key));
    }

    /** 发送带变量的消息。 */
    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        to.sendMessage(get(key, placeholders));
    }

    /** 便捷构造变量 map，参数为 key1, val1, key2, val2 ... */
    public static Map<String, String> vars(String... kv) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }
}
