package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步飞升任务变身状态的服务端包。
 * Server packet synchronizing ascension-quest morph state to the client.
 *
 * @author wylovech
 */
@AllArgsConstructor
public class SM_ASCENSION_MORPH extends AionServerPacket {

	private final int inascension;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(inascension);// if inascension =0x01 morph.
		writeC(0x00); // new 2.0 Packet --- probably pet info?
	}
}
