package com.aionemu.gameserver.network.chatserver.serverpackets;

import com.aionemu.gameserver.network.chatserver.ChatServerConnection;
import com.aionemu.gameserver.network.chatserver.CsServerPacket;

/**
 * 游戏服通知聊天服玩家上线并请求认证令牌的服务端包。
 * Server packet notifying the chat server of a player login and requesting an auth token.
 */
public class SM_CS_PLAYER_AUTH extends CsServerPacket {
	/**
	 * 玩家对象 ID。
	 * Player object id.
	 */
	private int playerId;

	/**
	 * 玩家账号登录名。
	 * Player account login name.
	 */
	private String playerLogin;

	/**
	 * 玩家角色昵称。
	 * Player character nick name.
	 */
	private String nick;

	/**
	 * 构造玩家认证请求包。
	 * Constructs a player auth request packet.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param playerLogin 账号登录名 / account login name
	 * @param nick 角色昵称 / character nick
	 */
	public SM_CS_PLAYER_AUTH(int playerId, String playerLogin, String nick) {
		super(0x01);
		this.playerId = playerId;
		this.playerLogin = playerLogin;
		this.nick = nick;
	}

	/**
	 * 写出玩家 ID、账号名与昵称。
	 * Writes player id, account name, and nick.
	 *
	 * @param con 目标连接 / target connection
	 */
	@Override
	protected void writeImpl(ChatServerConnection con) {
		writeD(playerId);
		writeS(playerLogin);
		writeS(nick);
	}
}
