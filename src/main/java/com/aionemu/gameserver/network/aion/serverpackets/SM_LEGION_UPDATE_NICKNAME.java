package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步军团成员昵称更新的服务端包。
 * Server packet that synchronizes a legion member nickname update to the client.
 * @author Simple
 */
@AllArgsConstructor
public class SM_LEGION_UPDATE_NICKNAME extends AionServerPacket {

	private final int playerObjId;
	private final String newNickname;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeS(newNickname);
	}
}
