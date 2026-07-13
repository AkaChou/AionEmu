package com.aionemu.loginserver.service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.dao.VipDAO;
import com.aionemu.loginserver.model.Vip;

import lombok.extern.slf4j.Slf4j;

/**
 * Independent account VIP operations.
 */
@Slf4j
public final class VipService {

    private final VipDAO dao;

    public VipService() {
        this(DAOManager.getDAO(VipDAO.class));
    }

    public VipService(VipDAO dao) {
        this.dao = dao;
    }

    public Vip findByAccountId(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("accountId must be positive");
        }
        return dao.findByAccountId(accountId);
    }

    public int syncMissingAccounts() {
        if (!VipConfig.AUTO_ENABLE) {
            return 0;
        }
        VipConfig.validate();
        int synchronizedAccounts = dao.syncMissingAccounts(VipConfig.AUTO_ENABLE_LEVEL);
        log.info(I18n.get("log.b4e1738c5a20", synchronizedAccounts, VipConfig.AUTO_ENABLE_LEVEL));
        return synchronizedAccounts;
    }

    public boolean insertIfAbsent(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("accountId must be positive");
        }
        if (!VipConfig.AUTO_ENABLE) {
            return false;
        }
        VipConfig.validate();
        return dao.insertIfAbsent(accountId, VipConfig.AUTO_ENABLE_LEVEL);
    }
}
