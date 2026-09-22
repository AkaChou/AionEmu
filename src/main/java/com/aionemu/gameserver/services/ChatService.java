package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAT_INIT;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.Getter;
import lombok.Setter;

/**
 * 聊天服务器桥接服务，管理玩家聊天鉴权与连接信息。
 * Chat server bridge service managing player chat auth and connection info.
 * @author ATracer
 */
public class ChatService {

	/** 聊天服务器 IP / Chat server IP
	 * -- GETTER --
	 *  获取聊天服务器 IP。
	 *  Returns the chat server IP.
	 *  IP bytes
	 * -- SETTER --
	 *  设置聊天服务器 IP。
	 *  Sets the chat server IP.
	 */
	@Setter
	@Getter
	private static byte[] ip = { 127, 0, 0, 1 };
	/** 聊天服务器端口。 / Chat server port.
	 * -- GETTER --
	 *  获取聊天服务器端口。
	 *  Returns the chat server port.
	 *  port
	 * -- SETTER --
	 *  设置聊天服务器端口。
	 *  Sets the chat server port.
	 *  port
	 */
	@Setter
	@Getter
	private static int port = 10241;

	/**
	 * 玩家登出时断开与聊天服务器的连接。
	 * Disconnects the player from the chat server on logout.
	 * @param player 玩家 / player
	 */
	public static void onPlayerLogout(Player player) {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer().sendPlayerLogout(player);
	}

	/**
	 * 玩家通过聊天服务器鉴权后下发初始化令牌。
	 * Sends the chat-init token after the player is authenticated by the chat server.
	 * player id
	 * @param token 鉴权令牌 / auth token
	 */
	public static void playerAuthed(int playerId, byte[] token) {
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player != null) {
			PacketSendUtility.sendPacket(player, new SM_CHAT_INIT(token));
		}
	}

}
