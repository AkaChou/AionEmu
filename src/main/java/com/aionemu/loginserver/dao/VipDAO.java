package com.aionemu.loginserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.model.Vip;

/**
 * Independent account VIP persistence.
 */
public abstract class VipDAO implements DAO {

    public abstract Vip findByAccountId(int accountId);

    public abstract int syncMissingAccounts(int level);

    public abstract boolean insertIfAbsent(int accountId, int level);

    @Override
    public final String getClassName() {
        return VipDAO.class.getName();
    }
}
