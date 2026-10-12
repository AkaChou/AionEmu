package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.persistence.entity.PlayerQuestPo;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.mybatisflex.core.service.IService;

/**
 * 玩家任务状态数据访问层。
 * Player quest-state data access layer.
 */
public interface PlayerQuestDao extends IService<PlayerQuestPo> {

	/**
	 * 从数据库加载玩家的任务状态列表。
	 * Loads the player's quest state list from the database.
	 * @param player 玩家 / player
	 * @return 任务状态列表 / quest state list
	 */
	QuestStateList load(Player player);

	/**
	 * 将玩家任务状态列表持久化到数据库（按 NEW/UPDATE/DELETED 批量处理）。
	 * Persists the player's quest state list (batch insert/update/delete by persistent state).
	 * @param player 玩家 / player
	 */
	void store(Player player);

	/**
	 * 在调用方事务内持久化玩家任务状态列表。
	 * Persists the player's quest state list inside the caller-owned transaction.
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param player 玩家 / player
	 * @throws SQLException 持久化失败 / if persistence fails
	 */
	void store(Connection connection, Player player) throws SQLException;

	/**
	 * 在调用方事务内按任务状态集合持久化。
	 * Persists the supplied quest-state collection inside the caller-owned transaction.
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param playerId 玩家 ID / player id
	 * @param states 任务状态集合 / quest state collection
	 * @throws SQLException 持久化失败 / if persistence fails
	 */
	void store(Connection connection, int playerId, Collection<QuestState> states) throws SQLException;
}
