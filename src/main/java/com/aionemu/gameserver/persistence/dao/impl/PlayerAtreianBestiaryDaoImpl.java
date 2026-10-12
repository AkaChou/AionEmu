package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABEntry;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.persistence.entity.PlayerAtreianBestiaryPo;
import com.aionemu.gameserver.persistence.mapper.PlayerAtreianBestiaryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家阿特雷亚图鉴数据访问实现。
 * Data access implementation for player Atreian Bestiary persistence.
 * @author Ranastic
 */
@Slf4j
@Service
@Lazy
public class PlayerAtreianBestiaryDaoImpl extends ServiceImpl<PlayerAtreianBestiaryMapper, PlayerAtreianBestiaryPo>
	implements PlayerAtreianBestiaryDao {

	@Override
	public PlayerABList load(Player player) {
		List<PlayerABEntry> entries = new ArrayList<>();

		try {
			List<PlayerAtreianBestiaryPo> rows = mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerAtreianBestiaryPo::getPlayerId, player.getObjectId()));
			for (PlayerAtreianBestiaryPo row : rows) {
				entries.add(new PlayerABEntry(row.getId(), row.getKillCount(), row.getLevel(), row.getClaimReward(),
					PersistentState.UPDATED));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e26023dd4433", player.getObjectId()), e);
		}

		return new PlayerABList(entries);
	}

	@Override
	public boolean store(int playerObjId, int id, int killCount, int level, int levelUpable) {
		try {
			mapper.upsert(playerObjId, id, killCount, level, levelUpable);
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.072ceacb9d9e", playerObjId), e);
			return false;
		}
	}

	@Override
	public boolean delete(int playerObjId, int id) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.eq(PlayerAtreianBestiaryPo::getPlayerId, playerObjId)
				.eq(PlayerAtreianBestiaryPo::getId, id));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e05ecfff5330", playerObjId), e);
			return false;
		}
	}

	@Override
	public int getKillCountById(int playerObjId, int id) {
		try {
			PlayerAtreianBestiaryPo po = mapper.selectOneByQuery(byPlayerAndId(playerObjId, id));
			return po == null || po.getKillCount() == null ? 0 : po.getKillCount();
		} catch (DataAccessException e) {
			log.debug("No kill count found for player {}, id {}", playerObjId, id);
			return 0;
		}
	}

	@Override
	public int getLevelById(int playerObjId, int id) {
		try {
			PlayerAtreianBestiaryPo po = mapper.selectOneByQuery(byPlayerAndId(playerObjId, id));
			return po == null || po.getLevel() == null ? 0 : po.getLevel();
		} catch (DataAccessException e) {
			log.debug("No level found for player {}, id {}", playerObjId, id);
			return 0;
		}
	}

	@Override
	public int getClaimRewardById(int playerObjId, int id) {
		try {
			PlayerAtreianBestiaryPo po = mapper.selectOneByQuery(byPlayerAndId(playerObjId, id));
			return po == null || po.getClaimReward() == null ? 0 : po.getClaimReward();
		} catch (DataAccessException e) {
			log.debug("No claim reward found for player {}, id {}", playerObjId, id);
			return 0;
		}
	}

	private static QueryWrapper byPlayerAndId(int playerObjId, int id) {
		return QueryWrapper.create()
			.eq(PlayerAtreianBestiaryPo::getPlayerId, playerObjId)
			.eq(PlayerAtreianBestiaryPo::getId, id);
	}
}
