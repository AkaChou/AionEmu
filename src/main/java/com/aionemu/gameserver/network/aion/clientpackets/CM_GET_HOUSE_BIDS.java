package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.HouseBidEntry;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_BIDS;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.collections.ListSplitter;

/**
 * 请求房屋拍卖出价列表的客户端包。
 * Client packet requesting the house auction bid list.
 */
public class CM_GET_HOUSE_BIDS extends AionClientPacket {
	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GET_HOUSE_BIDS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		HouseBidEntry playerBid = GameHousingServices.housingBidService().getLastPlayerBid(player.getObjectId());
		List<HouseBidEntry> houseBids = GameHousingServices.housingBidService().getHouseBidEntries(player.getRace());
		ListSplitter<HouseBidEntry> splitter = new ListSplitter<HouseBidEntry>(houseBids, 181);
		while (!splitter.isLast()) {
			List<HouseBidEntry> packetBids = splitter.getNext();
			HouseBidEntry playerData = splitter.isLast() ? playerBid : null;
			PacketSendUtility.sendPacket(player,
					new SM_HOUSE_BIDS(splitter.isFirst(), splitter.isLast(), playerData, packetBids));
		}
	}
}