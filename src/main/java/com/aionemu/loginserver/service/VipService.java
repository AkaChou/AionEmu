package com.aionemu.loginserver.service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.configs.VipConfig;
import com.aionemu.loginserver.persistence.dao.AccountVipDao;
import com.aionemu.loginserver.model.Vip;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号 VIP 状态服务：查询、自动启用与缺失账号同步。
 * Account VIP service: lookup, auto-enable and missing-account sync.
 */
@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class VipService {

    private final AccountVipDao vipDao;

    /**
     * 按账号 ID 查询 VIP 记录。
     * Finds the VIP record by account id.
     * @param accountId 账号 ID / account id
     * @return VIP 记录，无则 null / VIP record or null
     */
    public Vip findByAccountId(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("accountId must be positive");
        }
        return vipDao.findByAccountId(accountId);
    }

    /**
     * 配置启用时，为缺失的账号批量补齐 VIP 记录。
     * When enabled in config, syncs VIP records for missing accounts.
     * @return 补齐的账号数 / number of synchronized accounts
     */
    public int syncMissingAccounts() {
        if (!VipConfig.AUTO_ENABLE) {
            return 0;
        }
        VipConfig.validate();
        int synchronizedAccounts = vipDao.syncMissingAccounts(VipConfig.AUTO_ENABLE_LEVEL);
        log.info(I18n.get("log.b4e1738c5a20", synchronizedAccounts, VipConfig.AUTO_ENABLE_LEVEL));
        return synchronizedAccounts;
    }

    /**
     * 配置启用时，为指定账号插入默认等级 VIP 记录（已存在则不操作）。
     * When enabled in config, inserts a default-level VIP record for the account (no-op if present).
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
        return vipDao.insertIfAbsent(accountId, VipConfig.AUTO_ENABLE_LEVEL);
    }
}
