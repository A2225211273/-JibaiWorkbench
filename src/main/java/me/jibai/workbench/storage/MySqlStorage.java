package me.jibai.workbench.storage;

import java.util.UUID;

/**
 * MySQL 存储 —— 预留骨架。
 *
 * <p>当前版本尚未实现。主类在 storage.type=mysql 时会检测到本实现未就绪并回退到
 * {@link YamlStorage}，保证插件可用。连接参数已在 config.yml 的 storage.mysql 段预留。</p>
 *
 * <p>后续实现建议：使用连接池（HikariCP），表结构与 {@link YamlStorage} 对应，
 * 表名加 config 中的 table-prefix；所有读写走异步线程。</p>
 *
 * @author 即白
 */
public class MySqlStorage implements Storage {

    /** 是否已完成实现。当前为 false，主类据此回退 YAML。 */
    public static boolean isImplemented() {
        return false;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("MySqlStorage 尚未实现，请使用 yaml 存储。");
    }

    @Override
    public void close() {
    }

    @Override
    public boolean save() {
        return false;
    }

    @Override
    public boolean isRewardClaimed(UUID player, String key) {
        return false;
    }

    @Override
    public boolean setRewardClaimed(UUID player, String key, long timestamp) {
        return false;
    }

    @Override
    public long getRewardClaimTime(UUID player, String key) {
        return 0;
    }

    @Override
    public int getPurchaseCount(UUID player, String shopKey, String dayKey) {
        return 0;
    }

    @Override
    public boolean addPurchaseCount(UUID player, String shopKey, String dayKey) {
        return false;
    }

    @Override
    public boolean isPurchasedOnce(UUID player, String shopKey) {
        return false;
    }

    @Override
    public boolean setPurchasedOnce(UUID player, String shopKey) {
        return false;
    }

    @Override
    public long getCooldownUntil(UUID player, String key) {
        return 0;
    }

    @Override
    public boolean setCooldownUntil(UUID player, String key, long until) {
        return false;
    }
}
