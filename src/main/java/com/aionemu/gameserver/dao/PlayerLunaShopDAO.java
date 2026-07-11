package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家月神商店（Luna Shop）数据访问对象。
 * Player Luna Shop data access object.
 *
 * Created by wanke on 13/02/2017.
 */
public abstract class PlayerLunaShopDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerLunaShopDAO.class.getName();
	}

	/**
	 * 加载玩家月神商店数据。
	 * Loads player Luna Shop data.
	 *
	 * 玩家 / player
	 */
	public abstract void load(Player player);

	/**
	 * 新增月神商店免费次数记录。
	 * Adds a Luna Shop free-use record.
	 *
	 * player id
	 * free underpath
	 * free factory
	 * free chest
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean add(final int playerId, boolean freeUnderpath, boolean freeFactory, boolean freeChest);

	/**
	 * 删除月神商店数据。
	 * Deletes Luna Shop data.
	 *
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean delete();

	/**
	 * 持久化玩家月神商店数据。
	 * Stores player Luna Shop data.
	 *
	 * 玩家 / player
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean store(Player player);

	/**
	 * 按对象 ID 设置月神商店免费状态。
	 * Sets Luna Shop free flags by object ID.
	 *
	 * object id
	 * free underpath
	 * free factory
	 * free chest
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean setLunaShopByObjId(final int obj, boolean freeUnderpath, boolean freeFactory,
			boolean freeChest);
}
