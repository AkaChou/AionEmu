package com.aionemu.gameserver.network;

import org.springframework.beans.factory.ObjectProvider;

/**
 * 网络侧全局控制器，维护可见游戏服数量等共享状态。
 * Network-side global controller holding shared state such as visible server count.
 *
 * @author KID
 */
public class NetworkController {

	private static NetworkController instance = new NetworkController();
	private static volatile ObjectProvider<NetworkController> instanceProvider;

	/**
	 * 获取控制器单例（优先 Spring Provider）。
	 * Returns the controller singleton (prefers Spring provider).
	 *
	 * @return 控制器实例 / controller instance
	 */
	public static NetworkController getInstance() {
		ObjectProvider<NetworkController> provider = instanceProvider;
		if (provider == null) {
			return instance;
		}
		return provider.getIfAvailable(() -> instance);
	}

	/**
	 * 注入 Spring ObjectProvider，供 DI 覆盖静态单例。
	 * Injects Spring ObjectProvider to override the static singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<NetworkController> instanceProvider) {
		NetworkController.instanceProvider = instanceProvider;
	}

	/** 可见游戏服数量 / visible game-server count */
	private byte serverCount = 1;

	/**
	 * 获取可见游戏服数量。
	 * Returns visible game-server count.
	 *
	 * server count
	 */
	public final byte getServerCount() {
		return this.serverCount;
	}

	/**
	 * 设置可见游戏服数量。
	 * Sets visible game-server count.
	 *
	 * server count
	 */
	public final void setServerCount(byte count) {
		this.serverCount = count;
	}
}
