package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 聊天服初始化令牌下发包：向客户端发送聊天鉴权 token。
 * Server packet that delivers the chat-server authentication token to the client.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class SM_CHAT_INIT extends AionServerPacket {

	private final byte[] token;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(token.length);
		writeB(token);
	}
}
