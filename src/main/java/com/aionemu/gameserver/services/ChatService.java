package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAT_INIT;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 聊天服务器桥接服务，管理玩家聊天鉴权与连接信息。
 * Chat server bridge service managing player chat auth and connection info.
 *
 * @author ATracer
 */
public class ChatService {

	/** 聊天服务器 IP / Chat server IP */
	private static byte[] ip = { 127, 0, 0, 1 };
	/** 聊天服务器端口。 / Chat server port. */
	private static int port = 10241;

	/**
	 * 玩家登出时断开与聊天服务器的连接。
	 * Disconnects the player from the chat server on logout.
	 *
	 * @param player 玩家 / player
	 */
	public static void onPlayerLogout(Player player) {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.chatServer().sendPlayerLogout(player);
	}

	/**
	 * 玩家通过聊天服务器鉴权后下发初始化令牌。
	 * Sends the chat-init token after the player is authenticated by the chat server.
	 *
	 * player id
	 * @param token 鉴权令牌 / auth token
	 */
	public static void playerAuthed(int playerId, byte[] token) {
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player != null) {
			PacketSendUtility.sendPacket(player, new SM_CHAT_INIT(token));
		}
	}

	/**
	 * 获取聊天服务器 IP。
	 * Returns the chat server IP.
	 *
	 * IP bytes
	 */
	public static byte[] getIp() {
		return ip;
	}

	/**
	 * 获取聊天服务器端口。
	 * Returns the chat server port.
	 *
	 * port
	 */
	public static int getPort() {
		return port;
	}

	/**
	 * 设置聊天服务器 IP。
	 * Sets the chat server IP.
	 *
	 * @param _ip IP 字节数组 / IP bytes
	 */
	public static void setIp(byte[] _ip) {
		ip = _ip;
	}

	/**
	 * 设置聊天服务器端口。
	 * Sets the chat server port.
	 *
	 * port
	 */
	public static void setPort(int _port) {
		port = _port;
	}
}
