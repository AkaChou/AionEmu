package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import com.aionemu.gameserver.questEngine.runtime.QuestAuditEvent;
import com.aionemu.gameserver.questEngine.runtime.QuestPostCommitFailure;
import com.aionemu.gameserver.questEngine.runtime.QuestExecutionFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 对生产 catalog 的全量、快速、隔离内存任务流进行 transition 级审计；不将客户端顺序报告当作期望值。
 * Runs a transition-level full-catalog fast audit in isolated memory flows without treating any client sequence
 * report as the expected value.
 */
public final class QuestE2eBatchAudit {
	private QuestE2eBatchAudit() {
	}

	/** 对目录中全部 executable owner 生成独立结果。 / Produces independent results for every executable owner in a catalog. */
	public static List<QuestE2eAuditRow> audit(QuestCatalog catalog, ClientResourceOracle oracle) {
		return audit(catalog, oracle, LegacyQuestEvidenceOracle.empty());
	}

	/** 使用客户端和旧正式模板两类独立证据生成全量结果。 / Produces full results with client and legacy-template evidence. */
	public static List<QuestE2eAuditRow> audit(QuestCatalog catalog, ClientResourceOracle oracle,
			LegacyQuestEvidenceOracle evidence) {
		return audit(catalog, oracle, evidence, Optional.empty());
	}

	/** 使用静态世界可达性证据生成全量结果。 / Produces full results with static-world reachability evidence. */
	static List<QuestE2eAuditRow> audit(QuestCatalog catalog, ClientResourceOracle oracle,
			LegacyQuestEvidenceOracle evidence, QuestWorldReachabilityOracle worldReachability) {
		return audit(catalog, oracle, evidence, Optional.of(worldReachability));
	}

