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

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static final PacketLoggerService getInstance() {
		ObjectProvider<PacketLoggerService> provider = instanceProvider;
		PacketLoggerService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("PacketLoggerService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
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
