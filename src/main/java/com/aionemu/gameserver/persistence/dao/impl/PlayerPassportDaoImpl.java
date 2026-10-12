package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.PlayerPassportDao;
import com.aionemu.gameserver.persistence.entity.PlayerPassportPo;
import com.aionemu.gameserver.persistence.mapper.PlayerPassportMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家护照数据访问实现。
 * Data access implementation for player passports.
 * @author Alcapwnd, Lyras, FrozenKiller
 */
@Slf4j
@Service
@Lazy
public class PlayerPassportDaoImpl extends ServiceImpl<PlayerPassportMapper, PlayerPassportPo>
	implements PlayerPassportDao {

	@Override
	public void insertPassport(int accountId, int passportId, int stamps, Timestamp lastStamp) {
		try {
			PlayerPassportPo po = new PlayerPassportPo();
			po.setAccountId(accountId);
			po.setPassportId(passportId);
			po.setStamps(stamps);
			po.setLastStamp(lastStamp);
			mapper.insertSelective(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.06e64ddd17f9", e.getMessage()), e);
		}
	}

	@Override
	public void updatePassport(int accountId, int passportId, int stamps, boolean rewarded, Timestamp lastStamp) {
		try {
			UpdateChain.of(PlayerPassportPo.class)
				.set(PlayerPassportPo::getStamps, stamps)
				.set(PlayerPassportPo::getRewarded, rewarded ? 1 : 0)
				.set(PlayerPassportPo::getLastStamp, lastStamp)
				.where(PlayerPassportPo::getAccountId).eq(accountId)
				.and(PlayerPassportPo::getPassportId).eq(passportId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b7b8fa49af32"), e);
		}
	}

	@Override
	public int getStamps(int accountId, int passportId) {
		try {
			PlayerPassportPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.eq(PlayerPassportPo::getAccountId, accountId)
				.eq(PlayerPassportPo::getPassportId, passportId));
			return po == null || po.getStamps() == null ? 0 : po.getStamps();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d8f278a870b7", accountId, passportId), e);
			return 0;
		}
	}

	@Override
	public Timestamp getLastStamp(int accountId, int passportId) {
		try {
			PlayerPassportPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.eq(PlayerPassportPo::getAccountId, accountId)
				.eq(PlayerPassportPo::getPassportId, passportId));
			if (po != null && po.getLastStamp() != null) {
				return po.getLastStamp();
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.52b96dcdcf0e", accountId), e);
		}

		return new Timestamp(System.currentTimeMillis());
	}

	@Override
	public List<Integer> getPassports(int accountId) {
		try {
			return mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerPassportPo::getAccountId, accountId)).stream()
				.map(PlayerPassportPo::getPassportId)
				.toList();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.add8e8df4ec5", accountId), e);
			return List.of();
		}
	}
}
