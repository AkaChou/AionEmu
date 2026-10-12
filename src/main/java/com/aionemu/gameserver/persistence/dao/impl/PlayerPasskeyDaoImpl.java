package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.PlayerPasskeyDao;
import com.aionemu.gameserver.persistence.entity.PlayerPasskeyPo;
import com.aionemu.gameserver.persistence.mapper.PlayerPasskeyMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家二级密码数据访问实现。
 * Data access implementation for player passkey.
 */
@Slf4j
@Service
@Lazy
public class PlayerPasskeyDaoImpl extends ServiceImpl<PlayerPasskeyMapper, PlayerPasskeyPo>
	implements PlayerPasskeyDao {

	@Override
	public void insertPlayerPasskey(int accountId, String passkey) {
		try {
			PlayerPasskeyPo po = new PlayerPasskeyPo();
			po.setAccountId(accountId);
			po.setPasskey(passkey);
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.cf0e27a382ab", accountId), e);
		}
	}

	@Override
	public boolean updatePlayerPasskey(int accountId, String oldPasskey, String newPasskey) {
		try {
			return UpdateChain.of(PlayerPasskeyPo.class)
				.set(PlayerPasskeyPo::getPasskey, newPasskey)
				.where(PlayerPasskeyPo::getAccountId).eq(accountId)
				.and(PlayerPasskeyPo::getPasskey).eq(oldPasskey)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9cef2da6c580", accountId), e);
			return false;
		}
	}

	@Override
	public void updateForcePlayerPasskey(int accountId, String newPasskey) {
		try {
			UpdateChain.of(PlayerPasskeyPo.class)
				.set(PlayerPasskeyPo::getPasskey, newPasskey)
				.where(PlayerPasskeyPo::getAccountId).eq(accountId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9cef2da6c580", accountId), e);
		}
	}

	@Override
	public boolean checkPlayerPasskey(int accountId, String passkey) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create()
				.eq(PlayerPasskeyPo::getAccountId, accountId)
				.eq(PlayerPasskeyPo::getPasskey, passkey)) == 1;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c22df4b9c35d", accountId), e);
			return false;
		}
	}

	@Override
	public boolean existCheckPlayerPasskey(int accountId) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create()
				.eq(PlayerPasskeyPo::getAccountId, accountId)) == 1;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c22df4b9c35d", accountId), e);
			return false;
		}
	}
}
