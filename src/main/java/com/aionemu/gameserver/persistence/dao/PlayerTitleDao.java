package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.persistence.entity.PlayerTitlePo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家称号列表数据访问层。
 * Data access layer for player title list persistence.
 * @author xavier
 */
public interface PlayerTitleDao extends IService<PlayerTitlePo> {

	/**
	 * 加载玩家称号列表。
	 * Loads the title list for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 称号列表 / title list
	 */
	TitleList loadTitleList(int playerId);

	/**
	 * 在调用方事务连接上保存玩家一条称号记录，与任务状态同事务提交/回滚。
	 * Stores a title entry for the player on the caller-owned transaction connection.
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param playerId 玩家 object id / player object id
	 * @param titleId 称号 ID / title id
	 * @param remaining 剩余毫秒数，0 表示永久 / remaining ms, 0 for permanent
	 * @throws SQLException 写入失败时抛出 / on write failure
	 */
	void storeInTransaction(Connection connection, int playerId, int titleId, int remaining) throws SQLException;

	/**
	 * 保存玩家一条称号记录。
	 * Stores a title entry for the player.
	 * @param player 玩家 / player
	 * @param entry 称号条目 / title entry
	 */
	void storeTitles(Player player, Title entry);

	/**
	 * 移除玩家一条称号。
	 * Removes a title from the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param titleId 称号 ID / title id
	 */
	void removeTitle(int playerId, int titleId);
}
