package com.aionemu.gameserver.questEngine.e2e.journey;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.QuestE2ePacketValidator;
import com.aionemu.gameserver.questEngine.e2e.QuestE2eStatus;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestRouteResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 在单一真实协议回环和持续内存运行时中执行生产 XML 规划路径，
 * 并在首个不变量失败时停止。
 * Executes a production-XML plan in one real protocol loop and persistent in-memory runtime, stopping at the first
 * failed invariant.
 */
public final class QuestProductionJourneyExecutor {
	/** 首个失败步骤的完整诊断。 / Complete diagnosis for the first failing step. */
	public record Failure(int stepIndex, QuestE2eStatus status, String reason,
			QuestProductionJourneyPlanner.PlannedStep expected, QuestJourneyRunner.Step observed) {
		public Failure {
			if (stepIndex < 0) throw new IllegalArgumentException("stepIndex must be non-negative");
			status = Objects.requireNonNull(status, "status");
			reason = requireText(reason, "reason");
			expected = Objects.requireNonNull(expected, "expected");
		}
	}

	/** 一条规划路径的执行结果。 / Execution result for one planned path. */
	public record Result(int questId, boolean completed, List<QuestJourneyRunner.Step> steps, Failure failure) {
		public Result {
			if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
			steps = List.copyOf(steps);
			if (completed == (failure != null)) {
				throw new IllegalArgumentException("completed result and failure must be mutually exclusive");
			}
		}
	}

