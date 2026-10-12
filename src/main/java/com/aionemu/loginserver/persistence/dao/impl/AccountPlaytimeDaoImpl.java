package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.persistence.dao.AccountPlaytimeDao;
import com.aionemu.loginserver.persistence.entity.AccountPlaytimePo;
import com.aionemu.loginserver.persistence.mapper.AccountPlaytimeMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号累计在线时长数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class AccountPlaytimeDaoImpl extends ServiceImpl<AccountPlaytimeMapper, AccountPlaytimePo> implements AccountPlaytimeDao {

	@Override
	public boolean update(Integer accountId, AccountTime accountTime) {
		try {
			long onlineTime = accountTime.getAccumulatedOnlineTime();
			return mapper.accumulateOnlineTime(accountId, onlineTime) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f2c20e9730e3", accountId), e);
			return false;
		}
	}
}
