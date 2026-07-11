package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 聊天服初始化令牌下发包：向客户端发送聊天鉴权 token。
 * Server packet that delivers the chat-server authentication token to the client.
 *
 * @author ATracer
 */
public class SM_CHAT_INIT extends AionServerPacket {

	private byte[] token;

	/**
	 * 使用聊天鉴权 token 构造。
	 * Constructs with the chat authentication token.
	 *
	 * @param token 聊天服鉴权令牌 / chat-server auth token
	 */
	public SM_CHAT_INIT(byte[] token) {
		this.token = token;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(token.length);
		writeB(token);
	}
}
