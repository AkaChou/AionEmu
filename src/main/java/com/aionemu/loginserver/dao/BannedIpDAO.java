package com.aionemu.loginserver.dao;

import java.sql.Timestamp;
import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.BannedIP;

/**
 * IP 封禁数据访问抽象层。
 * DAO that manages banned IPs.
 *
 * @author SoulKeeper
 */
public abstract class BannedIpDAO implements DAO {

    /**
     * 插入永久 IP 封禁（无过期时间）。
     * Inserts an IP mask with no expire time (never expires).
     *
     * IP mask to ban
     * @return 封禁对象；失败时为 null / BannedIP, or null on error
     */
    public abstract BannedIP insert(String mask);

    /**
     * 插入带过期时间的 IP 封禁；过期时间为 null 表示永久。
     * Inserts an IP mask with expire time; null means infinite ban.
     *
     * IP mask to ban
     * Expiration time of ban
     * @return 封禁对象；失败时为 null / BannedIP, or null on error
     */
    public abstract BannedIP insert(String mask, Timestamp expireTime);

    /**
     * 插入封禁记录；对象 ID 必须为 null，成功时回写生成 ID。
     * Inserts a BannedIP whose id must be null; sets generated id on success.
     *
     * @param bannedIP 待插入记录 / Record to add
     * @return 是否插入成功 / True if inserted
     */
    public abstract boolean insert(BannedIP bannedIP);

    /**
     * 更新封禁记录；对象 ID 不可为 null。
     * Updates a BannedIP whose id must not be null.
     *
     * @param bannedIP 待更新记录 / Record to update
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean update(BannedIP bannedIP);

    /**
     * 按 IP 掩码删除封禁。
     * Removes ban by mask.
     *
     * IP mask to remove
     * @return 是否删除成功 / True if removed
     */
    public abstract boolean remove(String mask);

    /**
     * 按对象删除封禁（ID 不可为 null）。
     * Removes BannedIP record by object (id must not be null).
     *
     * @param bannedIP 待解封记录 / Record to unban
     * @return 是否删除成功 / True if removed
     */
    public abstract boolean remove(BannedIP bannedIP);

    /**
     * 加载全部 IP 封禁。
     * Returns all bans from database.
     *
     * @return 全部封禁集合 / All bans
     */
    public abstract Set<BannedIP> getAllBans();

    /**
     * 清理已过期的 IP 封禁。
     * Deletes expired IP bans.
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
        return BannedIpDAO.class.getName();
    }
}
