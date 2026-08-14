package com.aionemu.gameserver.network.factories;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection.State;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.network.loginserver.LsPacketHandler;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_ACCOUNT_RECONNECT_KEY;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_ACOUNT_AUTH_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_BAN_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_GS_AUTH_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_GS_CHARACTER_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_LS_CONTROL_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_LS_PING;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_MACBAN_LIST;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_PREMIUM_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_PTRANSFER_RESPONSE;
import com.aionemu.gameserver.network.loginserver.clientpackets.CM_REQUEST_KICK_ACCOUNT;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 登录服包处理器工厂：注册 LS 客户端包原型并提供处理器单例。
 * Login-server packet handler factory: registers LS client packet prototypes and exposes the handler singleton.
 *
 * @author Luno
 */
public class LsPacketHandlerFactory {

	private static volatile ObjectProvider<LsPacketHandlerFactory> instanceProvider;
	private LsPacketHandler handler = new LsPacketHandler();

	/**
	 * 获取工厂单例（优先 Spring Provider）。
	 * Returns the factory singleton (prefers Spring provider).
	 *
	 * @return 工厂实例 / factory instance
	 */
	public static final LsPacketHandlerFactory getInstance() {
		ObjectProvider<LsPacketHandlerFactory> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 *
	 * @param provider Spring Provider / Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<LsPacketHandlerFactory> provider) {
		instanceProvider = provider;
	}

	/**
	 * 注册登录服包处理器。
	 * Registers login-server packet handlers.
	 */
	public LsPacketHandlerFactory() {
		addPacket(new CM_ACCOUNT_RECONNECT_KEY(0x03), State.AUTHED);
		addPacket(new CM_ACOUNT_AUTH_RESPONSE(0x01), State.AUTHED);
		addPacket(new CM_GS_AUTH_RESPONSE(0x00), State.CONNECTED);
		addPacket(new CM_REQUEST_KICK_ACCOUNT(0x02), State.AUTHED);
		addPacket(new CM_LS_CONTROL_RESPONSE(0x04), State.AUTHED);
		addPacket(new CM_BAN_RESPONSE(0x05), State.AUTHED);
		addPacket(new CM_GS_CHARACTER_RESPONSE(0x08), State.AUTHED);
		addPacket(new CM_MACBAN_LIST(9), State.AUTHED);
		addPacket(new CM_PREMIUM_RESPONSE(10), State.AUTHED);
		addPacket(new CM_LS_PING(11), State.AUTHED);
		addPacket(new CM_PTRANSFER_RESPONSE(12), State.AUTHED);
	}

	/**
	 * 向处理器注册包原型及合法状态。
	 * Registers a packet prototype with valid states.
	 *
	 * @param prototype 包原型 / packet prototype
	 * @param states 合法连接状态 / valid connection states
	 */
	private void addPacket(LsClientPacket prototype, State... states) {
		handler.addPacketPrototype(prototype, states);
	}

	/**
	 * 获取已注册的包处理器。
	 * Returns the registered packet handler.
	 *
	 * @return 包处理器 / packet handler
	 */
	public LsPacketHandler getPacketHandler() {
		return handler;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final LsPacketHandlerFactory instance = new LsPacketHandlerFactory();
	}
}
