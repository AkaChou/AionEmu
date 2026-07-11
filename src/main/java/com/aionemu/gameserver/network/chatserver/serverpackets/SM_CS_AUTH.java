package com.aionemu.gameserver.network.chatserver.serverpackets;

import com.aionemu.gameserver.configs.network.IPConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.chatserver.ChatServerConnection;
import com.aionemu.gameserver.network.chatserver.CsServerPacket;

/**
 * 游戏服向聊天服发起认证的服务端包。
 * Server packet used by the game server to authenticate with the chat server.
 */
public class SM_CS_AUTH extends CsServerPacket {
	/**
	 * 构造认证包（操作码 0x00）。
	 * Constructs the auth packet (opcode 0x00).
	 */
	public SM_CS_AUTH() {
		super(0x00);
	}

	/**
	 * 写出游戏服 ID、默认地址与聊天密码。
	 * Writes game-server id, default address, and chat password.
	 *
	 * @param con 目标连接 / target connection
	 */
	@Override
	protected void writeImpl(ChatServerConnection con) {
		writeC(NetworkConfig.GAMESERVER_ID);
		writeC(IPConfig.getDefaultAddress().length);
		writeB(IPConfig.getDefaultAddress());
		writeS(NetworkConfig.CHAT_PASSWORD);
	}
}
