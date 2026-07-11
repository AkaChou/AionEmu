package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PETITION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.PetitionService;

/**
 * 提交或取消客服请愿（Petition）的客户端包。
 * Client packet for creating or canceling a GM petition.
 *
 * @author zdead
 */
public class CM_PETITION extends AionClientPacket {

	private int action;
	private String title = "";
	private String text = "";
	private String additionalData = "";
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_PETITION(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readH();
		if (action == 2) {
			readD();
		} else {
			String data = readS();
			String[] dataArr = data.split("/", 3);
			title = dataArr[0];
			text = dataArr[1];
			additionalData = dataArr[2];
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		int playerObjId = player.getObjectId();
		if (action == 2) {
			if (GameRuntimeServices.petitionService().hasRegisteredPetition(playerObjId)) {
				int petitionId = GameRuntimeServices.petitionService().getPetition(playerObjId).getPetitionId();
				GameRuntimeServices.petitionService().deletePetition(playerObjId);
				sendPacket(new SM_SYSTEM_MESSAGE(1300552, petitionId));
				sendPacket(new SM_SYSTEM_MESSAGE(1300553, 49));
				return;
			}
		}

		if (!GameRuntimeServices.petitionService().hasRegisteredPetition(playerObjId)) {
			Petition petition = GameRuntimeServices.petitionService().registerPetition(player, action, title, text,
					additionalData);
			sendPacket(new SM_PETITION(petition));
		}
	}
}
