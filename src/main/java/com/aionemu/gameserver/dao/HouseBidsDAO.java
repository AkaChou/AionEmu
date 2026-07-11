package com.aionemu.gameserver.dao;

import java.sql.Timestamp;
import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.house.PlayerHouseBid;

/**
 * 房屋竞拍数据访问对象。
 * House bids data access object.
 *
 * @author Rolandas
 */
public abstract class HouseBidsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return HouseBidsDAO.class.getName();
	}

	/**
	 * 加载全部房屋竞拍。
	 * Loads all house bids.
	 *
	 * bid set
	 */
	public abstract Set<PlayerHouseBid> loadBids();

	/**
	 * 添加一条房屋竞拍。
	 * Adds a house bid.
	 *
	 * player ID
	 * house ID
	 * bid offer
	 * @param time 出价时间 / bid time
	 * whether successful
	 */
	public abstract boolean addBid(int playerId, int houseId, long bidOffer, Timestamp time);

	/**
	 * 修改竞拍出价。
	 * Changes a bid offer.
	 *
	 * player ID
	 * house ID
	 * new bid offer
	 * @param time 出价时间 / bid time
	 */
	public abstract void changeBid(int playerId, int houseId, long newBidOffer, Timestamp time);

	/**
	 * 删除指定房屋的全部竞拍。
	 * Deletes all bids for a house.
	 *
	 * house ID
	 */
	public abstract void deleteHouseBids(int houseId);

}
