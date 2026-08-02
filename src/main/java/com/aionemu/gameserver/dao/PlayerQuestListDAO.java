package com.aionemu.gameserver.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 玩家任务列表数据访问抽象层。
 * DAO for player quest state list persistence.
 *
 * @author MrPoke
 * @modified vlog
 */
public abstract class PlayerQuestListDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerQuestListDAO.class.getName();
	}

	/**
	 * 加载玩家的任务状态列表。
	 * Loads the quest state list for the player.
	 *
	 * 玩家 / player
	 * @return 任务状态列表 / quest state list
	 */
	public abstract QuestStateList load(final Player player);

	/**
	 * 保存玩家的任务状态列表。
	 * Stores the quest state list for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void store(final Player player);

	/**
	 * Persists player quest state using the caller-owned transaction.
	 * Required quest mutations must share this connection with inventory and rewards.
	 */
	public abstract void store(Connection connection, Player player) throws SQLException;

	/**
	 * Persists an explicit set of quest states (not the player's live in-memory
	 * list) using the caller-owned transaction. Used by the state port to write
	 * the canonical projection without advancing the live {@link QuestState}
	 * before the transaction commits.
	 */
	public abstract void store(Connection connection, int playerId,
			Collection<QuestState> states) throws SQLException;
}
