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
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerGrowthDAO.class.getName();
	}

	/**
	 * 加载玩家成长数据。
	 * Loads player growth data.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void load(Player player);

	/**
	 * 新增一条成长记录。
	 * Adds a growth record.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param isFree 免费标记 / free flag
	 * @param rechargeCount 充值次数 / recharge count
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean add(final int playerId, boolean isFree, int rechargeCount);

	/**
	 * 删除成长数据。
	 * Deletes growth data.
	 *
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean delete();

	/**
	 * 持久化玩家成长数据。
	 * Stores player growth data.
	 *
	 * @param player 玩家 / player
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean store(Player player);

	/**
	 * 按对象 ID 设置成长状态。
	 * Sets growth state by object ID.
	 *
	 * @param obj 对象 ID / object id
	 * @param isFree 免费标记 / free flag
	 * @param rechargeCount 充值次数 / recharge count
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean setGrowthByObjId(final int obj, boolean isFree, int rechargeCount);
}
