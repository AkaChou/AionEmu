package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家成长/补给数据访问对象。
 * Player growth data access object.
 *
 * Created by wanke on 26/02/2017.
 */
public abstract class PlayerGrowthDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerGrowthDAO.class.getName();
	}

	/**
	 * 加载玩家成长数据。
	 * Loads player growth data.
	 *
	 * 玩家 / player
	 */
	public abstract void load(Player player);

	/**
	 * 新增一条成长记录。
	 * Adds a growth record.
	 *
	 * player id
	 * free flag
	 * recharge count
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean add(final int playerId, boolean isFree, int rechargeCount);

	/**
	 * 删除成长数据。
	 * Deletes growth data.
	 *
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean delete();

	/**
	 * 持久化玩家成长数据。
	 * Stores player growth data.
	 *
	 * 玩家 / player
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean store(Player player);

	/**
	 * 按对象 ID 设置成长状态。
	 * Sets growth state by object ID.
	 *
	 * object id
	 * free flag
	 * recharge count
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean setGrowthByObjId(final int obj, boolean isFree, int rechargeCount);
}
