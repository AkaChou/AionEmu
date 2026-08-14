package com.aionemu.loginserver.service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.dao.VipDAO;
import com.aionemu.loginserver.model.Vip;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号 VIP 状态服务：查询、自动启用与缺失账号同步。
 * Account VIP service: lookup, auto-enable and missing-account sync.
 */
@Slf4j
public final class VipService {

    private final VipDAO dao;

    /** 默认构造（解析 DAO）。 / Default constructor (resolves the DAO). */
    public VipService() {
        this(DAOManager.getDAO(VipDAO.class));
    }

    /** 以指定 DAO 构造。 / Constructs with the given DAO. */
    public VipService(VipDAO dao) {
        this.dao = dao;
    }

    /**
     * 按账号 ID 查询 VIP 记录。
     * Finds the VIP record by account id.
     *
     * @param accountId 账号 ID / account id
     * @return VIP 记录，无则 null / VIP record or null
     */
    public Vip findByAccountId(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("accountId must be positive");
        }
        return dao.findByAccountId(accountId);
    }

    /**
     * 配置启用时，为缺失的账号批量补齐 VIP 记录。
     * When enabled in config, syncs VIP records for missing accounts.
     *
     * @return 补齐的账号数 / number of synchronized accounts
     */
    public int syncMissingAccounts() {
        if (!VipConfig.AUTO_ENABLE) {
            return 0;
        }
        VipConfig.validate();
        int synchronizedAccounts = dao.syncMissingAccounts(VipConfig.AUTO_ENABLE_LEVEL);
        log.info(I18n.get("log.b4e1738c5a20", synchronizedAccounts, VipConfig.AUTO_ENABLE_LEVEL));
        return synchronizedAccounts;
    }

    /**
     * 配置启用时，为指定账号插入默认等级 VIP 记录（已存在则不操作）。
     * When enabled in config, inserts a default-level VIP record for the account (no-op if present).
     *
     * @param accountId 账号 ID / account id
     * @return 是否插入成功 / whether the insert succeeded
     */
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
