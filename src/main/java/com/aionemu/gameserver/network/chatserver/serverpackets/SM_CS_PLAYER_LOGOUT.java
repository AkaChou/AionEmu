package com.aionemu.gameserver.network.chatserver.serverpackets;

import com.aionemu.gameserver.network.chatserver.ChatServerConnection;
import com.aionemu.gameserver.network.chatserver.CsServerPacket;

/**
 * 游戏服通知聊天服玩家下线的服务端包。
 * Server packet notifying the chat server of a player logout.
 */
public class SM_CS_PLAYER_LOGOUT extends CsServerPacket {
	/**
	 * 玩家对象 ID。
	 * Player object id.
	 */
	private int playerId;

	/**
	 * 构造玩家下线通知包。
	 * Constructs a player logout notification packet.
	 *
	 * player object id
	 */
	public SM_CS_PLAYER_LOGOUT(int playerId) {
		super(0x02);
		this.playerId = playerId;
	}

	/**
	 * 写出玩家对象 ID。
	 * Writes the player object id.
	 *
	 * @param con 目标连接 / target connection
	 */
	@Override
	protected void writeImpl(ChatServerConnection con) {
		writeD(playerId);
	}
}
