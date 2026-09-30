package com.aionemu.gameserver.questEngine.e2e.client;

import java.util.List;

/**
 * 一次客户端请求的不可变执行结果：谁被处理、是否失败、状态是否变化、失败原因与产生的出站包。
 * Immutable outcome of one client request: whether it was handled, failed, changed state, the failure
 * cause, and the resulting outbound packets.
 * <p>
 * 该类型从无头客户端下沉到这里，使进程内运行时夹具（{@link com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime}）
 * 与协议回路（{@link QuestProtocolLoop}）不再依赖带客户端数据表的无头客户端本体。
 * It was sunk out of the headless client so the in-process runtime fixture and the protocol loop no
 * longer depend on the data-backed headless client itself.
 */
public record ClientActionOutcome(boolean handled, boolean failed, boolean stateChanged,
		RuntimeException failure, List<ServerPacketObservation> packets) {
	public ClientActionOutcome {
		packets = List.copyOf(packets);
	}
}
