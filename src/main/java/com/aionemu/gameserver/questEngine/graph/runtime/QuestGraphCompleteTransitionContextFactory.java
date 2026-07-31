package com.aionemu.gameserver.questEngine.graph.runtime;

import java.time.ZoneId;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ContinuationCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/** Builds the single complete typed transition context used by prepared-journal recovery. */
public final class QuestGraphCompleteTransitionContextFactory {

	private final int playerId;
	private final int accessLevel;
	private final ZoneId serverZoneId;
	private final PlayerQuestGraphStateList states;
	private final CapabilityProvider capabilities;

	public QuestGraphCompleteTransitionContextFactory(int playerId, int accessLevel, ZoneId serverZoneId,
			PlayerQuestGraphStateList states, CapabilityProvider capabilities) {
		if (playerId <= 0 || accessLevel < 0) {
			throw new IllegalArgumentException("Quest graph context owner is invalid");
		}
		this.playerId = playerId;
		this.accessLevel = accessLevel;
		this.serverZoneId = Objects.requireNonNull(serverZoneId, "server zone id");
		this.states = Objects.requireNonNull(states, "quest graph states");
		this.capabilities = Objects.requireNonNull(capabilities, "quest graph capability provider");
	}

	public TransitionContext create(CompiledQuestGraph graph, Function<ContinuationCommand, ActionResult> continuationResumer) {
		CompiledQuestGraph definition = Objects.requireNonNull(graph, "compiled quest graph");
		Function<ContinuationCommand, ActionResult> resumer = Objects.requireNonNull(continuationResumer, "continuation resumer");
		Capabilities complete = Objects.requireNonNull(capabilities.provide(definition, resumer), "complete quest graph capabilities");
		return TransitionContext.complete(playerId, accessLevel, serverZoneId, states,
			complete.conditionEvaluator(), complete.actionPreflight(), complete.actionExecutor(), complete.itemActions(),
			complete.timerActions(), complete.movieActions(), complete.itemUseAnimations(), complete.dialogProtocol(),
			complete.systemMessages(), complete.questStatusSync(), complete.flightTeleports(), complete.teleportActions(),
			complete.instanceSpawns(), complete.escortActions(), complete.lifecycleActions(), complete.dialogNpcLifecycle(),
			complete.postCommitProtocol(), complete.recipeBridge(), complete.craftSkillRewards(), complete.persistence());
	}

	/** Supplies graph-specific, fully operational capabilities without replacing missing endpoints with failure stubs. */
	@FunctionalInterface
	public interface CapabilityProvider {
		Capabilities provide(CompiledQuestGraph graph, Function<ContinuationCommand, ActionResult> continuationResumer);
	}

	/** The complete typed dependency set required by {@link TransitionContext#complete}. */
	public record Capabilities(Function<ConditionInvocation, ConditionResult> conditionEvaluator,
		Function<ActionInvocation, PreflightResult> actionPreflight,
		Function<ActionInvocation, ActionResult> actionExecutor,
		QuestGraphItemActionAdapter itemActions,
		QuestGraphTimerActionAdapter timerActions,
		QuestGraphMovieActionAdapter movieActions,
		QuestGraphItemUseAnimationAdapter itemUseAnimations,
		QuestGraphDialogProtocolAdapter dialogProtocol,
		QuestGraphSystemMessageAdapter systemMessages,
		QuestGraphQuestStatusSyncAdapter questStatusSync,
		QuestGraphFlightTeleportAdapter flightTeleports,
		QuestGraphTeleportActionAdapter teleportActions,
		QuestGraphInstanceSpawnAdapter instanceSpawns,
		QuestGraphEscortActionAdapter escortActions,
		QuestGraphLifecycleActionAdapter lifecycleActions,
		QuestGraphDialogNpcLifecycleAdapter dialogNpcLifecycle,
		QuestGraphPostCommitProtocolAdapter postCommitProtocol,
		QuestGraphRecipeBridge recipeBridge,
		QuestGraphCraftSkillRewardBridge craftSkillRewards,
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
		public Capabilities {
			Objects.requireNonNull(conditionEvaluator, "condition evaluator");
			Objects.requireNonNull(actionPreflight, "action preflight");
			Objects.requireNonNull(actionExecutor, "action executor");
			Objects.requireNonNull(itemActions, "item actions");
			Objects.requireNonNull(timerActions, "timer actions");
			Objects.requireNonNull(movieActions, "movie actions");
			Objects.requireNonNull(itemUseAnimations, "item-use animations");
			Objects.requireNonNull(dialogProtocol, "dialog protocol");
			Objects.requireNonNull(systemMessages, "system messages");
			Objects.requireNonNull(questStatusSync, "quest status sync");
			Objects.requireNonNull(flightTeleports, "flight teleports");
			Objects.requireNonNull(teleportActions, "teleport actions");
			Objects.requireNonNull(instanceSpawns, "instance spawns");
			Objects.requireNonNull(escortActions, "escort actions");
			Objects.requireNonNull(lifecycleActions, "lifecycle actions");
			Objects.requireNonNull(dialogNpcLifecycle, "dialog NPC lifecycle");
			Objects.requireNonNull(postCommitProtocol, "post-commit protocol");
			Objects.requireNonNull(recipeBridge, "recipe bridge");
			Objects.requireNonNull(craftSkillRewards, "craft-skill rewards");
			Objects.requireNonNull(persistence, "persistence");
		}
	}
}
