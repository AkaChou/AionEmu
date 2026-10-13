package com.aionemu.gameserver.network.aion.serverpackets;


import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.HousingBidService;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步房屋拍卖出价列表（分页）的服务端包。
 * Server packet that synchronizes the house auction bid list (paginated) to the client.
 */
@AllArgsConstructor
public class SM_HOUSE_BIDS extends AionServerPacket {
	private final boolean isFirst;
	private final boolean isLast;
	private final HouseBidEntry playerBid;
	private final List<HouseBidEntry> houseBids;

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		int secondsTillAuction = HousingBidService.getInstance().getSecondsTillAuction();
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
			sellData = HousingBidService.getInstance().getHouseBid(sellHouse.getObjectId());
			writeD(sellData.getEntryIndex());
			writeQ(sellData.getBidPrice());
		} else {
			writeD(0);
			writeQ(0);
		}
		writeH(houseBids.size());
		for (HouseBidEntry entry : houseBids) {
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
