package me.jibai.workbench.util;

import org.bukkit.ChatColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本颜色解析工具。
 *
 * <p>本插件面向 Bukkit / Spigot / Paper / Purpur 全核心，为保证最大兼容性，
 * 颜色统一使用 Bukkit {@link ChatColor}，不依赖 Paper 的 Adventure API。</p>
 *
 * <p>为方便在 messages.yml / 菜单文件中书写，同时支持三种颜色写法，解析时统一转换：</p>
 * <ul>
 *   <li>MiniMessage 风格标签，如 {@code <green>}、{@code <gold>}、{@code <red>}；</li>
 *   <li>十六进制颜色，如 {@code &#FF5555} 或 {@code <#FF5555>}（在支持的服务端上生效）；</li>
 *   <li>传统 {@code &} 颜色码，如 {@code &a}、{@code &c}。</li>
 * </ul>
 *
 * @author 即白
 */
public final class ColorUtil {

    /** MiniMessage 风格颜色 / 格式标签到 ChatColor 的映射。 */
    private static final Map<String, ChatColor> TAG_TO_COLOR = new LinkedHashMap<>();

    /** 匹配形如 &lt;green&gt; 或 &lt;/green&gt; 的标签。 */
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-z_]+)>", Pattern.CASE_INSENSITIVE);

    /** 匹配十六进制颜色：&#RRGGBB 或 <#RRGGBB>。 */
    private static final Pattern HEX_PATTERN = Pattern.compile("[&<]#([0-9a-fA-F]{6})>?");

    static {
        TAG_TO_COLOR.put("black", ChatColor.BLACK);
        TAG_TO_COLOR.put("dark_blue", ChatColor.DARK_BLUE);
        TAG_TO_COLOR.put("dark_green", ChatColor.DARK_GREEN);
        TAG_TO_COLOR.put("dark_aqua", ChatColor.DARK_AQUA);
        TAG_TO_COLOR.put("dark_red", ChatColor.DARK_RED);
        TAG_TO_COLOR.put("dark_purple", ChatColor.DARK_PURPLE);
        TAG_TO_COLOR.put("gold", ChatColor.GOLD);
        TAG_TO_COLOR.put("gray", ChatColor.GRAY);
        TAG_TO_COLOR.put("grey", ChatColor.GRAY);
        TAG_TO_COLOR.put("dark_gray", ChatColor.DARK_GRAY);
        TAG_TO_COLOR.put("blue", ChatColor.BLUE);
        TAG_TO_COLOR.put("green", ChatColor.GREEN);
        TAG_TO_COLOR.put("aqua", ChatColor.AQUA);
        TAG_TO_COLOR.put("red", ChatColor.RED);
        TAG_TO_COLOR.put("light_purple", ChatColor.LIGHT_PURPLE);
        TAG_TO_COLOR.put("yellow", ChatColor.YELLOW);
        TAG_TO_COLOR.put("white", ChatColor.WHITE);
        // 格式
        TAG_TO_COLOR.put("bold", ChatColor.BOLD);
        TAG_TO_COLOR.put("b", ChatColor.BOLD);
        TAG_TO_COLOR.put("italic", ChatColor.ITALIC);
        TAG_TO_COLOR.put("i", ChatColor.ITALIC);
        TAG_TO_COLOR.put("underlined", ChatColor.UNDERLINE);
        TAG_TO_COLOR.put("strikethrough", ChatColor.STRIKETHROUGH);
        TAG_TO_COLOR.put("obfuscated", ChatColor.MAGIC);
        TAG_TO_COLOR.put("reset", ChatColor.RESET);
    }

    private ColorUtil() {
    }

    /**
     * 将文本解析为带 {@link ChatColor} 颜色码的字符串，可发送给玩家或控制台。
     *
     * @param input 原始文本，可混用标签、hex 与传统 &amp; 颜色码；null 视为空串
     * @return 已上色的字符串
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // 先处理十六进制颜色
        String withHex = replaceHex(input);
        // 再处理 <tag> 标签
        String withTags = replaceTags(withHex);
        // 最后处理传统 & 颜色码
        return ChatColor.translateAlternateColorCodes('&', withTags);
    }

    /**
     * 去除文本中的所有颜色（标签、hex 与 &amp; 码都清除），用于写入文件等纯文本场景。
     */
    public static String stripColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String colored = colorize(input);
        return ChatColor.stripColor(colored);
    }

    /**
     * 把十六进制颜色替换为 {@link net.md_5.bungee.api.ChatColor} 的形式。
     * 使用反射避免在低版本服务端上因缺少 hex 支持导致类加载失败。
     */
    private static String replaceHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = buildHex(hex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 构造 §x§R§R§G§G§B§B 形式的 hex 颜色码，兼容 1.16+；低版本自动退化为空（被忽略）。 */
    private static String buildHex(String hex) {
        try {
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append('§').append(c);
            }
            return magic.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * 把 MiniMessage 风格标签替换为对应的 {@link ChatColor} 颜色码；
     * 未知标签保持原样，闭合标签（如 {@code </green>}）直接移除。
     */
    private static String replaceTags(String input) {
        Matcher matcher = TAG_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String closing = matcher.group(1);
            String name = matcher.group(2).toLowerCase();
            ChatColor color = TAG_TO_COLOR.get(name);
            if (color == null) {
                // 未知标签，原样保留（用 quoteReplacement 避免 $ 等特殊字符问题）
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            } else if (!closing.isEmpty()) {
                // 闭合标签统一移除（ChatColor 无区间概念）
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(color.toString()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
