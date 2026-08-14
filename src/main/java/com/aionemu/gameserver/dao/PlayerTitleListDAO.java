package com.aionemu.gameserver.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;

/**
 * 玩家称号列表数据访问抽象层。
 * DAO for player title list persistence.
 *
 * @author xavier
 */
public abstract class PlayerTitleListDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerTitleListDAO.class.getName();
	}

	/**
	 * 加载玩家称号列表。
	 * Loads the title list for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @return 称号列表 / title list
	 */
	public abstract TitleList loadTitleList(int playerId);

	/**
	 * 在调用方事务连接上保存玩家一条称号记录，与任务状态同事务提交/回滚。
	 * Stores a title entry for the player on the caller-owned transaction connection.
	 *
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param playerId 玩家 object id / player object id
	 * @param titleId 称号 ID / title id
	 * @param remaining 剩余毫秒数，0 表示永久 / remaining ms, 0 for permanent
	 * @throws SQLException 写入失败时抛出 / on write failure
	 */
	public abstract void storeInTransaction(Connection connection, int playerId, int titleId, int remaining)
			throws SQLException;

	/**
	 * 保存玩家一条称号记录。
	 * Stores a title entry for the player.
	 *
	 * @param player 玩家 / player
	 * @param entry 称号条目 / title entry
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeTitles(Player player, Title entry);

	/**
	 * 移除玩家一条称号。
	 * Removes a title from the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param titleId 称号 ID / title id
	 * @return 是否移除成功 / true if removed
	 */
	public abstract boolean removeTitle(int playerId, int titleId);
}
