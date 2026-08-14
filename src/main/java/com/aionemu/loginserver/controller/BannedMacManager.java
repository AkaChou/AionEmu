package com.aionemu.loginserver.controller;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.model.base.BannedMacEntry;

import lombok.Getter;

/**
 * MAC 封禁管理器：内存缓存 + 数据库持久化。
 * MAC ban manager: in-memory cache with database persistence.
 *
 * @author KID
 */
public class BannedMacManager {

    private BannedMacDAO dao = DAOManager.getDAO(BannedMacDAO.class);

    /**
     * 当前封禁 MAC 映射。
     * Current banned MAC map.
     */
    @Getter
    private Map<String, BannedMacEntry> bannedList = new ConcurrentHashMap<>();

    /**
     * 获取单例（遗留入口，启动迁移后弃用）。
     * Returns singleton (legacy entry, deprecated after boot migration).
     *
     * @return 管理器实例 / Manager instance
     * @deprecated 优先使用注入 / prefer injection
     */
    @Deprecated(since = "boot-migration")
    public static BannedMacManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 构造并加载全部 MAC 封禁。
     * Constructs manager and loads all MAC bans.
     */
    public BannedMacManager() {
        bannedList = dao.load();
    }

    /**
     * 解除 MAC 封禁并写库。
     * Unbans MAC and removes from database.
     *
     * @param address MAC 地址 / MAC address
     * @param details 备注（保留参数） / Details (kept for API)
     */
    public void unban(String address, String details) {
        if (bannedList.containsKey(address)) {
            bannedList.remove(address);
            dao.remove(address);
        }
    }

    /**
     * 封禁 MAC 并持久化。
     * Bans MAC and persists the entry.
     *
     * @param address MAC 地址 / MAC address
     * @param time 到期时间戳（毫秒） / Expiration epoch millis
     * @param details 备注 / Details
     */
    public void ban(String address, long time, String details) {
        BannedMacEntry mac = new BannedMacEntry(address, new Timestamp(time), details);
        this.bannedList.put(address, mac);
        this.dao.update(mac);
    }

    /**
     * 返回封禁映射（兼容旧 API）。
     * Returns ban map (legacy API alias).
     *
     * @return 封禁映射 / banned MAC map
     */
    public final Map<String, BannedMacEntry> getMap() {
        return this.bannedList;
    }

    private static final class SingletonHolder {

        private static final BannedMacManager INSTANCE = new BannedMacManager();
    }
}
