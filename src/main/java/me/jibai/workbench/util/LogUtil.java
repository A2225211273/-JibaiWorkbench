package me.jibai.workbench.util;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志工具，统一走插件 {@link Logger}，并支持一个全局 debug 开关。
 *
 * <p>debug 关闭时，{@link #debug(String)} 不输出，避免刷屏。</p>
 *
 * @author 即白
 */
public final class LogUtil {

    private static Logger logger;
    private static boolean debugEnabled;

    private LogUtil() {
    }

    /** 由主类在 onEnable 时初始化。 */
    public static void init(Plugin plugin) {
        logger = plugin.getLogger();
    }

    public static void setDebug(boolean debug) {
        debugEnabled = debug;
    }

    public static boolean isDebug() {
        return debugEnabled;
    }

    public static void info(String msg) {
        if (logger != null) {
            logger.info(msg);
        }
    }

    public static void warn(String msg) {
        if (logger != null) {
            logger.warning(msg);
        }
    }

    public static void error(String msg) {
        if (logger != null) {
            logger.severe(msg);
        }
    }

    public static void error(String msg, Throwable t) {
        if (logger != null) {
            logger.log(Level.SEVERE, msg, t);
        }
    }

    public static void debug(String msg) {
        if (debugEnabled && logger != null) {
            logger.info("[DEBUG] " + msg);
        }
    }
}
