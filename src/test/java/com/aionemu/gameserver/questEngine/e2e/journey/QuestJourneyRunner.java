package com.aionemu.gameserver.questEngine.e2e.journey;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在一个持续运行时和真实客户端协议回环中，从入口依次驱动任务到终点；
 * 构造后不会重新准备或投影任务状态。
 * Drives a quest sequentially from ingress to completion in one persistent runtime and real client protocol loop;
 * it never prepares or re-projects quest state after construction.
 */
public final class QuestJourneyRunner implements AutoCloseable {
	private static final int FIRST_OBJECT_ID = 1_900_000;
	private final CompiledQuestDefinition definition;
	private final QuestE2eRuntime runtime;
	private final QuestProtocolLoop protocol;
	private final QuestHeadlessClient client;
	private final QuestTransition initialTransition;
	private final List<Step> steps = new java.util.ArrayList<>();
	private int nextObjectId = FIRST_OBJECT_ID;
	private int traceCursor;

	/** 一步请求提交后的完整可观察快照。 / Complete observable snapshot after one request. */
	public record Step(String label, QuestHeadlessClient.DispatchOutcome outcome, QuestStatus status,
			int packedVariables, int page, int npcId, int objectId, List<QuestAction> committedActions,
			List<QuestTrace.Entry> trace) {
		public Step {
			label = Objects.requireNonNull(label, "label");
			outcome = Objects.requireNonNull(outcome, "outcome");
			status = Objects.requireNonNull(status, "status");
			committedActions = List.copyOf(committedActions);
			trace = List.copyOf(trace);
		}
	}

	/**
	 * 建立单任务 Journey；只在这里准备首个入口 transition，之后所有进度均来自真实请求。
	 * Creates a single-quest journey; the initial transition is prepared only here and every later projection comes
	 * from a real request.
	 */
	public QuestJourneyRunner(CompiledQuestDefinition definition, QuestTransition initialTransition,
			ClientResourceOracle oracle) throws Exception {
		this.definition = Objects.requireNonNull(definition, "definition");
		this.initialTransition = Objects.requireNonNull(initialTransition, "initialTransition");
		ClientResourceOracle clientOracle = Objects.requireNonNull(oracle, "oracle");
		if (!this.definition.definition().transitions().contains(initialTransition)) {
			throw new IllegalArgumentException("initial transition does not belong to the quest definition");
		}
		QuestE2eRuntime createdRuntime = new QuestE2eRuntime(this.definition);
		QuestProtocolLoop createdProtocol = null;
		try {
			createdRuntime.prepare(initialTransition);
			createdProtocol = new QuestProtocolLoop(createdRuntime);
		} catch (Exception failure) {
			createdRuntime.close();
			throw failure;
		}
		runtime = createdRuntime;
		protocol = createdProtocol;
		client = new QuestHeadlessClient(runtime.state(), clientOracle,
			protocol::dispatch, runtime.trace());
		traceCursor = runtime.trace().entries().size();
	}

	/**
	 * 通过真实 {@code CM_DIALOG_SELECT} 与指定 NPC 开始一次交互，并种入 KnownList 权威对象。
	 * Starts an interaction with the named NPC through a real {@code CM_DIALOG_SELECT} and exposes the authoritative
	 * object through KnownList.
	 */
	public Step interact(int npcId, int actionId) {
		int objectId = nextObjectId++;
		runtime.world().seedInteractionNpc(npcId, objectId);
		return perform("npc:" + npcId + ":" + actionId,
			() -> client.clickNpc(npcId, objectId, runtime.state().questId(), actionId));
	}

	/**
	 * 点击当前 Aion 5.8 任务页面实际可见的动作。
	 * Clicks an action visible on the current Aion 5.8 quest page.
	 */
	public Step clickVisibleAction(int actionId) {
		return perform("page-action:" + actionId, () -> client.clickPageAction(actionId));
	}

	/** 点击原生奖励选择窗口中的动作。 / Clicks an action in the native reward-selection window. */
	public Step clickNativeAction(int actionId) {
		return perform("native-action:" + actionId, () -> client.clickNativeAction(actionId));
	}

	/**
	 * 通过真实 {@code CM_SHOW_DIALOG} 和确定性的 AI 完成回调使用任务交互物，
	 * 并种入 KnownList 权威对象。
	 * Uses a quest interaction object through a real {@code CM_SHOW_DIALOG} and deterministic AI completion callback,
	 * exposing the authoritative object through KnownList.
	 */
	public Step useObject(int npcId) {
		int objectId = nextObjectId++;
		runtime.world().seedInteractionNpc(npcId, objectId);
		return perform("use-object:" + npcId, () -> client.useObject(npcId, objectId));
	}

	/**
	 * 向同一个持续运行时注入确定性的世界事件。
	 * Emits a deterministic world event into the same persistent runtime.
	 */
	public Step emitWorldEvent(QuestEvent event) {
		Objects.requireNonNull(event, "event");
		return perform("world:" + event.type(), () -> client.emitWorldEvent(event));
	}

	/** 返回当前任务状态。 / Returns the current quest status. */
	public QuestStatus status() { return runtime.state().status(); }

	/** 返回当前客户端页面。 / Returns the current client page. */
	public int page() { return runtime.state().currentPage(); }

	/** 返回当前进度字段值。 / Returns the current progress-field value. */
	public int variable(String field) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables())
			.getOrDefault(field, 0);
	}

	/**
	 * 返回构造时准备且在 Journey 中保持不变的入口 transition。
	 * Returns the construction-time ingress transition retained throughout the journey.
	 */
	public QuestTransition preparedTransition() {
		if (runtime.preparedTransition() != initialTransition) {
			throw new IllegalStateException("journey runtime was prepared again after construction");
		}
		return initialTransition;
	}

	/** 返回有序步骤快照。 / Returns the ordered step snapshots. */
	public List<Step> steps() { return List.copyOf(steps); }

	/**
	 * 返回客户端状态实际记录的出站包数量。
	 * Returns the outbound-packet count actually recorded by client state.
	 */
	public int observedPacketCount() { return runtime.state().packets().size(); }

	/** 返回完整的持续会话轨迹。 / Returns the complete persistent-session trace. */
	public List<QuestTrace.Entry> trace() { return runtime.trace().entries(); }

	@Override
	public void close() {
		try {
			protocol.close();
		} finally {
			runtime.close();
		}
	}

	private Step perform(String label, Supplier<QuestHeadlessClient.DispatchOutcome> request) {
		QuestHeadlessClient.DispatchOutcome outcome = request.get();
		List<QuestTrace.Entry> completeTrace = runtime.trace().entries();
		List<QuestTrace.Entry> stepTrace = completeTrace.subList(traceCursor, completeTrace.size());
		traceCursor = completeTrace.size();
		Step step = new Step(label, outcome, runtime.state().status(), runtime.state().packedVariables(),
			runtime.state().currentPage(), runtime.state().currentNpcId(), runtime.state().currentObjectId(),
			runtime.committedTransactionActions(), stepTrace);
		steps.add(step);
		return step;
	}
}
