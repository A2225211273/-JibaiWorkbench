package me.jibai.workbench.storage;

import java.util.UUID;

/**
 * SQLite 存储 —— 预留骨架。
 *
 * <p>当前版本尚未实现 JDBC 逻辑（避免引入未经测试的数据库代码）。
 * 主类在 storage.type=sqlite 时会检测到本实现未就绪并回退到 {@link YamlStorage}，
 * 以保证插件始终可用。</p>
 *
 * <p>后续实现建议：</p>
 * <ul>
 *   <li>在 build.gradle.kts 加入 {@code org.xerial:sqlite-jdbc} 并 shade 进 jar；</li>
 *   <li>连接 {@code jdbc:sqlite:plugins/JibaiWorkbench/data/data.db}；</li>
 *   <li>建表 rewards / purchases / cooldowns，字段与 {@link YamlStorage} 结构对应；</li>
 *   <li>数据库读写放异步线程，结果回主线程使用（对应线程安全要求）。</li>
 * </ul>
 *
 * @author 即白
 */
public class SqliteStorage implements Storage {

    /** 是否已完成实现。当前为 false，主类据此回退 YAML。 */
    public static boolean isImplemented() {
        return false;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("SqliteStorage 尚未实现，请使用 yaml 存储。");
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
