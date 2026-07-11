package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 对 CM_RECONNECT_AUTH 的应答，下发登录服重连认证密钥。
 * Response to CM_RECONNECT_AUTH providing a reconnection key for LoginServer auth.
 *
 * @author -Nemesiss-
 */
public class SM_RECONNECT_KEY extends AionServerPacket {

	/**
	 * 重连密钥，用于认证。 / key for reconnection - will be used for authentication
	 */
	private final int key;

	/**
	 * 使用给定参数构造 SM_RECONNECT_KEY 包。
	 * Constructs new <tt>SM_RECONNECT_KEY</tt> packet
	 *
	 * @param key 重连密钥 / reconnect key
	 */
	public SM_RECONNECT_KEY(int key) {
		this.key = key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x00);
		writeD(key);
	}
}
