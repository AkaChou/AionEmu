package com.aionemu.gameserver.dao;

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
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerTitleListDAO.class.getName();
	}

	/**
	 * 加载玩家称号列表。
	 * Loads the title list for the player.
	 *
	 * player object id
	 * title list
	 */
	public abstract TitleList loadTitleList(int playerId);

	/**
	 * 保存玩家一条称号记录。
	 * Stores a title entry for the player.
	 *
	 * 玩家 / player
	 * @param entry 称号条目 / title entry
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeTitles(Player player, Title entry);

	/**
	 * 移除玩家一条称号。
	 * Removes a title from the player.
	 *
	 * player object id
	 * title id
	 * @return 是否移除成功 / true if removed
	 */
	public abstract boolean removeTitle(int playerId, int titleId);
}
