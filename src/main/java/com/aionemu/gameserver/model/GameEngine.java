package com.aionemu.gameserver.model;

import java.util.concurrent.CountDownLatch;

/**
 * 游戏引擎接口。
 * Game Engine interface.
 *
 * @author ATracer
 */
public interface GameEngine {

	/**
	 * 为引擎加载资源。
	 * Load resources for engine
	 */
	void load(CountDownLatch progressLatch);

	/**
	 * 清理引擎资源。
	 * Cleanup resources for engine
	 */
	void shutdown();
}
