package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OWNER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端登记房屋拍卖请求包。
 * Client packet for registering a house for auction.
 */
public class CM_REGISTER_HOUSE extends AionClientPacket {
	long bidKinah;
	long unk1;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_REGISTER_HOUSE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		bidKinah = readQ();
		unk1 = readQ();
	}

	@Override
	protected void runImpl() {
		if (!HousingConfig.ENABLE_HOUSE_AUCTIONS) {
			return;
		}
		Player player = getConnection().getActivePlayer();
		House house = player.getActiveHouse();
		if (house == null || house.getHouseType() == HouseType.STUDIO) {
			return;
		}
		if (house.getStatus() == HouseStatus.SELL_WAIT) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_FAIL_ALREADY_REGISTED);
			return;
		}
		if (!GameHousingServices.housingBidService().isRegisteringAllowed()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_AUCTION_TIMEOUT);
			return;
		}
		if (!house.isFeePaid() && HousingConfig.ENABLE_HOUSE_PAY) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_AUCTION_OVERDUE);
			return;
		}
		long fee = (long) (bidKinah * 0.3f);
		if (player.getInventory().getKinah() < fee) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY);
			return;
		}
		player.getInventory().decreaseKinah(fee);
		GameHousingServices.housingBidService().addHouseToAuction(house, bidKinah);
		PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_AUCTION_MY_HOUSE(house.getAddress().getId()));
		((HouseController) house.getController()).updateAppearance();
		PacketSendUtility.sendPacket(player, new SM_HOUSE_OWNER_INFO(player, house));
	}
}
