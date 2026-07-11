package com.aionemu.loginserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.base.BannedMacEntry;

/**
 * MAC 封禁数据访问抽象层。
 * DAO that manages banned MAC addresses.
 *
 * @author KID
 */
public abstract class BannedMacDAO implements DAO {

    /**
     * 写入或替换一条 MAC 封禁。
     * Inserts or replaces a banned MAC entry.
     *
     * @param entry 封禁条目 / Banned MAC entry
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean update(BannedMacEntry entry);

    /**
     * 按 MAC 地址删除封禁。
     * Removes ban by MAC address.
     *
     * MAC address
     * @return 是否删除成功 / True if removed
     */
    public abstract boolean remove(String address);

    /**
     * 加载全部 MAC 封禁。
     * Loads all banned MAC entries.
     *
     * @return MAC → 封禁条目映射 / Map of address to entry
     */
    public abstract Map<String, BannedMacEntry> load();

    /**
     * 清理已过期的 MAC 封禁。
     * Deletes expired MAC bans.
     */
    public abstract void cleanExpiredBans();

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return BannedMacDAO.class.getName();
    }
}
