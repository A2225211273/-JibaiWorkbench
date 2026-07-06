package me.jibai.workbench.storage;

import me.jibai.workbench.JibaiWorkbenchPlugin;
import me.jibai.workbench.util.LogUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 基于 data/players.yml 的默认存储实现，适合中小型服务器。
 *
 * <p>结构（players.yml）：</p>
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     rewards:
 *       &lt;key&gt;: &lt;时间戳&gt;
 *     purchases:
 *       daily:
 *         &lt;shopKey&gt;:
 *           &lt;dayKey&gt;: &lt;次数&gt;
 *       once:
 *         &lt;shopKey&gt;: true
 *     cooldowns:
 *       &lt;key&gt;: &lt;到期时间戳&gt;
 * </pre>
 *
 * <p>所有读写都在主线程调用（点击事件回调内），YAML 内存操作足够快，
 * 落盘用 {@link #save()} 汇总，避免每次点击都 IO。</p>
 *
 * @author 即白
 */
public class YamlStorage implements Storage {

    private final JibaiWorkbenchPlugin plugin;
    private final File file;
    private FileConfiguration data;

    public YamlStorage(JibaiWorkbenchPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data" + File.separator + "players.yml");
    }

    @Override
    public void init() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                LogUtil.warn("无法创建 data 目录：" + dir.getPath());
            }
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    LogUtil.warn("无法创建 players.yml。");
                }
            }
            this.data = YamlConfiguration.loadConfiguration(file);
            LogUtil.info("YAML 存储已就绪：" + file.getPath());
        } catch (IOException e) {
            LogUtil.error("初始化 YAML 存储失败，将使用内存临时存储：" + e.getMessage());
            this.data = new YamlConfiguration();
        }
    }

    @Override
    public void close() {
        save();
    }

    @Override
    public synchronized boolean save() {
        if (data == null) {
            return false;
        }
        try {
            data.save(file);
            return true;
        } catch (IOException e) {
            // 保存失败必须记录，绝不能静默（对应文档易错点 15）
            LogUtil.error("保存 players.yml 失败：" + file.getPath() + " -> " + e.getMessage());
            return false;
        }
    }

    private String base(UUID player) {
        return "players." + player;
    }

    @Override
    public boolean isRewardClaimed(UUID player, String key) {
        return getRewardClaimTime(player, key) > 0;
    }

    @Override
    public synchronized boolean setRewardClaimed(UUID player, String key, long timestamp) {
        String path = base(player) + ".rewards." + key;
        Object old = data.get(path);
        data.set(path, timestamp);
        if (!save()) {
            data.set(path, old); // 落盘失败回滚内存，避免与磁盘不一致
            return false;
        }
        return true;
    }

    @Override
    public long getRewardClaimTime(UUID player, String key) {
        return data.getLong(base(player) + ".rewards." + key, 0L);
    }

    @Override
    public int getPurchaseCount(UUID player, String shopKey, String dayKey) {
        return data.getInt(base(player) + ".purchases.daily." + shopKey + "." + dayKey, 0);
    }

    @Override
    public synchronized boolean addPurchaseCount(UUID player, String shopKey, String dayKey) {
        String path = base(player) + ".purchases.daily." + shopKey + "." + dayKey;
        Object old = data.get(path);
        data.set(path, data.getInt(path, 0) + 1);
        if (!save()) {
            data.set(path, old);
            return false;
        }
        return true;
    }

    @Override
    public boolean isPurchasedOnce(UUID player, String shopKey) {
        return data.getBoolean(base(player) + ".purchases.once." + shopKey, false);
    }

    @Override
    public synchronized boolean setPurchasedOnce(UUID player, String shopKey) {
        String path = base(player) + ".purchases.once." + shopKey;
        Object old = data.get(path);
        data.set(path, true);
        if (!save()) {
            data.set(path, old);
            return false;
        }
        return true;
    }

    @Override
    public long getCooldownUntil(UUID player, String key) {
        return data.getLong(base(player) + ".cooldowns." + key, 0L);
    }

    @Override
    public synchronized boolean setCooldownUntil(UUID player, String key, long until) {
        String path = base(player) + ".cooldowns." + key;
        Object old = data.get(path);
        data.set(path, until);
        if (!save()) {
            data.set(path, old);
            return false;
        }
        return true;
    }
}
