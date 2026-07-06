package me.jibai.workbench.storage;

import java.util.UUID;

/**
 * 数据存储接口。玩家奖励领取记录、限购记录、冷却记录都走这里，
 * 不写进 config.yml（对应文档要求）。
 *
 * <p>默认实现为 {@link YamlStorage}；{@link SqliteStorage} / {@link MySqlStorage}
 * 为预留骨架，当前版本会在主类中回退到 YAML。</p>
 *
 * @author 即白
 */
public interface Storage {

    /** 初始化存储（打开文件 / 连接）。 */
    void init();

    /** 关闭存储（保存 / 断开连接），插件 onDisable 时调用。 */
    void close();

    /**
     * 立即持久化当前数据（YAML 落盘）。
     *
     * @return true=保存成功；false=保存失败（调用方应据此判断是否提示成功）
     */
    boolean save();

    // ===== 奖励领取记录 =====

    /**
     * 是否已领取指定奖励。
     *
     * @param player 玩家 UUID
     * @param key    奖励键
     * @return true=已领取（对于每日/每周奖励，指本周期内已领取）
     */
    boolean isRewardClaimed(UUID player, String key);

    /**
     * 记录一次奖励领取，写入当前时间戳。
     *
     * @return true=写入并落盘成功；false=保存失败
     */
    boolean setRewardClaimed(UUID player, String key, long timestamp);

    /**
     * 取上次领取时间戳，无记录返回 0。
     */
    long getRewardClaimTime(UUID player, String key);

    // ===== 限购记录（每日购买次数） =====

    /**
     * 取玩家某商品在指定「日期键」下的已购买次数。
     *
     * @param dayKey 形如 20260706 的日期键，用于每日限购归零
     */
    int getPurchaseCount(UUID player, String shopKey, String dayKey);

    /**
     * 已购买次数 +1。
     *
     * @return true=写入并落盘成功；false=保存失败
     */
    boolean addPurchaseCount(UUID player, String shopKey, String dayKey);

    /** 取一次性商品是否已购买。 */
    boolean isPurchasedOnce(UUID player, String shopKey);

    /**
     * 标记一次性商品已购买。
     *
     * @return true=写入并落盘成功；false=保存失败
     */
    boolean setPurchasedOnce(UUID player, String shopKey);

    // ===== 冷却记录 =====

    /**
     * 取冷却到期时间戳（毫秒），无记录返回 0。
     */
    long getCooldownUntil(UUID player, String key);

    /**
     * 设置冷却到期时间戳。
     *
     * @return true=写入并落盘成功；false=保存失败
     */
    boolean setCooldownUntil(UUID player, String key, long until);
}
