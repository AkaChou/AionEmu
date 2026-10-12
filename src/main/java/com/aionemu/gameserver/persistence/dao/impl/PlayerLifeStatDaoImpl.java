package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.persistence.dao.PlayerLifeStatDao;
import com.aionemu.gameserver.persistence.entity.PlayerLifeStatPo;
import com.aionemu.gameserver.persistence.mapper.PlayerLifeStatMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家生命状态数据访问实现。
 * Data access implementation for player life stats.
 * @author Mr. Poke
 */
@Slf4j
@Service
@Lazy
public class PlayerLifeStatDaoImpl extends ServiceImpl<PlayerLifeStatMapper, PlayerLifeStatPo>
	implements PlayerLifeStatDao {

	/**
	 * 加载玩家生命状态。
	 * Loads player life stats.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadPlayerLifeStat(Player player) {
		try {
			PlayerLifeStatPo po = mapper.selectOneById(player.getObjectId());
			if (po == null) {
				insertPlayerLifeStat(player);
				return;
			}
			PlayerLifeStats lifeStats = player.getLifeStats();
			lifeStats.setCurrentHp(po.getHp());
			lifeStats.setCurrentMp(po.getMp());
			lifeStats.setCurrentFp(po.getFp());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.df17e20d3575", player.getObjectId()), e);
			insertPlayerLifeStat(player);
		}
	}

	/**
	 * 插入玩家生命状态。
	 * Inserts player life stats.
	 * @param player 玩家 / player
	 */
	@Override
	public void insertPlayerLifeStat(Player player) {
		try {
			PlayerLifeStats lifeStats = player.getLifeStats();

			mapper.upsertLifeStat(player.getObjectId(), lifeStats.getCurrentHp(), lifeStats.getCurrentMp(),
				lifeStats.getCurrentFp());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.57de73400d87", player.getObjectId()), e);
		}
	}

	/**
	 * 更新玩家生命状态。
	 * Updates player life stats.
	 * @param player 玩家 / player
	 */
	@Override
	public void updatePlayerLifeStat(Player player) {
		PlayerLifeStats lifeStats = player.getLifeStats();
		int hp = lifeStats.getCurrentHp();
		int mp = lifeStats.getCurrentMp();
		int fp = lifeStats.getCurrentFp();

		if (hp < 0)
			hp = 0;
		if (mp < 0)
			mp = 0;
		if (fp < 0)
			fp = 0;

		try {
			boolean updated = UpdateChain.of(PlayerLifeStatPo.class)
				.set(PlayerLifeStatPo::getHp, hp)
				.set(PlayerLifeStatPo::getMp, mp)
				.set(PlayerLifeStatPo::getFp, fp)
				.where(PlayerLifeStatPo::getPlayerId).eq(player.getObjectId())
				.update();

			if (!updated) {
				insertPlayerLifeStat(player);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.78b2c8b3d4cc", player.getObjectId()), e);
		}
	}
}
