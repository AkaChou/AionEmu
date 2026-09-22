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
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 登录服包处理器工厂：注册 LS 客户端包原型并提供处理器单例。
 * Login-server packet handler factory: registers LS client packet prototypes and exposes the handler singleton.
 *
 * @author Luno
 */
public class LsPacketHandlerFactory {

    /**
     * -- SETTER --
     *  注入 Spring ObjectProvider，供 DI 覆盖静态单例。
     *  Injects Spring ObjectProvider to override the static singleton.
     *
     * @param provider Spring Provider / Spring provider
     */
    @Setter
    private static volatile ObjectProvider<LsPacketHandlerFactory> instanceProvider;
	private final LsPacketHandler handler = new LsPacketHandler();

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static final LsPacketHandlerFactory getInstance() {
		ObjectProvider<LsPacketHandlerFactory> provider = instanceProvider;
		LsPacketHandlerFactory provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("LsPacketHandlerFactory 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
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
}
