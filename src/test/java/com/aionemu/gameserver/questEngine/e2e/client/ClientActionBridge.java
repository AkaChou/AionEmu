package com.aionemu.gameserver.questEngine.e2e.client;

/**
 * 把一次客户端请求投递给被测任务引擎的桥；由进程内运行时夹具实现，无头客户端调用。
 * Bridge that hands one client request to the quest engine under test; implemented by the in-process
 * runtime fixture and called by the headless client.
 */
@FunctionalInterface
public interface ClientActionBridge {
	/** 执行一次请求并返回结果。 / Dispatches one request and returns its outcome. */
	ClientActionOutcome dispatch(ClientActionRequest request);
}
