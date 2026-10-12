package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.persistence.dao.AccountTimeDao;
import com.aionemu.loginserver.persistence.entity.AccountTimePo;
import com.aionemu.loginserver.persistence.mapper.AccountTimeMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号会话/惩罚时间数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class AccountTimeDaoImpl extends ServiceImpl<AccountTimeMapper, AccountTimePo> implements AccountTimeDao {

	@Override
	public boolean updateAccountTime(int accountId, AccountTime accountTime) {
		try {
			int rows = mapper.replaceAccountTime(accountId, accountTime.getLastLoginTime(),
				accountTime.getExpirationTime(), Math.toIntExact(accountTime.getSessionDuration()),
				Math.toIntExact(accountTime.getAccumulatedOnlineTime()), Math.toIntExact(accountTime.getAccumulatedRestTime()),
				accountTime.getPenaltyEnd());
			return rows > 0;
		} catch (DataAccessException | ArithmeticException e) {
			log.error(I18n.get("log.ff2d8c2ec1de", accountId), e);
			return false;
		}
	}

	@Override
	public AccountTime getAccountTime(int accountId) {
		try {
			AccountTimePo po = mapper.selectOneById(accountId);
			if (po == null) {
				return null;
			}
			AccountTime accountTime = new AccountTime();
			accountTime.setLastLoginTime(po.getLastActive());
			accountTime.setSessionDuration(longValue(po.getSessionDuration()));
			accountTime.setAccumulatedOnlineTime(longValue(po.getAccumulatedOnline()));
			accountTime.setAccumulatedRestTime(longValue(po.getAccumulatedRest()));
			accountTime.setPenaltyEnd(po.getPenaltyEnd());
			accountTime.setExpirationTime(po.getExpirationTime());
			return accountTime;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.acdac4f0d5c7", accountId), e);
			return null;
		}
	}

	private static long longValue(Integer value) {
		return value == null ? 0L : value;
	}
}
