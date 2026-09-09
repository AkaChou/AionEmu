package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端下发安全令牌字符串。
 * Server packet delivering a security token string to the client.
 *
 * @author xXMashUpXx
 */
@AllArgsConstructor
public class SM_SECURITY_TOKEN extends AionServerPacket {

	private final String token;

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(token, 64);
	}
}
