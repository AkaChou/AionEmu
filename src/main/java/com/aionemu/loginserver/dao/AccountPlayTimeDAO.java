package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.AccountTime;

/**
 * 账号累计在线时长数据访问抽象层。
 * DAO that manages accumulated account play time.
 */
public abstract class AccountPlayTimeDAO implements DAO {

    /**
     * 累加更新账号在线时长。
     * Accumulates and updates account online play time.
     *
     * 账号 ID / Account id
     * @param accountTime 含在线时长的账号时间对象 / Account time with online duration
     * @return 是否更新成功 / True if updated
     */
    public abstract boolean update(final Integer accountId, final AccountTime accountTime);

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    public final String getClassName() {
        return AccountPlayTimeDAO.class.getName();
    }
}
