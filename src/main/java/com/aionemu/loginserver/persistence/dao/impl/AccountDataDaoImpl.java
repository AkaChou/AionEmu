package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.persistence.entity.AccountDataPo;
import com.aionemu.loginserver.persistence.mapper.AccountDataMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.util.LambdaUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class AccountDataDaoImpl extends ServiceImpl<AccountDataMapper, AccountDataPo> implements AccountDataDao {

	@Override
	public Account getAccount(String name) {
		try {
			AccountDataPo po = mapper.selectOneByQuery(QueryWrapper.create().eq(AccountDataPo::getName, name));
			return po == null ? null : toAccount(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7be9f6648605", name), e);
			return null;
		}
	}

	@Override
	public Account getAccount(int id) {
		try {
			AccountDataPo po = mapper.selectOneById(id);
			return po == null ? null : toAccount(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.443dba3838a7", id), e);
			return null;
		}
	}

	@Override
	public boolean insertAccount(Account account) {
		try {
			AccountDataPo po = toPo(account);
			po.setToll(0L);
			int rows = mapper.insert(po);
			if (rows > 0) {
				account.setId(po.getId());
				return true;
			}
			return false;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.34a80423f0dc"), e);
			return false;
		}
	}

	@Override
	public boolean updateAccount(Account account) {
		try {
			return mapper.update(toPo(account)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.40f6bd6c99c5", account.getId()), e);
			return false;
		}
	}

	@Override
	public void updateLastServer(int accountId, byte lastServer) {
		try {
			UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getLastServer, (int) lastServer)
				.where(AccountDataPo::getId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7a357f84a3ac", accountId), e);
		}
	}

	@Override
	public void updateLastIp(int accountId, String ip) {
		try {
			UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getLastIp, ip)
				.where(AccountDataPo::getId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a2c8c099cda2", accountId), e);
		}
	}

	@Override
	public String getLastIp(int accountId) {
		try {
			AccountDataPo po = mapper.selectOneById(accountId);
			return po != null && po.getLastIp() != null ? po.getLastIp() : "";
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6acadf7febd4", accountId), e);
			return "";
		}
	}

	@Override
	public boolean updateLastMac(int accountId, String mac) {
		try {
			return UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getLastMac, mac)
				.where(AccountDataPo::getId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8cb7e88bf3fb", accountId), e);
			return false;
		}
	}

	@Override
	public boolean updateMembership(int accountId) {
		try {
			QueryColumn oldMembership = LambdaUtil.getQueryColumn(AccountDataPo::getOldMembership);
			return UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getMembership, oldMembership)
				.set(AccountDataPo::getExpire, null)
				.where(AccountDataPo::getId).eq(accountId)
				.and(AccountDataPo::getExpire).lt(QueryMethods.currentTimestamp())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.971689f314b7", accountId), e);
			return false;
		}
	}

	@Override
	public void deleteInactiveAccounts(int daysOfInactivity) {
		try {
			mapper.deleteInactiveAccounts(daysOfInactivity);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}

	@Override
	public long getToll(int accountId) {
		try {
			AccountDataPo po = mapper.selectOneById(accountId);
			return po != null && po.getToll() != null ? po.getToll() : 0L;
		} catch (DataAccessException e) {
			return 0L;
		}
	}

	@Override
	public boolean updatePoints(int accountId, long points, long required) {
		try {
			return UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getToll, points - required)
				.where(AccountDataPo::getId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			return false;
		}
	}

	@Override
	public long getLuna(int accountId) {
		try {
			AccountDataPo po = mapper.selectOneById(accountId);
			return po != null && po.getLuna() != null ? po.getLuna() : 0L;
		} catch (DataAccessException e) {
			return 0L;
		}
	}

	@Override
	public void updateLuna(int accountId, long luna) {
		try {
			UpdateChain.of(AccountDataPo.class)
				.set(AccountDataPo::getLuna, (int) luna)
				.where(AccountDataPo::getId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a2c8c099cda2", accountId), e);
		}
	}

	private static Account toAccount(AccountDataPo po) {
		Account account = new Account();
		account.setId(po.getId());
		account.setName(po.getName());
		account.setPasswordHash(po.getPassword());
		account.setAccessLevel(byteValue(po.getAccessLevel()));
		account.setMembership(byteValue(po.getMembership()));
		account.setActivated(byteValue(po.getActivated()));
		account.setLastServer(byteValue(po.getLastServer()));
		account.setLastIp(po.getLastIp());
		account.setLastMac(po.getLastMac());
		account.setIpForce(po.getIpForce());
		account.setReturn(byteValue(po.getReturnAccount()));
		account.setReturnEnd(po.getReturnEnd());
		return account;
	}

	private static AccountDataPo toPo(Account account) {
		AccountDataPo po = new AccountDataPo();
		po.setId(account.getId());
		po.setName(account.getName());
		po.setPassword(account.getPasswordHash());
		po.setAccessLevel((int) account.getAccessLevel());
		po.setMembership((int) account.getMembership());
		po.setActivated((int) account.getActivated());
		po.setLastServer((int) account.getLastServer());
		po.setLastIp(account.getLastIp());
		po.setLastMac(account.getLastMac());
		po.setIpForce(account.getIpForce());
		po.setReturnAccount((int) account.getReturn());
		po.setReturnEnd(account.getReturnEnd());
		return po;
	}

	private static byte byteValue(Integer value) {
		return value == null ? 0 : value.byteValue();
	}
}
