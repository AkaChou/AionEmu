package com.aionemu.gameserver.persistence.dao;

import java.sql.Timestamp;
import java.util.Set;

import com.aionemu.gameserver.model.house.PlayerHouseBid;
import com.aionemu.gameserver.persistence.entity.HouseBidPo;
import com.mybatisflex.core.service.IService;

/**
 * 房屋竞拍数据访问层：负责存储与加载 house_bids 数据。
 * House bids data access layer: stores/loads house_bids data.
 */
public interface HouseBidDao extends IService<HouseBidPo> {

	/**
	 * 加载全部房屋竞拍。
	 * Loads all house bids.
	 *
	 * @return 竞标集合 / bid set
	 */
	Set<PlayerHouseBid> loadBids();

	/**
	 * 添加一条房屋竞拍。
	 * Adds a house bid.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @param houseId  房屋 ID / house ID
	 * @param bidOffer 竞标出价 / bid offer
	 * @param time     出价时间 / bid time
	 * @return 是否成功 / whether successful
	 */
	boolean addBid(int playerId, int houseId, long bidOffer, Timestamp time);

	/**
	 * 修改竞拍出价。
	 * Changes a bid offer.
	 *
	 * @param playerId    玩家 ID / player ID
	 * @param houseId     房屋 ID / house ID
	 * @param newBidOffer 新竞标出价 / new bid offer
	 * @param time        出价时间 / bid time
	 */
	void changeBid(int playerId, int houseId, long newBidOffer, Timestamp time);

	/**
	 * 删除指定房屋的全部竞拍。
	 * Deletes all bids for a house.
	 *
	 * @param houseId 房屋 ID / house ID
	 */
	void deleteHouseBids(int houseId);
}
