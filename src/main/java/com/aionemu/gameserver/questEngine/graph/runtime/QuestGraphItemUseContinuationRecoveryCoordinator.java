package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ContinuationCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemUseContinuationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 在登录或到期 callback 上校验当前 PREPARED journal，再恢复 durable item-use tail。
 * Validates the current PREPARED journal on login or wake-up before resuming a durable item-use tail.
 */
public final class QuestGraphItemUseContinuationRecoveryCoordinator {

	private final int playerId;
	private final PlayerQuestGraphStateList states;
	private final QuestGraphDefinitionRegistry registry;
	private final QuestGraphTransitionExecutor executor;
	private final BiFunction<CompiledQuestGraph, Function<ContinuationCommand, ActionResult>, TransitionContext> contexts;

	/** 绑定在线玩家和其已加载状态；adapter 可直接使用 {@link #resume(ContinuationCommand)} 作为唤醒端点。 */
	public QuestGraphItemUseContinuationRecoveryCoordinator(Player player, QuestGraphDefinitionRegistry registry,
			QuestGraphTransitionExecutor executor,
			BiFunction<CompiledQuestGraph, Function<ContinuationCommand, ActionResult>, TransitionContext> contexts) {
		this(requirePlayer(player).getObjectId(), player.getQuestGraphStateList(), registry, executor, contexts);
	}

	/** 创建可注入的恢复协调器，供确定性验证和非 Player composition root 使用。 */
	QuestGraphItemUseContinuationRecoveryCoordinator(int playerId, PlayerQuestGraphStateList states,
			QuestGraphDefinitionRegistry registry, QuestGraphTransitionExecutor executor,
			BiFunction<CompiledQuestGraph, Function<ContinuationCommand, ActionResult>, TransitionContext> contexts) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Item-use continuation recovery player id is invalid");
		}
		this.playerId = playerId;
		this.states = Objects.requireNonNull(states, "states");
		this.registry = Objects.requireNonNull(registry, "registry");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.contexts = Objects.requireNonNull(contexts, "contexts");
	}

	/** 恢复玩家全部待处理 continuation；任一损坏或失败都显式关闭。 / Recovers every pending continuation and fails closed on corruption or failure. */
	public ActionResult recoverOnLogin() {
		for (PlayerQuestGraphState state : states.snapshot()) {
			if (!hasRecoverableContinuation(state)) {
				continue;
			}
			CompiledQuestGraph graph = registry.snapshot().data().graphs().get(state.getQuestId());
			if (graph == null || executor.recover(graph, requireContext(graph)) != Status.APPLIED) {
				return ActionResult.FAILED;
			}
		}
		return ActionResult.APPLIED;
	}

	/** 唤醒指定 journal；旧 callback 或身份漂移只返回失败，不执行 tail。 / Wakes one exact journal; stale callbacks or identity drift fail without executing the tail. */
	public ActionResult resume(ContinuationCommand command) {
		if (command == null || command.playerId() != playerId) {
			return ActionResult.FAILED;
		}
		PlayerQuestGraphState state = states.get(command.questId());
		if (!matches(state, command)) {
			return ActionResult.ALREADY_APPLIED;
		}
		CompiledQuestGraph graph = registry.snapshot().data().graphs().get(command.questId());
		return graph != null && executor.recover(graph, requireContext(graph)) == Status.APPLIED
			? ActionResult.APPLIED : ActionResult.FAILED;
	}

	private TransitionContext requireContext(CompiledQuestGraph graph) {
		TransitionContext context = contexts.apply(graph, this::resume);
		if (context == null || context.playerId() != playerId || context.states() != states) {
			throw new IllegalStateException("Item-use continuation recovery context does not own the player state");
		}
		return context;
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	private static boolean hasRecoverableContinuation(PlayerQuestGraphState state) {
		return state != null && state.getLifecycle() == Lifecycle.PREPARED && state.getJournal() != null
			&& !state.getJournal().getItemUseContinuationPlans().isEmpty();
	}

	private static boolean matches(PlayerQuestGraphState state, ContinuationCommand command) {
		if (!hasRecoverableContinuation(state) || state.getQuestId() != command.questId()
				|| state.getJournal().getBaseRevision() != command.baseRevision()
				|| !state.getJournal().getTransitionId().equals(command.transitionId())
				|| !state.getJournal().getEventId().equals(command.eventId())) {
			return false;
		}
		ItemUseContinuationPlan plan = state.getJournal().getItemUseContinuationPlans().get(command.actionIndex());
		return state.getJournal().getNextActionIndex() == command.actionIndex() && plan != null
			&& plan.itemId() == command.itemId() && plan.itemObjectId() == command.itemObjectId()
			&& plan.durationMs() == command.durationMs() && plan.readyAt() == command.readyAt()
			&& command.idempotencyKey().equals(idempotencyKey(command));
	}

	private static String idempotencyKey(ContinuationCommand command) {
		return QuestGraphTransitionExecutor.actionIdempotencyKey(command.eventId(), command.questId(), command.transitionId(),
			command.playerId(), command.actionIndex());
	}
}
