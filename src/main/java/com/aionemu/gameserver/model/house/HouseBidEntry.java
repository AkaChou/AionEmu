package com.aionemu.gameserver.model.house;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * 房屋 Bid 条目。
 * House Bid Entry model.
 */

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HouseBidEntry implements Cloneable {
	/** 返回条目索引 / Returns the entry index*/
	@Getter
	@Setter
	private int entryIndex;
	/** 返回 land id / Returns the land id */
	@Getter
	private int landId;
	/** 返回 address / Returns the address */
	@Getter
	private int address;
	/** 返回 building id / Returns the building id */
	@Getter
	@Setter
	private int buildingId;
	/** 获取房屋类型。 / Returns the house type. */
	@Getter
	private HouseType houseType;
	/** 返回 bid price / Returns the bid price */
	@Getter
	@Setter
	private long bidPrice;
	private final long unk2 = 100000;
	/** 返回 bid count / Returns the bid count */
	@Getter
	private int bidCount;
	/** 返回映射 ID / Returns the map id */
	@Getter
	private int mapId;
	/** 返回 last bidding player / Returns the last bidding player */
	@Getter
	@Setter
	private int lastBiddingPlayer;
	/** 返回 last bid time / Returns the last bid time */
	@Getter
	@Setter
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

	/** 递增 bid count / Increment Bid Count */
	public void incrementBidCount() {
		this.bidCount++;
	}

	/** 返回 unk 2 / Returns the unk 2 */
	public final long getUnk2() {
		return unk2;
	}

	/** 返回 refund kinah / Returns the refund kinah */
	public long getRefundKinah() {
		return (long) (bidPrice * HousingConfig.BID_REFUND_PERCENT);
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
