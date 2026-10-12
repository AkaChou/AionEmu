package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.persistence.dao.AccountVipDao;
import com.aionemu.loginserver.persistence.entity.AccountVipPo;
import com.aionemu.loginserver.persistence.mapper.AccountVipMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号 VIP 状态数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class AccountVipDaoImpl extends ServiceImpl<AccountVipMapper, AccountVipPo> implements AccountVipDao {

	@Override
	public Vip findByAccountId(int accountId) {
		try {
			AccountVipPo po = mapper.selectOneById(accountId);
			return po == null ? null : new Vip(po.getAccountId(), po.getVipLevel(),
				longValue(po.getVipExp()), longValue(po.getExpireTime()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.acdac4f0d5c7", accountId), e);
			return null;
		}
	}

	@Override
	public int syncMissingAccounts(int level) {
		try {
			return mapper.syncMissingAccounts(level);
		} catch (DataAccessException e) {
			throw new IllegalStateException("Could not synchronize missing account VIP data", e);
		}
	}

	@Override
	public boolean insertIfAbsent(int accountId, int level) {
		try {
			return mapper.insertIfAbsent(accountId, level) > 0;
		} catch (DataAccessException e) {
			return false;
		}
	}

	private static long longValue(java.math.BigInteger value) {
		return value == null ? 0L : value.longValue();
	}
}
