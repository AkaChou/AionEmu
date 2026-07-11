package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步飞升任务变身状态的服务端包。
 * Server packet synchronizing ascension-quest morph state to the client.
 *
 * @author wylovech
 */
public class SM_ASCENSION_MORPH extends AionServerPacket {

	private int inascension;

	/**
	 * 构造飞升变身状态包。
	 * Creates an ascension morph state packet.
	 *
	 * @param inascension 是否处于飞升变身（1=变身） / whether currently morphed for ascension (1 = morph)
	 */
	public SM_ASCENSION_MORPH(int inascension) {
		this.inascension = inascension;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(inascension);// if inascension =0x01 morph.
		writeC(0x00); // new 2.0 Packet --- probably pet info?
	}
}
