package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry.Snapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ContinuationCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemUseContinuationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/** Recovers every persisted PREPARED journal through one complete typed context factory. */
public final class QuestGraphPreparedRecoveryCoordinator {

	private final int playerId;
	private final PlayerQuestGraphStateList states;
	private final QuestGraphDefinitionRegistry registry;
	private final QuestGraphTransitionExecutor executor;
	private final QuestGraphCompleteTransitionContextFactory contexts;

	public QuestGraphPreparedRecoveryCoordinator(int playerId, PlayerQuestGraphStateList states,
			QuestGraphDefinitionRegistry registry, QuestGraphTransitionExecutor executor,
			QuestGraphCompleteTransitionContextFactory contexts) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Prepared recovery player id is invalid");
		}
		this.playerId = playerId;
		this.states = Objects.requireNonNull(states, "quest graph states");
		this.registry = Objects.requireNonNull(registry, "quest graph definition registry");
		this.executor = Objects.requireNonNull(executor, "quest graph transition executor");
		this.contexts = Objects.requireNonNull(contexts, "complete transition context factory");
	}

	/** Recovers the ordered snapshot of all journals that are still PREPARED. */
	public ActionResult recoverPrepared() {
		Snapshot definitions = registry.snapshot();
		for (PlayerQuestGraphState candidate : states.snapshot()) {
			if (candidate.getLifecycle() != Lifecycle.PREPARED) {
				continue;
			}
			CompiledQuestGraph graph = definitions.data().graphs().get(candidate.getQuestId());
			if (graph == null) {
				return ActionResult.FAILED;
			}
			ActionResult result = recover(graph);
			if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
				return ActionResult.FAILED;
			}
		}
		return ActionResult.APPLIED;
	}

	/** Resumes one exact persisted item-use barrier and rejects stale callback authority. */
	public ActionResult resume(ContinuationCommand command) {
		if (command == null || command.playerId() != playerId) {
			return ActionResult.FAILED;
		}
		synchronized (states) {
			PlayerQuestGraphState state = states.get(command.questId());
			if (!matches(state, command)) {
				return ActionResult.ALREADY_APPLIED;
			}
			CompiledQuestGraph graph = registry.snapshot().data().graphs().get(command.questId());
			return graph == null ? ActionResult.FAILED : recover(graph);
		}
	}

	private ActionResult recover(CompiledQuestGraph graph) {
		synchronized (states) {
			PlayerQuestGraphState current = states.get(graph.questId());
			if (current == null || current.getLifecycle() != Lifecycle.PREPARED) {
				return ActionResult.ALREADY_APPLIED;
			}
			TransitionContext context = Objects.requireNonNull(contexts.create(graph, this::resume), "complete transition context");
			if (context.playerId() != playerId || context.states() != states) {
				throw new IllegalStateException("Prepared recovery context does not own the player state");
			}
			return executor.recover(graph, context) == Status.APPLIED ? ActionResult.APPLIED : ActionResult.FAILED;
		}
	}

	private static boolean matches(PlayerQuestGraphState state, ContinuationCommand command) {
		if (state == null || state.getLifecycle() != Lifecycle.PREPARED || state.getJournal() == null
				|| state.getQuestId() != command.questId() || state.getJournal().getBaseRevision() != command.baseRevision()
				|| !state.getJournal().getTransitionId().equals(command.transitionId())
				|| !state.getJournal().getEventId().equals(command.eventId())) {
			return false;
		}
		ItemUseContinuationPlan plan = state.getJournal().getItemUseContinuationPlans().get(command.actionIndex());
		return state.getJournal().getNextActionIndex() == command.actionIndex() && plan != null
			&& plan.itemId() == command.itemId() && plan.itemObjectId() == command.itemObjectId()
			&& plan.durationMs() == command.durationMs() && plan.readyAt() == command.readyAt()
			&& command.idempotencyKey().equals(QuestGraphTransitionExecutor.actionIdempotencyKey(command.eventId(), command.questId(),
				command.transitionId(), command.playerId(), command.actionIndex()));
	}
}
