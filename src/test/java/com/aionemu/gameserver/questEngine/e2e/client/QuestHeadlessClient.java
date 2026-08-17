package com.aionemu.gameserver.questEngine.e2e.client;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.List;

/**
 * 仅沿 Aion 5.8 客户端可见动作驱动任务的无头客户端，不自行推断下一页或服务器状态。
 * Headless client that drives quests only through Aion 5.8 client-visible actions and never infers the next page or
 * server state.
 */
public final class QuestHeadlessClient {
	private final VirtualClientState state;
	private final ClientResourceOracle oracle;
	private final ActionBridge bridge;
	private final QuestTrace trace;

	/** 客户端动作到任务运行时的同步桥。 / Synchronous bridge from client actions to the quest runtime. */
	@FunctionalInterface
	public interface ActionBridge {
		DispatchOutcome dispatch(ClientActionRequest request);
	}

	/** 一次客户端请求的不可变执行结果。 / Immutable outcome of one client request. */
	public record DispatchOutcome(boolean handled, boolean failed, boolean stateChanged,
			RuntimeException failure, List<ServerPacketObservation> packets) {
		public DispatchOutcome {
			packets = List.copyOf(packets);
		}
	}

	public QuestHeadlessClient(VirtualClientState state, ClientResourceOracle oracle,
			ActionBridge bridge, QuestTrace trace) {
		this.state = java.util.Objects.requireNonNull(state, "state");
		this.oracle = java.util.Objects.requireNonNull(oracle, "oracle");
		this.bridge = java.util.Objects.requireNonNull(bridge, "bridge");
		this.trace = java.util.Objects.requireNonNull(trace, "trace");
	}

	/** 点击指定 NPC 的任务 action。 / Clicks a quest action on the specified NPC. */
	public DispatchOutcome clickNpc(int npcId, int objectId, int questId, int dialogId) {
		if (questId != state.questId()) {
			throw new IllegalArgumentException("questId does not belong to this client state");
		}
		state.interactWith(npcId, objectId);
		return dispatch(ClientActionRequest.dialog(questId, npcId, objectId, dialogId));
	}

	/** 点击当前客户端页面上实际可见的 action。 / Clicks an action actually visible on the current client page. */
	public DispatchOutcome clickPageAction(int actionId) {
		boolean visible = oracle.visibleActions(state.questId(), state.currentPage()).stream()
			.anyMatch(action -> action.actionId() == actionId && oracle.actionExists(action.actionId()));
		if (!visible) {
			throw new IllegalArgumentException("action is not visible on the current Aion 5.8 client page");
		}
		if (state.currentNpcId() <= 0 || state.currentObjectId() <= 0) {
			throw new IllegalStateException("page action has no authoritative interaction object");
		}
		return dispatch(ClientActionRequest.dialog(state.questId(), state.currentNpcId(),
			state.currentObjectId(), actionId));
	}

	/** 完成任务交互物的使用动作。 / Completes a quest interaction-object use action. */
	public DispatchOutcome useObject(int npcId, int objectId) {
		state.interactWith(npcId, objectId);
		return dispatch(ClientActionRequest.useObject(state.questId(), npcId, objectId));
	}

	/** 使用背包中的任务物品。 / Uses a quest item from inventory. */
	public DispatchOutcome useItem(int itemId, int itemObjectId) {
		return dispatch(ClientActionRequest.useItem(state.questId(), itemId, itemObjectId));
	}

	/** 注入确定性的世界事件。 / Emits a deterministic world event. */
	public DispatchOutcome emitWorldEvent(QuestEvent event) {
		return dispatch(ClientActionRequest.world(state.questId(), event));
	}

	private DispatchOutcome dispatch(ClientActionRequest request) {
		trace.add("CLIENT", request.kind() + ":" + request.event());
		DispatchOutcome outcome = bridge.dispatch(request);
		for (ServerPacketObservation packet : outcome.packets()) {
			state.observe(packet);
		}
		return outcome;
	}
}
