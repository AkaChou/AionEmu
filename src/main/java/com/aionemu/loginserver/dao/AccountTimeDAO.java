package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.AccountTime;

/**
 * 账号会话/惩罚时间数据访问抽象层。
 * DAO that manages account time (session, penalty, expiration).
 */
public abstract class AccountTimeDAO implements DAO {

    /**
     * 写入或替换账号时间数据。
     * Updates {@link AccountTime} data of an account.
     *
     * 账号 ID / Account id
     * @param accountTime 账号时间数据 / Account time set
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean updateAccountTime(int accountId, AccountTime accountTime);

    /**
     * 读取账号时间数据。
     * Loads {@link AccountTime} data of an account.
     *
     * 账号 ID / Account id
     * @return 账号时间；不存在时为 null / AccountTime, or null if missing
     */
    public abstract AccountTime getAccountTime(int accountId);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return AccountTimeDAO.class.getName();
    }
}
