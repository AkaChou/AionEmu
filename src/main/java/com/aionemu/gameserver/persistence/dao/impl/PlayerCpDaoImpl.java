package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.cp.PlayerCPEntry;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerCpDao;
import com.aionemu.gameserver.persistence.entity.PlayerCpPo;
import com.aionemu.gameserver.persistence.mapper.PlayerCpMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家创造力点数（CP）数据访问实现。
 * Data access implementation for player creativity points.
 */
@Slf4j
@Service
@Lazy
public class PlayerCpDaoImpl extends ServiceImpl<PlayerCpMapper, PlayerCpPo> implements PlayerCpDao {

	@Override
	public PlayerCPList loadCP(Player player) {
		List<PlayerCPEntry> cp = new ArrayList<>();

		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerCpPo::getPlayerId).eq(player.getObjectId()))
				.forEach(po -> cp.add(new PlayerCPEntry(po.getSlot(), po.getPoint(), PersistentState.UPDATED)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f66364d8a626", player.getObjectId(), " from DB"), e);
		}
		return new PlayerCPList(cp);
	}

	@Override
	public void storeCP(int objectId, int slot, int point) {
		try {
			mapper.upsert(objectId, slot, point);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ca5c95d176db", objectId, " from DB"), e);
		}
	}

	@Override
	public void deleteCP(int objectId, int slot) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerCpPo::getPlayerId).eq(objectId)
				.and(PlayerCpPo::getSlot).eq(slot));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.55ed70097989", objectId, " from DB"), e);
		}
	}

	@Override
	public int getSlotSize(int playerObjId) {
		try {
			return (int) mapper.selectCountByQuery(QueryWrapper.create()
				.where(PlayerCpPo::getPlayerId).eq(playerObjId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ddd6a72fd9a0", playerObjId), e);
			return 0;
		}
	}
}
