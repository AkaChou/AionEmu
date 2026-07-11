package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.PetitionService;

/**
 * 同步客服工单（Petition）状态的服务端包；无工单时写出空结构。
 * Server packet that synchronizes GM petition status; writes an empty structure when none is open.
 */
public class SM_PETITION extends AionServerPacket {
	private Petition petition;

	/**
	 * 构造无工单（清空客户端工单 UI）的包。
	 * Creates a packet that clears the client petition UI.
	 */
	public SM_PETITION() {
		this.petition = null;
	}

	/**
	 * active petition
	 */
	public SM_PETITION(Petition petition) {
		this.petition = petition;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (petition == null) {
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeH(0x00);
			writeC(0x00);
		} else {
			writeC(0x01);
			writeD(100);
			writeH(GameRuntimeServices.petitionService().getWaitingPlayers(con.getActivePlayer().getObjectId()));
			writeS(Integer.toString(petition.getPetitionId()));
			writeH(0x00);
			writeC(50);
			writeC(49);
			writeH(GameRuntimeServices.petitionService().calculateWaitTime(petition.getPlayerObjId()));
			writeD(0x00);
		}
	}
}
