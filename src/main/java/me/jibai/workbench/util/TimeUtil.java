package me.jibai.workbench.util;

/**
 * 时间格式化工具，全部使用标准 JDK API，无第三方依赖。
 *
 * @author 即白
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * 把毫秒级剩余时间格式化为「X天X时X分X秒」的中文可读形式。
     *
     * @param millis 剩余毫秒，&lt;=0 返回「已就绪」
     * @return 中文时间描述
     */
    public static String formatRemaining(long millis) {
        if (millis <= 0) {
            return "已就绪";
        }
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分");
        }
        // 当只有秒或需要补充秒时显示
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        return sb.toString();
    }
}
