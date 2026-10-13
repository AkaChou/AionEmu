package com.aionemu.gameserver.network;

import org.springframework.beans.factory.ObjectProvider;

/**
 * 网络侧全局控制器，维护可见游戏服数量等共享状态。
 * Network-side global controller holding shared state such as visible server count.
 * @author KID
 */
public class NetworkController {

	private static volatile ObjectProvider<NetworkController> instanceProvider;
	private static volatile NetworkController resolvedInstance;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static NetworkController getInstance() {
		NetworkController resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<NetworkController> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("NetworkController 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 * @param instanceProvider Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<NetworkController> instanceProvider) {
		NetworkController.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/** 可见游戏服数量 / visible game-server count */
	private byte serverCount = 1;

	/**
	 * 获取可见游戏服数量。
	 * Returns visible game-server count.
	 * server count
	 */
	public final byte getServerCount() {
		return this.serverCount;
	}

	/**
	 * 设置可见游戏服数量。
	 * Sets visible game-server count.
	 * @param count server count
	 */
	public final void setServerCount(byte count) {
		this.serverCount = count;
	}
}
