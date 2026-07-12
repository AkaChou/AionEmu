package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 管家（Butler）敬礼/互动动画相关的服务端包。
 * interaction animation.
 *
 * @author Ranastic
 */
public class SM_BUTLER_SALUTE extends AionServerPacket {
	private int playerObjId;
	private int isInside;
	private int unk1;
	private int unk2;
	private int unk3;
	private int unk4;

	/**
	 * 构造管家敬礼包。
	 * Builds a butler salute packet.
	 *
	 * @param unk1 未知字段 1 / unknown field 1
	 * @param unk2 未知字段 2 / unknown field 2
	 * @param unk3 未知字段 3 / unknown field 3
	 * @param unk4 未知字段 4 / unknown field 4
	 * player object id
	 * @param isInside 是否在屋内 / whether inside
	 */
	public SM_BUTLER_SALUTE(int unk1, int unk2, int unk3, int unk4, int playerObjId, int isInside) {
		this.unk1 = unk1;
		this.unk2 = unk2;
		this.unk3 = unk3;
		this.unk4 = unk4;
		this.playerObjId = playerObjId;
		this.isInside = isInside;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(unk1);
		writeC(unk2);
		writeD(unk3);
		writeC(unk4);
		writeD(playerObjId);
		writeC(isInside);
	}
}
