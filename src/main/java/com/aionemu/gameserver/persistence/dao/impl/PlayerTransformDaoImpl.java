package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.entity.PlayerTransformPo;
import com.aionemu.gameserver.persistence.mapper.PlayerTransformMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家变身面板数据访问实现。
 * Data access implementation for player transformation panel persistence.
 */
@Slf4j
@Service
@Lazy
public class PlayerTransformDaoImpl extends ServiceImpl<PlayerTransformMapper, PlayerTransformPo>
	implements PlayerTransformDao {

	@Override
	public void loadPlTransfo(Player player) {
		try {
			for (PlayerTransformPo po : mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerTransformPo::getPlayerId, player.getObjectId()))) {
				player.getTransformModel().setPanelId(po.getPanelId());
				player.getTransformModel().setItemId(po.getItemId());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.81bdb310b4b8", player.getObjectId()), e);
		}
	}

	@Override
	public boolean storePlTransfo(int playerId, int panelId, int itemId) {
		try {
			return mapper.upsert(playerId, panelId, itemId) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0ec4dcbf87ba", playerId), e);
			return false;
		}
	}

	@Override
	public boolean deletePlTransfo(int playerId) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create()
				.eq(PlayerTransformPo::getPlayerId, playerId)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f096dcaaec6b", playerId), e);
			return false;
		}
	}
}
