package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;

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
}
