package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家 NPC 势力声望数据访问对象。
 * Player NPC-factions data access object.
 *
 * @author MrPoke
 */
public abstract class PlayerNpcFactionsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerNpcFactionsDAO.class.getName();
	}

	/**
	 * 加载玩家 NPC 势力数据。
	 * Loads player NPC-faction data.
	 *
	 * 玩家 / player
	 */
	public abstract void loadNpcFactions(Player player);

	/**
	 * 存储玩家 NPC 势力数据。
	 * Stores player NPC-faction data.
	 *
	 * 玩家 / player
	 */
	public abstract void storeNpcFactions(Player player);
}
