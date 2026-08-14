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
	 *
	 * @param progressLatch 进度门闩 / progress latch
	 */
	void load(CountDownLatch progressLatch);

	/**
	 * 清理引擎资源。
	 * Cleanup resources for engine
	 */
	void shutdown();
}
