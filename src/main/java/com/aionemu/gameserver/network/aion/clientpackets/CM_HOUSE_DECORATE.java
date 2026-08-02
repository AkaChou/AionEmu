package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_EDIT;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 房屋装饰部件更换的客户端包。
 * Client packet for changing house decoration parts.
 */
public class CM_HOUSE_DECORATE extends AionClientPacket {
	int objectId;
	int templateId;
	int lineNr;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOUSE_DECORATE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取装饰部件对象 ID 与行号。
	 * Reads decoration part object id and line number.
	 */
	@Override
	protected void readImpl() {
		objectId = readD();
		templateId = readD();
		lineNr = readH();
	}
	/**
	 * 更换房屋装饰部件并通知客户端。
	 * Swaps house decoration parts and notifies the client.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		House house = player.getHouseRegistry().getOwner();
		PartType partType = PartType.getForLineNr(lineNr);
		int floor = lineNr - partType.getStartLineNr();
		if (objectId == 0) {
			HouseDecoration decor = house.getRegistry().getDefaultPartByType(partType, floor);
			if (decor.isUsed()) {
				return;
			}
			house.getRegistry().setPartInUse(decor, floor);
		} else {
			HouseDecoration decor = house.getRegistry().getCustomPartByObjId(objectId);
			house.getRegistry().setPartInUse(decor, floor);
			sendPacket(new SM_HOUSE_EDIT(4, 2, objectId));
		}
		sendPacket(new SM_HOUSE_EDIT(4, 2, objectId));
		house.getRegistry().setPersistentState(PersistentState.UPDATE_REQUIRED);
		((HouseController) house.getController()).updateAppearance();
		GameEngineServices.questEngine().onHouseItemUseEvent(new QuestEnv(null, player, 0, 0), templateId, objectId);
	}
}
