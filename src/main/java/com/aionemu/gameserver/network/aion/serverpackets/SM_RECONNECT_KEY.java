package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 对 CM_RECONNECT_AUTH 的应答，下发登录服重连认证密钥。
 * Response to CM_RECONNECT_AUTH providing a reconnection key for LoginServer auth.
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_RECONNECT_KEY extends AionServerPacket {

	/**
	 * 重连密钥，用于认证。 / key for reconnection - will be used for authentication
	 */
	private final int key;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x00);
		writeD(key);
	}
}
