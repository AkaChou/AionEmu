package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端下发安全令牌字符串。
 * Server packet delivering a security token string to the client.
 *
 * @author xXMashUpXx
 */
public class SM_SECURITY_TOKEN extends AionServerPacket {

	private String token;

	/**
	 * 使用给定参数构造 SM_SECURITY_TOKEN 包。
	 * Creates a SM_SECURITY_TOKEN packet with the given parameters.
	 *
	 * @param token 安全令牌 / security token
	 */
	public SM_SECURITY_TOKEN(String token) {
		this.token = token;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(token, 64);
	}
}
