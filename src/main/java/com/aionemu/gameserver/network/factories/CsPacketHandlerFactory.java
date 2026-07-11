package com.aionemu.gameserver.network.factories;

import com.aionemu.gameserver.network.chatserver.ChatServerConnection.State;
import com.aionemu.gameserver.network.chatserver.CsClientPacket;
import com.aionemu.gameserver.network.chatserver.CsPacketHandler;
import com.aionemu.gameserver.network.chatserver.clientpackets.CM_CS_AUTH_RESPONSE;
import com.aionemu.gameserver.network.chatserver.clientpackets.CM_CS_PLAYER_AUTH_RESPONSE;

/**
 * 聊天服包处理器工厂：注册 CS 客户端包原型。
 * Chat-server packet handler factory: registers CS client packet prototypes.
 *
 * @author ATracer
 */
public class CsPacketHandlerFactory {

	private CsPacketHandler handler = new CsPacketHandler();

	/**
	 * 注册聊天服包处理器。
	 * Registers chat-server packet handlers.
	 */
	public CsPacketHandlerFactory() {
		addPacket(new CM_CS_AUTH_RESPONSE(0x00), State.CONNECTED);
		addPacket(new CM_CS_PLAYER_AUTH_RESPONSE(0x01), State.AUTHED);
	}

	/**
	 * 向处理器注册包原型及合法状态。
	 * Registers a packet prototype with valid states.
	 *
	 * packet prototype
	 * @param states 合法连接状态 / valid connection states
	 */
	private void addPacket(CsClientPacket prototype, State... states) {
		handler.addPacketPrototype(prototype, states);
	}

	/**
	 * 获取已注册的包处理器。
	 * Returns the registered packet handler.
	 *
	 * packet handler
	 */
	public CsPacketHandler getPacketHandler() {
		return handler;
	}
}
