package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端下发连接加密密钥的服务端包。
 * Server packet that delivers the connection encryption key to the client.
 *
 * @author -Nemesiss-
 */
public class SM_KEY extends AionServerPacket {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(con.enableCryptKey());
	}
}
