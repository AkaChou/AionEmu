package com.aionemu.gameserver.model.house;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.templates.housing.HouseType;

/**
 * 房屋 Bid 条目。
 * House Bid Entry model.
 */

public class HouseBidEntry implements Cloneable {
	private int entryIndex;
	private int landId;
	private int address;
	private int buildingId;
	private HouseType houseType;
	private long bidPrice;
	private final long unk2 = 100000;
	private int bidCount;
	private int mapId;
	private int lastBiddingPlayer;
	private long lastBidTime;

	public HouseBidEntry(House house, int index, long initialBid) {
		entryIndex = index;
		landId = house.getLand().getId();
		address = house.getAddress().getId();
		mapId = house.getAddress().getMapId();
		buildingId = house.getBuilding().getId();
		houseType = house.getHouseType();
		bidPrice = initialBid;
		lastBiddingPlayer = 0;
		lastBidTime = 0;
	}

	private HouseBidEntry() {
	}

	/** 返回条目索引 / Returns the entry index*/
	public int getEntryIndex() {
		return entryIndex;
	}

	/** 设置 entry index / Sets the entry index */
	public void setEntryIndex(int entryIndex) {
		this.entryIndex = entryIndex;
	}

	/** 返回 land id / Returns the land id */
	public int getLandId() {
		return landId;
	}

	/** 返回 address / Returns the address */
	public int getAddress() {
		return address;
	}

	/** 返回 building id / Returns the building id */
	public int getBuildingId() {
		return buildingId;
	}

	/** 设置 building id / Sets the building id */
	public void setBuildingId(int buildingId) {
		this.buildingId = buildingId;
	}

	/** 返回 bid price / Returns the bid price */
	public long getBidPrice() {
		return bidPrice;
	}

	/** 设置 bid price / Sets the bid price */
	public void setBidPrice(long bidPrice) {
		this.bidPrice = bidPrice;
	}

	/** 返回 bid count / Returns the bid count */
	public int getBidCount() {
		return bidCount;
	}

	/** 递增 bid count / Increment Bid Count */
	public void incrementBidCount() {
		this.bidCount++;
	}

	/** 返回 unk 2 / Returns the unk 2 */
	public final long getUnk2() {
		return unk2;
	}

	/** 获取房屋类型。 / Returns the house type. */
	public HouseType getHouseType() {
		return houseType;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapId;
	}

	/** 返回 last bidding player / Returns the last bidding player */
	public int getLastBiddingPlayer() {
		return lastBiddingPlayer;
	}

	/** 设置最后出价玩家 / Sets the last bidding player */
	public void setLastBiddingPlayer(int lastBiddingPlayer) {
		this.lastBiddingPlayer = lastBiddingPlayer;
	}

	/** 返回 refund kinah / Returns the refund kinah */
	public long getRefundKinah() {
		return (long) (bidPrice * (float) HousingConfig.BID_REFUND_PERCENT);
	}

	/** 返回 last bid time / Returns the last bid time */
	public long getLastBidTime() {
		return lastBidTime;
	}

	/** 设置 last bid time / Sets the last bid time */
	public void setLastBidTime(long lastBidTime) {
		this.lastBidTime = lastBidTime;
	}

	/** 克隆 / Clone. */
	public Object Clone() {
		HouseBidEntry cloned = new HouseBidEntry();
		cloned.address = this.address;
		cloned.bidCount = this.bidCount;
		cloned.bidPrice = this.bidPrice;
		cloned.buildingId = this.buildingId;
		cloned.entryIndex = this.entryIndex;
		cloned.houseType = this.houseType;
		cloned.landId = this.landId;
		cloned.mapId = this.mapId;
		cloned.lastBiddingPlayer = this.lastBiddingPlayer;
		return cloned;
	}
}
