package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.InteractionAction;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.InteractionEligibilityEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ReadOnlyContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 将服务端对象快照映射为 fail-closed 的只读交互资格查询。
 * Maps server object snapshots to fail-closed, read-only interaction eligibility queries.
 */
public final class QuestGraphInteractionEligibilityBridge {

	private final QuestGraphRouter router;

	/** 创建绑定已编译图路由器的 typed query bridge。 / Creates a typed query bridge bound to a compiled-graph router. */
	public QuestGraphInteractionEligibilityBridge(QuestGraphRouter router) {
		this.router = Objects.requireNonNull(router, "router");
	}

	/**
	 * 只在一个候选以 APPLIED/STOP 完成只读评估时允许交互；无匹配、拒绝、失败或伪造 authority 均拒绝。
	 * Allows interaction only when one candidate completes read-only evaluation as APPLIED/STOP; no match, rejection,
	 * failure, or forged authority is denied.
	 */
	public boolean isAllowed(String eventId, long occurredAt, InteractionSnapshot snapshot, PlayerQuestGraphStateList states,
			Function<ConditionInvocation, ConditionResult> conditionEvaluator) {
		try {
			Objects.requireNonNull(snapshot, "snapshot");
			Objects.requireNonNull(states, "states");
			Objects.requireNonNull(conditionEvaluator, "conditionEvaluator");
			InteractionEligibilityEvent event = event(eventId, occurredAt, snapshot);
			DispatchResult result = router.dispatch(event, states,
				match -> QuestGraphTransitionExecutor.evaluateReadOnly(match,
					new ReadOnlyContext(snapshot.playerId(), states, conditionEvaluator)));
			return result.status() == Status.APPLIED && result.propagation() == Propagation.STOP;
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** 从服务端快照创建可持久化的 typed eligibility event。 / Creates a persistable typed eligibility event from a server snapshot. */
	public static InteractionEligibilityEvent event(String eventId, long occurredAt, InteractionSnapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot");
		return new InteractionEligibilityEvent(eventId, snapshot.playerId(), occurredAt, snapshot.objectTemplateId(), snapshot.objectId(),
			snapshot.worldId(), snapshot.instanceId(), snapshot.action(), snapshot.serverInteractionAvailable());
	}

	/** 保存对象服务签发的不可变交互快照。 / Holds an immutable interaction snapshot issued by the object service. */
	public record InteractionSnapshot(int playerId, int objectTemplateId, int objectId, int worldId, int instanceId,
			InteractionAction action, boolean serverInteractionAvailable) {
		/** 校验结构边界；authority=false 保留给 fail-closed 查询结果。 / Validates structural bounds while retaining authority=false for fail-closed results. */
		public InteractionSnapshot {
			if (playerId <= 0 || objectTemplateId <= 0 || objectId <= 0 || worldId <= 0 || instanceId <= 0 || action == null) {
				throw new IllegalArgumentException("Interaction snapshot is invalid");
			}
		}
	}
}
