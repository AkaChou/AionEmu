package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.HousingBidService;

/**
 * 向客户端同步房屋拍卖出价列表（分页）的服务端包。
 * Server packet that synchronizes the house auction bid list (paginated) to the client.
 */
public class SM_HOUSE_BIDS extends AionServerPacket {
	private boolean isFirst;
	private boolean isLast;
	private HouseBidEntry playerBid;
	private List<HouseBidEntry> houseBids;

	/**
	 * 构造房屋拍卖出价列表包。
	 * Creates a house auction bid list packet.
	 *
	 * @param isFirstPacket 是否为分页首包 / whether this is the first page packet
	 * @param isLastPacket 是否为分页末包 / whether this is the last page packet
	 * @param playerBid 玩家当前出价条目 / player's current bid entry
	 * @param houseBids 本页房屋出价列表 / house bid entries for this page
	 */
	public SM_HOUSE_BIDS(boolean isFirstPacket, boolean isLastPacket, HouseBidEntry playerBid,
			List<HouseBidEntry> houseBids) {
		isFirst = isFirstPacket;
		isLast = isLastPacket;
		this.playerBid = playerBid;
		this.houseBids = houseBids;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		int secondsTillAuction = GameHousingServices.housingBidService().getSecondsTillAuction();
		writeC(isFirst ? 1 : 0);
		writeC(isLast ? 1 : 0);
		if (playerBid == null) {
			writeD(0);
			writeQ(0);
		} else {
			writeD(playerBid.getEntryIndex());
			writeQ(playerBid.getBidPrice());
		}
		List<House> playerHouses = player.getHouses();
		House sellHouse = null;
		for (House house : playerHouses) {
			if (house.getStatus() == HouseStatus.SELL_WAIT) {
				sellHouse = house;
				break;
			}
		}
		HouseBidEntry sellData = null;
		if (sellHouse != null) {
			sellData = GameHousingServices.housingBidService().getHouseBid(sellHouse.getObjectId());
			writeD(sellData.getEntryIndex());
			writeQ(sellData.getBidPrice());
		} else {
			writeD(0);
			writeQ(0);
		}
		writeH(houseBids.size());
		for (int n = 0; n < houseBids.size(); n++) {
			HouseBidEntry entry = houseBids.get(n);
			writeD(entry.getEntryIndex());
			writeD(entry.getLandId());
			writeD(entry.getAddress());
			writeD(entry.getBuildingId());
			if (sellData != null && entry.getEntryIndex() == sellData.getEntryIndex())
				writeD(0);
			else if (HousingBidService.canBidHouse(player, entry.getMapId(), entry.getLandId()))
				writeD(entry.getHouseType().getId());
			else
				writeD(0);
			writeQ(entry.getBidPrice());
			writeQ(entry.getUnk2());
			writeD(entry.getBidCount());
			writeD(secondsTillAuction);
		}
	}
}
