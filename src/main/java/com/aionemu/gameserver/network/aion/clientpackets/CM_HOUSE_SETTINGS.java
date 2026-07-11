package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.nio.charset.Charset;

import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HousePermissions;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_ACQUIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 修改房屋权限与欢迎设置的客户端包。
 * Client packet for changing house permissions and welcome settings.
 */
public class CM_HOUSE_SETTINGS extends AionClientPacket {
	int doorState;
	int displayOwner;
	String signNotice;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOUSE_SETTINGS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取房屋权限、欢迎语与显示设置。
	 * Reads house permissions, welcome text, and display settings.
	 */
	@Override
	protected void readImpl() {
		doorState = readC();
		displayOwner = readC();
		signNotice = readS();
	}
	/**
	 * 保存房屋门/仓库权限与欢迎设置。
	 * Saves house door/warehouse permissions and welcome settings.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		House house = GameHousingServices.housingService().getPlayerStudio(player.getObjectId());
		if (house == null) {
			int address = GameHousingServices.housingService().getPlayerAddress(player.getObjectId());
			house = GameHousingServices.housingService().getHouseByAddress(address);
		}
		HousePermissions doorPermission = HousePermissions.getPacketDoorState(doorState);
		house.setDoorState(doorPermission);
		house.setNoticeState(HousePermissions.getNoticeState(displayOwner));
		house.setSignNotice(signNotice.getBytes(Charset.forName("UTF-16LE")));
		PacketSendUtility.sendPacket(player,
				new SM_HOUSE_ACQUIRE(player.getObjectId(), house.getAddress().getId(), true));
		HouseController controller = (HouseController) house.getController();
		controller.updateAppearance();

		if (doorPermission == HousePermissions.DOOR_OPENED_ALL) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_ORDER_OPEN_DOOR);
		} else if (doorPermission == HousePermissions.DOOR_OPENED_FRIENDS) {
			house.getController().kickVisitors(player, false, true);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_ORDER_CLOSE_DOOR_WITHOUT_FRIENDS);
		} else if (doorPermission == HousePermissions.DOOR_CLOSED) {
			house.getController().kickVisitors(player, true, true);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_ORDER_CLOSE_DOOR_ALL);
		}
	}
}