	private static List<QuestE2eAuditRow> audit(QuestCatalog catalog, ClientResourceOracle oracle,
			LegacyQuestEvidenceOracle evidence, Optional<QuestWorldReachabilityOracle> worldReachability) {
		java.util.Objects.requireNonNull(catalog, "catalog");
		java.util.Objects.requireNonNull(oracle, "oracle");
		java.util.Objects.requireNonNull(evidence, "evidence");
		java.util.Objects.requireNonNull(worldReachability, "worldReachability");
		List<QuestE2eAuditRow> rows = new ArrayList<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			for (QuestTransition transition : definition.definition().transitions()) {
				rows.add(auditTransition(definition, transition, oracle, evidence, worldReachability));
			}
		}
		return List.copyOf(rows);
	}

	/** 审计单个 transition，便于重点任务门禁和单元测试。 / Audits one transition for focused gates and unit tests. */
	public static QuestE2eAuditRow auditTransition(CompiledQuestDefinition definition,
			QuestTransition transition, ClientResourceOracle oracle) {
		return auditTransition(definition, transition, oracle, LegacyQuestEvidenceOracle.empty());
	}

	/** 审计单个 transition，并对照旧正式模板合同。 / Audits one transition against the legacy retail-template contract. */
	public static QuestE2eAuditRow auditTransition(CompiledQuestDefinition definition,
			QuestTransition transition, ClientResourceOracle oracle, LegacyQuestEvidenceOracle evidence) {
		return auditTransition(definition, transition, oracle, evidence, Optional.empty());
	}

	/** 显式应用静态世界可达性门禁审计单条 transition。 / Audits one transition with the explicit static-world gate. */
	static QuestE2eAuditRow auditTransition(CompiledQuestDefinition definition,
			QuestTransition transition, ClientResourceOracle oracle, LegacyQuestEvidenceOracle evidence,
			QuestWorldReachabilityOracle worldReachability) {
		return auditTransition(definition, transition, oracle, evidence, Optional.of(worldReachability));
	}

	private static QuestE2eAuditRow auditTransition(CompiledQuestDefinition definition,
			QuestTransition transition, ClientResourceOracle oracle, LegacyQuestEvidenceOracle evidence,
			Optional<QuestWorldReachabilityOracle> worldReachability) {
		String runtimeReason = worldReachability.map(candidate ->
			candidate.runtimeRequiredReason(definition, transition)).orElse("");
		if (!runtimeReason.isEmpty()) {
			return worldRequiredRow(definition, transition, runtimeReason);
		}
		QuestE2eAuditRow fast = auditTransitionFast(definition, transition, oracle, evidence);
		return shouldEscalate(fast, transition) ? auditProtocol(definition, transition, oracle, fast) : fast;
	}

	private static QuestE2eAuditRow worldRequiredRow(CompiledQuestDefinition definition,
			QuestTransition transition, String reason) {
		QuestNode source = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.sourceNode())).findFirst().orElse(null);
		QuestNode target = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElse(null);
		return new QuestE2eAuditRow(definition.id(), transition.event().type(), transition.sourceNode(),
			transition.targetNode(), "", "", QuestE2eTransitionMatch.UNSUPPORTED_SCENARIO_FACTS, "STATIC_WORLD",
			target == null ? "" : target.projection().status().name(),
			source == null ? "" : source.projection().status().name(),
			source == null ? 0 : definition.definition().progressLayout().pack(source.projection().variables()),
			npcId(transition.event()), 0, dialogId(transition.event()), 0, QuestE2eStatus.RUNTIME_REQUIRED, reason,
			"STATIC_WORLD_DATA", List.of());
	}

	private static QuestE2eAuditRow auditTransitionFast(CompiledQuestDefinition definition,
			QuestTransition transition, ClientResourceOracle oracle, LegacyQuestEvidenceOracle evidence) {
		java.util.Objects.requireNonNull(evidence, "evidence");
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int npcId = npcId(transition.event());
			int objectId = runtime.expectedDialogTargetObjectId();
			int dialogId = dialogId(transition.event());
			if (runtime.unsupportedFacts()) {
				return row(definition, transition, runtime, oracle, QuestE2eStatus.RUNTIME_REQUIRED,
					"transition references facts unavailable to deterministic memory world", npcId, objectId, dialogId, 0);
			}
			var outcome = runtime.dispatchPrepared();
			int shownPage = firstPage(outcome.packets());
			QuestE2eStatus routeStatus = classifyRoute(runtime, outcome);
			if (routeStatus != QuestE2eStatus.PASS) {
				return row(definition, transition, runtime, oracle, routeStatus,
					routeReason(routeStatus, runtime, outcome), npcId, runtime.expectedDialogTargetObjectId(), dialogId, shownPage);
			}
			QuestE2ePacketValidator.Result packetResult = QuestE2ePacketValidator.validate(
				definition, transition, runtime.expectedDialogTargetObjectId(), outcome.packets());
			if (!packetResult.valid()) {
				return row(definition, transition, runtime, oracle, packetResult.status(), packetResult.reason(), npcId,
					runtime.expectedDialogTargetObjectId(), dialogId, shownPage);
			}
			String evidenceMismatch = evidence.mismatch(definition, transition, shownPage);
			if (!evidenceMismatch.isEmpty() && outcome.handled()) {
				return row(definition, transition, runtime, oracle, QuestE2eStatus.EVIDENCE_REQUIRED,
					evidenceMismatch, npcId, runtime.expectedDialogTargetObjectId(), dialogId, shownPage);
			}
			QuestE2eStatus status = classifyResponse(definition, transition, runtime, outcome, oracle, shownPage);
			String reason = reason(status, outcome, shownPage, definition, transition);
			return row(definition, transition, runtime, oracle, status, reason, npcId,
				runtime.expectedDialogTargetObjectId(), dialogId, shownPage);
		} catch (Exception failure) {
			return new QuestE2eAuditRow(definition.id(), transition.event().type(), transition.sourceNode(),
				transition.targetNode(), "", "", QuestE2eTransitionMatch.NO_TRANSITION_MATCHED,
				"FAST",
				"", "", 0, npcId(transition.event()), 0, dialogId(transition.event()), 0,
				QuestE2eStatus.RUNTIME_REQUIRED, failure.getClass().getSimpleName() + ":" + failure.getMessage(),
				"CURRENT_IR", List.of());
		}
	}

	private static boolean shouldEscalate(QuestE2eAuditRow row, QuestTransition transition) {
		boolean protocolEvent = transition.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.dialogId() != null && talk.dialogId() >= 0
			|| transition.event() instanceof QuestEvent.UseItem;
		if (!protocolEvent || row.transitionMatch() != QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED) {
			return false;
		}
		if (transition.event() instanceof QuestEvent.UseItem && transition.actions().stream()
				.anyMatch(QuestE2eBatchAudit::requiresItemProtocol)) {
			return true;
		}
		return switch (row.status()) {
			case CLICK_NO_RESPONSE, PAGE_NOT_IN_CLIENT, INVALID_INTERACTION_OBJECT, INVALID_DIALOG_PACKET,
				INVALID_PACKET_ORDER, STATE_CHANGED_WITHOUT_RESPONSE, AFTER_COMMIT_FAILURE, RUNTIME_REQUIRED -> true;
			default -> false;
		};
	}

	private static boolean requiresItemProtocol(QuestAction action) {
		return action instanceof QuestAction.BlockDefaultItemUse
			|| action instanceof QuestAction.GiveItem
			|| action instanceof QuestAction.RemoveItem
			|| action instanceof QuestAction.UnequipItem
			|| action instanceof QuestAction.GrantReward
			|| action instanceof QuestAction.GrantSelectedReward
			|| action instanceof QuestAction.CompleteQuest;
	}

	private static QuestE2eAuditRow auditProtocol(CompiledQuestDefinition definition, QuestTransition transition,
			ClientResourceOracle oracle, QuestE2eAuditRow fast) {
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			ClientActionRequest request = protocolRequest(definition, transition, runtime);
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(request);
				int shownPage = firstPage(outcome.packets());
				QuestE2eStatus status = classifyProtocol(definition, transition, runtime, outcome, oracle, shownPage);
				String reason = protocolReason(status, transition.event(), runtime, outcome, shownPage, fast.reason());
				String evidence = shownPage > 0 && oracle.pageExists(definition.id(), shownPage)
					? "AION_5_8_CLIENT" : fast.evidence();
				return new QuestE2eAuditRow(fast.questId(), fast.eventType(), fast.sourceNode(), fast.targetNode(),
					fast.matchedSourceNode(), fast.matchedTargetNode(), fast.transitionMatch(), protocolMode(transition.event()),
					fast.targetStatus(), runtime.state().status().name(), runtime.state().packedVariables(), fast.npcId(),
					runtime.expectedDialogTargetObjectId(), fast.dialogId(), shownPage, status, reason, evidence,
					runtime.trace().entries());
			}
		} catch (Exception failure) {
			return new QuestE2eAuditRow(fast.questId(), fast.eventType(), fast.sourceNode(), fast.targetNode(),
				fast.matchedSourceNode(), fast.matchedTargetNode(), fast.transitionMatch(), protocolMode(transition.event()),
				fast.targetStatus(), fast.observedStatus(), fast.observedPackedVariables(), fast.npcId(), fast.objectId(),
				fast.dialogId(), fast.shownPage(), QuestE2eStatus.RUNTIME_REQUIRED,
				"protocol loop failed:" + failure.getClass().getName() + ":" + failure.getMessage(), fast.evidence(), fast.trace());
		}
	}

	private static ClientActionRequest protocolRequest(CompiledQuestDefinition definition, QuestTransition transition,
			QuestE2eRuntime runtime) {
		return switch (transition.event()) {
			case QuestEvent.TalkToNpc talk -> ClientActionRequest.dialog(definition.id(), talk.npcId(),
				runtime.state().currentObjectId(), talk.dialogId());
			case QuestEvent.UseItem use -> ClientActionRequest.useItem(definition.id(), use.itemId(),
				runtime.expectedDialogTargetObjectId());
			default -> throw new IllegalArgumentException("event has no CM protocol request: " + transition.event().type());
		};
	}

	private static QuestE2eStatus classifyProtocol(CompiledQuestDefinition definition, QuestTransition transition,
			QuestE2eRuntime runtime, QuestHeadlessClient.DispatchOutcome outcome, ClientResourceOracle oracle, int shownPage) {
		if (outcome.failure() != null) return QuestE2eStatus.TRANSACTION_FAILURE;
		if (!runtime.auditEvents().isEmpty()) {
			QuestAuditEvent audit = runtime.auditEvents().getFirst();
			if (audit.rootFailureType().equals(UnsupportedOperationException.class.getName())) {
				return QuestE2eStatus.RUNTIME_REQUIRED;
			}
			return audit.committed() ? QuestE2eStatus.AFTER_COMMIT_FAILURE : QuestE2eStatus.TRANSACTION_FAILURE;
		}
		if (!outcome.handled() && !outcome.stateChanged() && outcome.packets().isEmpty()) {
			return QuestE2eStatus.INVALID_DIALOG_PACKET;
		}
		QuestE2ePacketValidator.Result packetResult = QuestE2ePacketValidator.validate(definition, transition,
			runtime.expectedDialogTargetObjectId(), outcome.packets());
		if (!packetResult.valid()) return packetResult.status();
		QuestNode target = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElse(null);
		if (target == null || runtime.state().status() != target.projection().status()) {
			return QuestE2eStatus.STATE_MISMATCH;
		}
		if (shownPage > 0 && !oracle.pageExists(definition.id(), shownPage)) return QuestE2eStatus.PAGE_NOT_IN_CLIENT;
		boolean response = hasObservableResponse(outcome, transition);
		if (!outcome.stateChanged() && !response && requiresQuestItemAiCompletion(definition, transition)) {
			return QuestE2eStatus.RUNTIME_REQUIRED;
		}
		if (outcome.handled() && !outcome.stateChanged() && !response && transition.actions().stream()
				.anyMatch(com.aionemu.gameserver.questEngine.definition.QuestAction.BlockDefaultItemUse.class::isInstance)) {
			return QuestE2eStatus.PASS;
		}
		if (outcome.stateChanged() && !response && !transition.afterCommit().isEmpty()) {
			return QuestE2eStatus.STATE_CHANGED_WITHOUT_RESPONSE;
		}
		if (!outcome.stateChanged() && !response) return QuestE2eStatus.CLICK_NO_RESPONSE;
		return QuestE2eStatus.PASS;
	}

	private static String protocolReason(QuestE2eStatus status, QuestEvent event, QuestE2eRuntime runtime,
			QuestHeadlessClient.DispatchOutcome outcome, int shownPage, String fastReason) {
		String mode = protocolMode(event);
		if (outcome.failure() != null) {
			return mode + ":" + outcome.failure().getClass().getName() + ":" + outcome.failure().getMessage();
		}
		if (!runtime.auditEvents().isEmpty()) {
			QuestAuditEvent audit = runtime.auditEvents().getFirst();
			return mode + ":" + audit.failureStage() + ":" + audit.actionType() + ":"
				+ audit.rootFailureType() + ":" + audit.rootFailureMessage();
		}
		if (status == QuestE2eStatus.PASS) return mode + " cleared fast status: " + fastReason;
		if (status == QuestE2eStatus.PAGE_NOT_IN_CLIENT) {
			return mode + " confirmed server page " + shownPage + " is absent from Aion 5.8 client tables";
		}
		return mode + " confirmed " + status;
	}

	private static String protocolMode(QuestEvent event) {
		return event instanceof QuestEvent.UseItem ? "CM_USE_ITEM" : "CM_DIALOG_SELECT";
	}

	private static QuestE2eStatus classifyRoute(QuestE2eRuntime runtime,
			com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient.DispatchOutcome outcome) {
		if (outcome.failure() != null) {
			Throwable failure = outcome.failure();
			if (cause(failure, QuestPostCommitFailure.class) != null) return QuestE2eStatus.AFTER_COMMIT_FAILURE;
			if (cause(failure, QuestExecutionFailureException.class) != null) return QuestE2eStatus.TRANSACTION_FAILURE;
			return QuestE2eStatus.TRANSACTION_FAILURE;
		}
		if (!runtime.auditEvents().isEmpty()) {
			QuestAuditEvent audit = runtime.auditEvents().getFirst();
			if (audit.rootFailureType().equals(UnsupportedOperationException.class.getName())) {
				return QuestE2eStatus.RUNTIME_REQUIRED;
			}
			return audit.committed() ? QuestE2eStatus.AFTER_COMMIT_FAILURE : QuestE2eStatus.TRANSACTION_FAILURE;
		}
		return switch (runtime.transitionMatch()) {
			case EXPECTED_TRANSITION_MATCHED -> QuestE2eStatus.PASS;
			case ALTERNATE_TRANSITION_MATCHED -> QuestE2eStatus.AMBIGUOUS_ROUTE;
			case NO_TRANSITION_MATCHED -> runtime.routeCandidateCount() == 0
				? QuestE2eStatus.NO_ROUTE : QuestE2eStatus.NO_MATCH;
			case UNSUPPORTED_SCENARIO_FACTS -> QuestE2eStatus.RUNTIME_REQUIRED;
		};
	}

	private static QuestE2eStatus classifyResponse(CompiledQuestDefinition definition, QuestTransition transition,
			QuestE2eRuntime runtime, com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient.DispatchOutcome outcome,
			ClientResourceOracle oracle, int shownPage) {
		QuestNode target = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElse(null);
		if (target == null || runtime.state().status() != target.projection().status()) return QuestE2eStatus.STATE_MISMATCH;
		if (shownPage > 0 && !oracle.pageExists(definition.id(), shownPage)) return QuestE2eStatus.PAGE_NOT_IN_CLIENT;
		if (requiresInteractionObject(transition) && runtime.state().currentObjectId() <= 0) {
			return QuestE2eStatus.INVALID_INTERACTION_OBJECT;
		}
		boolean response = hasObservableResponse(outcome, transition);
		if (!outcome.stateChanged() && !response && requiresQuestItemAiCompletion(definition, transition)) {
			return QuestE2eStatus.RUNTIME_REQUIRED;
		}
		if (outcome.stateChanged() && !response && !transition.afterCommit().isEmpty()) {
			return QuestE2eStatus.STATE_CHANGED_WITHOUT_RESPONSE;
		}
		if (runtime.matchedRouteResult() == com.aionemu.gameserver.questEngine.runtime.QuestRouteResult.BLOCKED) {
			return QuestE2eStatus.PASS;
		}
		if (isClick(transition.event()) && !outcome.stateChanged() && !response) return QuestE2eStatus.CLICK_NO_RESPONSE;
		return QuestE2eStatus.PASS;
	}

	private static String routeReason(QuestE2eStatus status, QuestE2eRuntime runtime,
			com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient.DispatchOutcome outcome) {
		if (!runtime.auditEvents().isEmpty()) {
			QuestAuditEvent audit = runtime.auditEvents().getFirst();
			return audit.failureStage() + ":" + audit.actionType() + ":"
				+ audit.rootFailureType() + ":" + audit.rootFailureMessage();
		}
		if (outcome.failure() != null) {
			return outcome.failure().getClass().getSimpleName() + ":" + outcome.failure().getMessage();
		}
		if (status == QuestE2eStatus.NO_ROUTE) return "event index returned no route candidate";
		if (status == QuestE2eStatus.NO_MATCH) return "no transition matched event and prepared source facts";
		if (status == QuestE2eStatus.AMBIGUOUS_ROUTE) {
			QuestTransition actual = runtime.matchedTransition();
			return "alternate transition matched " + actual.sourceNode() + "->" + actual.targetNode();
		}
		return status.name();
	}

	private static QuestE2eAuditRow row(CompiledQuestDefinition definition, QuestTransition transition,
			QuestE2eRuntime runtime, ClientResourceOracle oracle, QuestE2eStatus status, String reason,
			int npcId, int objectId, int dialogId, int shownPage) {
		QuestNode target = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElse(null);
		QuestTransition actual = runtime.matchedTransition();
		String evidence = shownPage > 0 && oracle.pageExists(definition.id(), shownPage) ? "AION_5_8_CLIENT" : "CURRENT_IR";
		return new QuestE2eAuditRow(definition.id(), transition.event().type(), transition.sourceNode(), transition.targetNode(),
			actual == null ? "" : actual.sourceNode(), actual == null ? "" : actual.targetNode(), runtime.transitionMatch(),
			"FAST",
			target == null ? "" : target.projection().status().name(), runtime.state().status().name(),
			runtime.state().packedVariables(), npcId, objectId, dialogId, shownPage, status, reason, evidence,
			runtime.trace().entries());
	}

	private static String reason(QuestE2eStatus status,
			com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient.DispatchOutcome outcome,
			int shownPage, CompiledQuestDefinition definition, QuestTransition transition) {
		if (status == QuestE2eStatus.PASS) return "";
		if (outcome.failure() != null) return outcome.failure().getClass().getSimpleName() + ":" + outcome.failure().getMessage();
		if (status == QuestE2eStatus.PAGE_NOT_IN_CLIENT) return "server page " + shownPage + " is absent from Aion 5.8 client tables";
		if (status == QuestE2eStatus.NO_MATCH) return "no transition matched event and prepared source facts";
		if (status == QuestE2eStatus.RUNTIME_REQUIRED && requiresQuestItemAiCompletion(definition, transition)) {
			return "quest item response is completed by QuestItemNpcAI2 outside the dispatcher";
		}
		return status.name() + " for " + transition.event().type();
	}

	private static boolean requiresInteractionObject(QuestTransition transition) {
		return transition.event() instanceof QuestEvent.TalkToNpc && !transition.afterCommit().isEmpty();
	}

	private static boolean hasObservableNonPacketEffect(QuestTransition transition) {
		return transition.afterCommit().stream().anyMatch(action ->
			!(action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.SyncQuestState)
				&& !(action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.ShowQuestDialog)
				&& !(action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.ShowQuestSelectionDialog)
				&& !(action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.ShowDialogWindow)
				&& !(action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.CloseDialog));
	}

	/** 将已提交事务动作与包、世界副作用统一视为可观察响应。 / Treats committed actions, packets, and world side effects as observable responses. */
	private static boolean hasObservableResponse(
			com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient.DispatchOutcome outcome,
			QuestTransition transition) {
		return !outcome.packets().isEmpty() || !transition.actions().isEmpty()
			|| hasObservableNonPacketEffect(transition);
	}

	/** 识别由交互物 AI 在 dispatcher 返回后打开掉落列表的路由。 / Identifies routes whose item AI opens the drop list after dispatcher return. */
	private static boolean requiresQuestItemAiCompletion(CompiledQuestDefinition definition,
			QuestTransition transition) {
		if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
				|| talk.dialogId() != null && talk.dialogId() != -1
				|| !transition.actions().isEmpty() || !transition.afterCommit().isEmpty()) {
			return false;
		}
		return definition.definition().metadata().drops().stream()
			.anyMatch(drop -> drop.npcId() == talk.npcId());
	}

	private static boolean isClick(QuestEvent event) {
		return event instanceof QuestEvent.TalkToNpc || event instanceof QuestEvent.QuestDialog
			|| event instanceof QuestEvent.UseItem;
	}

	private static int firstPage(List<ServerPacketObservation> packets) {
		return packets.stream().filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW)
			.mapToInt(ServerPacketObservation::dialogId).findFirst().orElse(0);
	}

	private static int npcId(QuestEvent event) {
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.npcId();
			case QuestEvent.KillNpc kill -> kill.npcId();
			case QuestEvent.AttackNpc attack -> attack.npcId();
			case QuestEvent.CanAct canAct -> canAct.templateId();
			default -> 0;
		};
	}

	private static int dialogId(QuestEvent event) {
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId() == null ? 0 : talk.dialogId();
			case QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> 0;
		};
	}

	private static <T extends Throwable> T cause(Throwable failure, Class<T> type) {
		for (Throwable current = failure; current != null; current = current.getCause()) {
			if (type.isInstance(current)) return type.cast(current);
		}
		return null;
	}
}
