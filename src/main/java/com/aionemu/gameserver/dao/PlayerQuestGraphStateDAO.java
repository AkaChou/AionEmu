package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 定义玩家任务图状态的加载与事务保存合同。
 * Defines the load and transactional-store contract for player quest graph state.
 */
public abstract class PlayerQuestGraphStateDAO implements DAO {

	/**
	 * 返回所有实现共享的 DAO 标识。
	 * Returns the DAO identifier shared by all implementations.
	 */
	@Override
	public String getClassName() {
		return PlayerQuestGraphStateDAO.class.getName();
	}

	/**
	 * 加载玩家全部任务图状态；损坏数据必须抛错。
	 * Loads all quest graph states for a player; corrupt data must fail explicitly.
	 */
	public abstract PlayerQuestGraphStateList load(Player player);

	/**
	 * 在一个事务中保存玩家任务图状态和删除账本。
	 * Stores player quest graph states and the deletion ledger in one transaction.
	 */
	public abstract void store(Player player);

	/**
	 * 仅在数据库 revision 与期望值一致时写入下一状态；不存在时 expectedRevision 为 null。
	 * Writes the next state only when the database revision matches; expectedRevision is null for insertion.
	 */
	public abstract boolean compareAndSet(int playerId, Long expectedRevision, PlayerQuestGraphState state);
}