	/**
	 * 执行一条完全由生产定义规划的路径。
	 * Executes one path planned entirely from the production definition.
	 */
	public Result execute(CompiledQuestDefinition definition, ClientResourceOracle oracle,
			QuestProductionJourneyPlanner.Plan plan) throws Exception {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(oracle, "oracle");
		Objects.requireNonNull(plan, "plan");
		if (definition.id() != plan.questId()) {
			throw new IllegalArgumentException("plan does not belong to definition");
		}
		List<QuestJourneyRunner.Step> observedSteps = new ArrayList<>();
		QuestTransition ingress = plan.steps().stream().map(QuestProductionJourneyPlanner.PlannedStep::transition)
			.filter(Objects::nonNull).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("journey plan has no production ingress"));
		List<QuestTransition> journeyTransitions = plan.steps().stream()
			.map(QuestProductionJourneyPlanner.PlannedStep::transition).filter(Objects::nonNull).toList();
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, ingress, oracle, plan.playerClass(),
			plan.initialInventory(), journeyTransitions)) {
			for (int index = 0; index < plan.steps().size(); index++) {
				QuestProductionJourneyPlanner.PlannedStep expected = plan.steps().get(index);
				if (expected.transition() != null) journey.prepareStep(expected.transition());
				QuestStatus beforeStatus = journey.status();
				int beforePackedVariables = journey.packedVariables();
				Map<Integer, Integer> beforeInventory = journey.inventorySnapshot();
				QuestJourneyRunner.Step observed;
				try {
					observed = executeStep(journey, expected);
				} catch (RuntimeException failure) {
					return failed(definition.id(), observedSteps, index, QuestE2eStatus.CLICK_NO_RESPONSE,
						failure.getClass().getSimpleName() + ":" + failure.getMessage(), expected, null);
				}
				observedSteps.add(observed);
				Failure failure = validate(definition, oracle, index, expected, beforeStatus, beforePackedVariables,
					beforeInventory, observed);
				if (failure != null) return new Result(definition.id(), false, observedSteps, failure);
			}
			if (journey.status() != QuestStatus.COMPLETE) {
				QuestProductionJourneyPlanner.PlannedStep expected = plan.steps().getLast();
				return failed(definition.id(), observedSteps, plan.steps().size() - 1,
					QuestE2eStatus.STATE_MISMATCH, "planned path ended without COMPLETE status", expected,
					observedSteps.getLast());
			}
			return new Result(definition.id(), true, observedSteps, null);
		}
	}

	private static QuestJourneyRunner.Step executeStep(QuestJourneyRunner journey,
			QuestProductionJourneyPlanner.PlannedStep step) {
		return switch (step.kind()) {
			case INTERACT -> {
				QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) step.transition().event();
				yield journey.interact(talk.npcId(), talk.dialogId() == null ? 0 : talk.dialogId());
			}
			case TARGETLESS_ACTION -> journey.clickTargetlessAction(dialogId(step.transition().event()));
			case PAGE_ACTION -> journey.clickVisibleAction(dialogId(step.transition().event()));
			case CLIENT_LOCAL_FINISH_DIALOG -> journey.finishDialogLocally();
			case NATIVE_REWARD_ACTION -> journey.clickNativeAction(dialogId(step.transition().event()));
			case USE_OBJECT -> journey.useObject(((QuestEvent.TalkToNpc) step.transition().event()).npcId());
			case USE_OBJECT_DROP -> journey.useObjectAndReceiveMetadataDrop(
				((QuestEvent.TalkToNpc) step.transition().event()).npcId(), step.metadataDrop().itemId());
			case USE_ITEM -> journey.useItem(((QuestEvent.UseItem) step.transition().event()).itemId());
			case ITEM_PLAY -> {
				QuestEvent.ItemPlay itemPlay = (QuestEvent.ItemPlay) step.transition().event();
				yield journey.playItem(itemPlay.itemId(), itemPlay.animationMillis());
			}
			case WORLD_EVENT -> journey.emitWorldEvent(step.transition());
		};
	}

	private static Failure validate(CompiledQuestDefinition definition, ClientResourceOracle oracle, int stepIndex,
			QuestProductionJourneyPlanner.PlannedStep expected, QuestStatus beforeStatus, int beforePackedVariables,
			Map<Integer, Integer> beforeInventory,
			QuestJourneyRunner.Step observed) {
		if (expected.kind() == QuestProductionJourneyPlanner.StepKind.CLIENT_LOCAL_FINISH_DIALOG) {
			return validateClientLocalFinish(stepIndex, expected, beforeStatus, beforePackedVariables, observed);
		}
		if (!observed.auditEvents().isEmpty()) {
			var audit = observed.auditEvents().getFirst();
			QuestE2eStatus status = audit.committed()
				? QuestE2eStatus.AFTER_COMMIT_FAILURE : QuestE2eStatus.TRANSACTION_FAILURE;
			return failure(stepIndex, status, audit.failureStage() + ":" + audit.actionType() + ":"
				+ audit.rootFailureType() + ":" + audit.rootFailureMessage(), expected, observed);
		}
		if (observed.outcome().failed()) {
			RuntimeException failure = observed.outcome().failure();
			return failure(stepIndex, QuestE2eStatus.TRANSACTION_FAILURE,
				failure == null ? "runtime marked request failed" : failure.getClass().getName() + ":" + failure.getMessage(),
				expected, observed);
		}
		if (observed.matchedRouteResult() != QuestRouteResult.HANDLED
				&& observed.matchedRouteResult() != QuestRouteResult.BLOCKED) {
			return failure(stepIndex, QuestE2eStatus.NO_MATCH,
				"request did not produce a conclusive handled route; candidates=" + observed.routeCandidateCount(),
				expected, observed);
		}
		QuestTransition expectedTransition = expected.transition();
		if (!observed.matchedTransitionCandidates().contains(expectedTransition)) {
			QuestE2eStatus status = observed.matchedTransitionCandidates().isEmpty()
				? QuestE2eStatus.NO_MATCH : QuestE2eStatus.AMBIGUOUS_ROUTE;
			return failure(stepIndex, status,
				"expected production transition was not attributable; attributed="
					+ routeNames(observed.matchedTransitionCandidates()), expected, observed);
		}
		if (observed.matchedTransitionCandidates().size() > 1) {
			return failure(stepIndex, QuestE2eStatus.AMBIGUOUS_ROUTE,
				"protocol request is attributable to multiple production transitions "
					+ routeNames(observed.matchedTransitionCandidates()), expected, observed);
		}
		NodeProjection target = projection(definition, expected.transition().targetNode());
		if (target == null) {
			return failure(stepIndex, QuestE2eStatus.STATE_MISMATCH,
				"production transition target node is absent", expected, observed);
		}
		QuestStatus expectedStatus = target.status();
		Map<String, Integer> expectedVariables = new java.util.LinkedHashMap<>(
			definition.definition().progressLayout().unpack(beforePackedVariables));
		Set<String> actionTouchedFields = new java.util.HashSet<>();
		for (var action : expected.transition().actions()) {
			if (action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.SetStatus setStatus) {
				expectedStatus = setStatus.status();
			} else if (action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.SetVariable set) {
				expectedVariables.put(set.field(), set.value());
				actionTouchedFields.add(set.field());
			} else if (action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.IncrementVariable increment) {
				expectedVariables.merge(increment.field(), increment.delta(), Integer::sum);
				actionTouchedFields.add(increment.field());
			}
		}
		target.variables().forEach((field, value) -> {
			if (!actionTouchedFields.contains(field)) expectedVariables.put(field, value);
		});
		if (expectedStatus != observed.status()) {
			return failure(stepIndex, QuestE2eStatus.STATE_MISMATCH,
				"target status does not match production projection", expected, observed);
		}
		Map<String, Integer> observedVariables = definition.definition().progressLayout()
			.unpack(observed.packedVariables());
		boolean variablesMatch = expectedVariables.entrySet().stream()
			.allMatch(entry -> Objects.equals(observedVariables.get(entry.getKey()), entry.getValue()));
		if (!variablesMatch) {
			return failure(stepIndex, QuestE2eStatus.STATE_MISMATCH,
				"target variables do not match production projection expected=" + expectedVariables
					+ " actual=" + observedVariables, expected, observed);
		}
		boolean observableResponse = !observed.outcome().packets().isEmpty()
			|| !observed.committedActions().isEmpty()
			|| hasObservableWorldEffect(expectedTransition)
			|| expected.metadataDrop() != null;
		if (observed.outcome().stateChanged() && !observableResponse && !expected.transition().afterCommit().isEmpty()) {
			return failure(stepIndex, QuestE2eStatus.STATE_CHANGED_WITHOUT_RESPONSE,
				"state changed but the declared response produced no observable output", expected, observed);
		}
		if (isClientStep(expected.kind()) && !observed.outcome().stateChanged() && !observableResponse) {
			return failure(stepIndex, QuestE2eStatus.CLICK_NO_RESPONSE,
				"client action matched but produced no observable response", expected, observed);
		}
		QuestE2ePacketValidator.Result packetResult = QuestE2ePacketValidator.validate(definition,
			expected.transition(), observed.expectedDialogTargetObjectId(), observed.outcome().packets());
		if (!packetResult.valid()) {
			return failure(stepIndex, packetResult.status(), packetResult.reason(), expected, observed);
		}
		for (ServerPacketObservation packet : observed.outcome().packets()) {
			if (packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW && packet.dialogId() > 0
					&& !oracle.pageExists(definition.id(), packet.dialogId())) {
				return failure(stepIndex, QuestE2eStatus.PAGE_NOT_IN_CLIENT,
					"server page " + packet.dialogId() + " is absent from Aion 5.8 client tables", expected, observed);
			}
		}
		if (expected.metadataDrop() != null) {
			int itemId = expected.metadataDrop().itemId();
			int beforeCount = beforeInventory.getOrDefault(itemId, 0);
			if (observed.inventory().getOrDefault(itemId, 0) != beforeCount + 1) {
				return failure(stepIndex, QuestE2eStatus.STATE_MISMATCH,
					"deterministic metadata drop was not added exactly once", expected, observed);
			}
		}
		return null;
	}

	/**
	 * 验证客户端结束按钮确实走过真实协议、服务端保持无 route，且只有客户端本地关闭页面。
	 * Verifies that a client finish action traversed the real protocol, the server stayed unrouted, and only the
	 * client closed the page locally.
	 */
	private static Failure validateClientLocalFinish(int stepIndex,
			QuestProductionJourneyPlanner.PlannedStep expected, QuestStatus beforeStatus, int beforePackedVariables,
			QuestJourneyRunner.Step observed) {
		if (!observed.auditEvents().isEmpty()) {
			return failure(stepIndex, QuestE2eStatus.TRANSACTION_FAILURE,
				"client-local finish emitted quest audit events", expected, observed);
		}
		if (observed.outcome().failed()) {
			RuntimeException failure = observed.outcome().failure();
			return failure(stepIndex, QuestE2eStatus.TRANSACTION_FAILURE,
				failure == null ? "client-local finish failed" : failure.getClass().getName() + ":" + failure.getMessage(),
				expected, observed);
		}
		if (observed.outcome().handled() || observed.outcome().stateChanged() || !observed.outcome().packets().isEmpty()
				|| observed.matchedRouteResult() != QuestRouteResult.UNKNOWN
				|| !observed.matchedTransitionCandidates().isEmpty()) {
			return failure(stepIndex, QuestE2eStatus.AMBIGUOUS_ROUTE,
				"client-local finish unexpectedly used a production route or server response", expected, observed);
		}
		if (observed.status() != beforeStatus || observed.packedVariables() != beforePackedVariables) {
			return failure(stepIndex, QuestE2eStatus.STATE_MISMATCH,
				"client-local finish changed quest state", expected, observed);
		}
		if (observed.page() != 0) {
			return failure(stepIndex, QuestE2eStatus.CLICK_NO_RESPONSE,
				"client-local finish did not close the current page", expected, observed);
		}
		return null;
	}

	private static boolean hasObservableWorldEffect(QuestTransition transition) {
		return transition.afterCommit().stream().anyMatch(action ->
			!(action instanceof AfterCommitAction.SyncQuestState)
				&& !(action instanceof AfterCommitAction.ShowQuestDialog)
				&& !(action instanceof AfterCommitAction.ShowQuestSelectionDialog)
				&& !(action instanceof AfterCommitAction.ShowDialogWindow)
				&& !(action instanceof AfterCommitAction.CloseDialog));
	}

	private static boolean isClientStep(QuestProductionJourneyPlanner.StepKind kind) {
		return kind != QuestProductionJourneyPlanner.StepKind.WORLD_EVENT;
	}

	private static NodeProjection projection(CompiledQuestDefinition definition, String node) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(node))
			.map(candidate -> candidate.projection())
			.findFirst().orElse(null);
	}

	private static int dialogId(QuestEvent event) {
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId() == null ? 0 : talk.dialogId();
			case QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> throw new IllegalArgumentException("step does not carry a dialog action");
		};
	}

	private static String routeNames(List<QuestTransition> transitions) {
		return transitions.stream()
			.map(transition -> transition.sourceNode() + "->" + transition.targetNode())
			.toList().toString();
	}

	private static Result failed(int questId, List<QuestJourneyRunner.Step> steps, int stepIndex,
			QuestE2eStatus status, String reason, QuestProductionJourneyPlanner.PlannedStep expected,
			QuestJourneyRunner.Step observed) {
		return new Result(questId, false, steps, failure(stepIndex, status, reason, expected, observed));
	}

	private static Failure failure(int stepIndex, QuestE2eStatus status, String reason,
			QuestProductionJourneyPlanner.PlannedStep expected, QuestJourneyRunner.Step observed) {
		return new Failure(stepIndex, status, reason, expected, observed);
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
		return value;
	}
}
