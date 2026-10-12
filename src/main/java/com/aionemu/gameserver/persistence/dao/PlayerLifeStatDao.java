package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerLifeStatPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家生命状态（HP/MP 等）数据访问层。
 * Data access layer for player life stats.
 * @author Mr. Poke
 */
public interface PlayerLifeStatDao extends IService<PlayerLifeStatPo> {

	/**
	 * 加载玩家生命状态。
	 * Loads player life stats.
	 * @param player 玩家 / player
	 */
	void loadPlayerLifeStat(Player player);

	/**
	 * 插入玩家生命状态记录。
	 * Inserts a player life-stat record.
	 * @param player 玩家 / player
	 */
	void insertPlayerLifeStat(Player player);

	/**
	 * 更新玩家生命状态记录。
	 * Updates a player life-stat record.
	 * @param player 玩家 / player
	 */
	void updatePlayerLifeStat(Player player);
}
