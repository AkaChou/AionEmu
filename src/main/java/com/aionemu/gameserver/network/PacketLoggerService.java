package com.aionemu.gameserver.network;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.administration.DeveloperConfig;

/**
 * 开发期包名日志服务，按配置输出客户端/服务端包名。
 * Dev-time packet name logger for client/server packets when enabled.
 *
 * @author Ghostfur (Aion-Unique)
 */
@Slf4j
public class PacketLoggerService {

	private static volatile ObjectProvider<PacketLoggerService> instanceProvider;

	/**
	 * 记录客户端包名（CM）。
	 * Logs a client (CM) packet name.
	 *
	 * packet name
	 */
	public void logPacketCM(String name) {
		if (DeveloperConfig.SHOW_PACKETS) {
			log.info(I18n.get("log.41fd9c89f6da", name));
		}
	}

	/**
	 * 记录服务端包名（SM）。
	 * Logs a server (SM) packet name.
	 *
	 * packet name
	 */
	public void logPacketSM(String name) {
		if (DeveloperConfig.SHOW_PACKETS) {
			log.info(I18n.get("log.2f85dc0db82c", name));
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final PacketLoggerService instance = new PacketLoggerService();
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static final PacketLoggerService getInstance() {
		ObjectProvider<PacketLoggerService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<PacketLoggerService> instanceProvider) {
		PacketLoggerService.instanceProvider = instanceProvider;
	}
}
