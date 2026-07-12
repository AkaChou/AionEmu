package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BUTLER_SALUTE;

/**
 * 管家（Butler）敬礼/互动相关的客户端包。
 * housing butler interaction.
 *
 * @author Ranastic
 */
public class CM_BUTLER_SALUTE extends AionClientPacket {
	private int playerObjId;
	private int isInside;
	private int unk1;
	private int unk2;
	private int unk3;
	private int unk4;

	public CM_BUTLER_SALUTE(int opcode, State state, State... states) {
		super(opcode, state, states);
	}

	@Override
	protected void readImpl() {
		unk1 = readD();
		unk2 = readC();
		unk3 = readD();
		unk4 = readC();
		playerObjId = readD();
		isInside = readC();
	}

	@Override
	protected void runImpl() {
		sendPacket(new SM_BUTLER_SALUTE(unk1, unk2, unk3, unk4, playerObjId, isInside));
	}
}
