package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.ClientActionRequest;
import com.aionemu.gameserver.questEngine.e2e.client.QuestHeadlessClient;
import com.aionemu.gameserver.questEngine.e2e.client.QuestProtocolLoop;
import com.aionemu.gameserver.questEngine.e2e.client.QuestTrace;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.client.VirtualClientState;
import com.aionemu.gameserver.questEngine.e2e.world.VirtualClock;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eRuntime;
import com.aionemu.gameserver.questEngine.runtime.QuestE2eWorldFixture;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首版端到端工具的核心门禁：客户端 oracle、真实端口、事务顺序、虚拟 tick 和 packet 字段。
 * Core gates for the first end-to-end tooling slice: client oracle, real ports, transaction order, virtual ticks,
 * and packet fields.
 */
class QuestE2eInfrastructureTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");

	@Test
	void clientOracleReadsOnlyAion58PageAndActionTables() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		assertTrue(oracle.pageExists(1913, 1011));
		assertTrue(oracle.actionExists(29));
		assertFalse(oracle.questIds().isEmpty());
	}

	@Test
	void legacyEvidenceOracleReadsRetailContractWithoutSequenceAudit() throws Exception {
		LegacyQuestEvidenceOracle evidence = LegacyQuestEvidenceOracle.load(CLIENT_MAPPING);
		assertNotNull(evidence.contract(1913));
		assertEquals(List.of(203758), evidence.contract(1913).startNpcIds());
	}

	@Test
	void virtualClockExecutesFollowTickWithoutRealThreads() {
		VirtualClock clock = new VirtualClock();
		List<String> calls = new java.util.ArrayList<>();
		clock.schedule(5, () -> calls.add("tick"));
		clock.tick(4);
		assertEquals(List.of(), calls);
		clock.tick(1);
		assertEquals(List.of("tick"), calls);
	}

	@Test
	void realMoviePortProducesTypedObservation() throws Exception {
		VirtualClientState state = new VirtualClientState(1913);
		QuestTrace trace = new QuestTrace();
		try (QuestE2eWorldFixture world = new QuestE2eWorldFixture(state, trace)) {
			QuestMutationPlan plan = new QuestMutationPlan(1913, QuestStatus.START, 0, List.of(), List.of());
			assertTrue(world.moviePort().playMovie(world.snapshot(), plan, 42));
			assertEquals(ServerPacketObservation.Type.PLAY_MOVIE, world.drainPackets().getFirst().type());
		}
	}

	@Test
	void runtimeUsesProductionDispatcherAndFailsClosedOnCommitFailure() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> !java.util.Objects.equals(candidate.sourceNode(), candidate.targetNode()))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestStatus sourceStatus = runtime.state().status();
			runtime.failNextCommit();
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchWorld(transition.event());
			assertTrue(outcome.failed(), outcome::toString);
			assertEquals(sourceStatus, runtime.state().status());
			assertNotNull(outcome.failure());
		}
	}

	@Test
	void focusedProductionTransitionEmitsAuthoritativeDialogObjectAndQuestIds() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc
				&& !candidate.afterCommit().isEmpty())
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchWorld(transition.event());
			assertFalse(outcome.failed(), outcome::toString);
			outcome.packets().stream().filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW)
				.findFirst().ifPresent(packet -> {
					assertTrue(packet.targetObjectId() > 0);
					assertEquals(definition.id(), packet.questId());
				});
		}
	}

	@Test
	void closeDialogPacketDoesNotRequireAnInteractionObject() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc
				&& candidate.afterCommit().stream().anyMatch(
					action -> action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.CloseDialog))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchWorld(transition.event());
			assertFalse(outcome.failed(), outcome::toString);
			assertTrue(outcome.packets().stream().anyMatch(packet ->
				packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW && packet.dialogId() == 0));
			assertTrue(QuestE2ePacketValidator.validate(definition, transition,
				runtime.expectedDialogTargetObjectId(), outcome.packets()).valid());
		}
	}

	@Test
	void useItemDialogPacketUsesTheItemObjectId() throws Exception {
		CompiledQuestDefinition definition = definition(1970);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem
				&& !candidate.afterCommit().isEmpty())
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int expectedObjectId = runtime.expectedDialogTargetObjectId();
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchWorld(transition.event());
			ServerPacketObservation dialog = outcome.packets().stream()
				.filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW && packet.dialogId() > 0)
				.findFirst().orElseThrow();
			assertTrue(expectedObjectId > 0);
			assertEquals(expectedObjectId, dialog.targetObjectId());
			assertTrue(QuestE2ePacketValidator.validate(definition, transition,
				expectedObjectId, outcome.packets()).valid());
		}
	}

	@Test
	void automaticQuestStartDialogsUseTargetlessObjectZeroAfterStateSync() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		assertTargetlessAutomaticStartDialog(10110, QuestEvent.LevelUp.class, oracle);
		assertTargetlessAutomaticStartDialog(10110, QuestEvent.ZoneMissionEnd.class, oracle);
	}

	@Test
	void automaticQuestStartsWithoutPagesOnlySynchronizeState() throws Exception {
		CompiledQuestDefinition definition = definition(1920);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.LevelUp)
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertTrue(outcome.packets().stream()
				.anyMatch(packet -> packet.type() == ServerPacketObservation.Type.QUEST_ACTION));
			assertFalse(outcome.packets().stream()
				.anyMatch(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW));
			assertTrue(QuestE2ePacketValidator.validate(definition, transition, 0, outcome.packets()).valid());
		}
	}

	@Test
	void npcFactionStartUsesProductionLifecycleAfterCommit() throws Exception {
		CompiledQuestDefinition definition = definition(35015);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "unaccepted".equals(candidate.sourceNode()) && "started".equals(candidate.targetNode()))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			assertEquals(ENpcFactionQuestState.NOTING, runtime.world().npcFactionState(2));

			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();

			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(ENpcFactionQuestState.START, runtime.world().npcFactionState(2));
			assertTraceOrder(runtime.trace(), "STATE", "publish:START", "AFTER_COMMIT", "StartNpcFactionQuest",
				"WORLD", "npc-faction-start:2:START");
		}
	}

	@Test
	void npcFactionCompletionUsesProductionLifecycleAfterCommit() throws Exception {
		CompiledQuestDefinition definition = definition(35015);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode()) && "complete".equals(candidate.targetNode()))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			assertEquals(ENpcFactionQuestState.START, runtime.world().npcFactionState(2));

			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();

			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(ENpcFactionQuestState.COMPLETE, runtime.world().npcFactionState(2));
			assertTraceOrder(runtime.trace(), "STATE", "publish:COMPLETE", "AFTER_COMMIT", "CompleteNpcFactionQuest",
				"WORLD", "npc-faction-complete:2:COMPLETE");
		}
	}

	@Test
	void classChangeUsesThePreparedStartingClassAndMutatesTheLightweightPlayer() throws Exception {
		CompiledQuestDefinition definition = definition(1006);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.afterCommit().stream().anyMatch(action ->
				action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.SetPlayerClass setClass
					&& setClass.playerClass() == PlayerClass.GLADIATOR))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			assertEquals(PlayerClass.WARRIOR, runtime.world().player().getPlayerClass());

			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();

			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(PlayerClass.GLADIATOR, runtime.world().player().getPlayerClass());
			assertTrue(runtime.trace().entries().stream().anyMatch(entry ->
				"WORLD".equals(entry.phase()) && "player-class:WARRIOR:GLADIATOR".equals(entry.detail())));
		}
	}

	@Test
	void effectOperationsUseTheRealPlayerEffectBoundaryWithInMemoryState() throws Exception {
		CompiledQuestDefinition definition = definition(14114);
		QuestTransition apply = definition.definition().transitions().stream()
			.filter(candidate -> candidate.afterCommit().stream().anyMatch(
				com.aionemu.gameserver.questEngine.definition.AfterCommitAction.ApplyEffect.class::isInstance))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(apply);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertFalse(outcome.failed(), outcome::toString);
			assertTrue(runtime.world().hasEffect(8197));
			assertTrue(runtime.trace().entries().stream().anyMatch(entry ->
				"WORLD".equals(entry.phase()) && "effect-apply:8197:0".equals(entry.detail())));
		}

		QuestTransition remove = definition.definition().transitions().stream()
			.filter(candidate -> candidate.afterCommit().stream().anyMatch(
				com.aionemu.gameserver.questEngine.definition.AfterCommitAction.RemoveEffect.class::isInstance))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(remove);
			assertTrue(runtime.world().hasEffect(8197));
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertFalse(outcome.failed(), outcome::toString);
			assertFalse(runtime.world().hasEffect(8197));
			assertTrue(runtime.trace().entries().stream().anyMatch(entry ->
				"WORLD".equals(entry.phase()) && "effect-remove:8197".equals(entry.detail())));
		}
	}

	@Test
	void playerEmotionUsesTheAuthoritativeInteractionObjectThroughTheRealPort() throws Exception {
		CompiledQuestDefinition definition = definition(1004);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.afterCommit().stream().anyMatch(
				com.aionemu.gameserver.questEngine.definition.AfterCommitAction.PlayerEmotion.class::isInstance))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int objectId = runtime.expectedDialogTargetObjectId();
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertFalse(outcome.failed(), outcome::toString);
			assertTrue(runtime.auditEvents().isEmpty(), runtime.auditEvents()::toString);
			assertTrue(runtime.trace().entries().stream().anyMatch(entry ->
				"PACKET".equals(entry.phase()) && ("player-emotion:STAND:" + objectId).equals(entry.detail())),
				runtime.trace().entries()::toString);
			assertTrue(outcome.packets().stream().anyMatch(packet -> packet.type() == ServerPacketObservation.Type.OTHER
				&& "SM_EMOTION".equals(packet.detail())));
		}
	}

	@Test
	void rawSystemMessageUsesTheRealPlayerPacketPort() throws Exception {
		CompiledQuestDefinition definition = definition(10521);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "s12".equals(candidate.sourceNode()) && "s13".equals(candidate.targetNode()))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertFalse(outcome.failed(), outcome::toString);
			assertTrue(runtime.trace().entries().stream().anyMatch(entry ->
				"PACKET".equals(entry.phase()) && "system-packet:1403364".equals(entry.detail())));
			assertTrue(outcome.packets().stream().anyMatch(packet -> packet.type() == ServerPacketObservation.Type.OTHER
				&& "SM_SYSTEM_MESSAGE".equals(packet.detail())));
		}
	}

	@Test
	void canActSelfLoopWithoutPacketsIsNotAClientClickFailure() throws Exception {
		CompiledQuestDefinition definition = definition(11036);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.CanAct)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch());
	}

	@Test
	void variableConditionScenarioExercisesTheExpectedCompletionTransition() throws Exception {
		CompiledQuestDefinition definition = definition(1842);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "m1".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode())
				&& candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.KillNpc)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertEquals("reward", row.matchedTargetNode());
	}

	@Test
	void runtimeAttributesAConclusiveAlternateTransition() throws Exception {
		CompiledQuestDefinition definition = definition(1842);
		QuestTransition expected = definition.definition().transitions().stream()
			.filter(candidate -> "m1".equals(candidate.sourceNode()) && "m1".equals(candidate.targetNode())
				&& candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.KillNpc)
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(expected);
			runtime.state().project(QuestStatus.START, 208);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchWorld(expected.event());
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(QuestE2eTransitionMatch.ALTERNATE_TRANSITION_MATCHED, runtime.transitionMatch());
			assertEquals("reward", runtime.matchedTransition().targetNode());
		}
	}

	@Test
	void blockDefaultItemUseIsAConclusiveResponse() throws Exception {
		CompiledQuestDefinition definition = definition(11036);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode())
				&& candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem
				&& candidate.actions().stream().anyMatch(
					action -> action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.BlockDefaultItemUse))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertEquals("CM_USE_ITEM", row.validationMode());
	}

	@Test
	void protocolUpgradeClearsUseItemClickFailureWhenInventoryChanges() throws Exception {
		CompiledQuestDefinition definition = definition(2333);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem
				&& candidate.actions().stream().anyMatch(
					action -> action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.GiveItem))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals("CM_USE_ITEM", row.validationMode());
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
	}

	@Test
	void worldSideEffectWithoutPacketIsAConclusiveResponse() throws Exception {
		CompiledQuestDefinition definition = definition(1002);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "s20".equals(candidate.sourceNode()) && "s20".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 31)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertTrue(row.trace().stream().anyMatch(entry ->
			"WORLD".equals(entry.phase()) && entry.detail().equals("flight:1001")));
	}

	@Test
	void scheduledRefreshWithoutPacketIsAConclusiveResponse() throws Exception {
		CompiledQuestDefinition definition = definition(80030);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem)
			.filter(candidate -> !candidate.afterCommit().isEmpty())
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertTrue(row.trace().stream().anyMatch(entry ->
			"CLOCK".equals(entry.phase()) && entry.detail().equals("refresh:10")));
	}

	@Test
	void negativeDialogSentinelIsNotEscalatedThroughUnsignedCmDialogSelect() throws Exception {
		CompiledQuestDefinition definition = definition(1004);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == -1)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals("FAST", row.validationMode());
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertFalse(row.status() == QuestE2eStatus.INVALID_DIALOG_PACKET, row::toString);
	}

	@Test
	void negativeDialogTransactionActionIsAnObservableResponse() throws Exception {
		CompiledQuestDefinition definition = definition(1170);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == -1)
			.filter(candidate -> candidate.actions().stream().anyMatch(
				action -> action instanceof com.aionemu.gameserver.questEngine.definition.QuestAction.GiveItem))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		assertEquals("FAST", row.validationMode());
	}

	@Test
	void wildcardQuestItemDropRouteRequiresTheAiCompletionPipeline() throws Exception {
		CompiledQuestDefinition definition = definition(1103);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == null)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eStatus.RUNTIME_REQUIRED, row.status(), row::toString);
		assertEquals("quest item response is completed by QuestItemNpcAI2 outside the dispatcher", row.reason());
	}

	@Test
	void negativeQuestItemDropRouteRequiresTheAiCompletionPipeline() throws Exception {
		CompiledQuestDefinition definition = definition(2213);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == -1)
			.filter(candidate -> candidate.actions().isEmpty() && candidate.afterCommit().isEmpty())
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eStatus.RUNTIME_REQUIRED, row.status(), row::toString);
		assertEquals("FAST", row.validationMode());
	}

	@Test
	void killNpcSetIsMaterializedAsOneAuthoritativeKillEvent() throws Exception {
		CompiledQuestDefinition definition = definition(1842);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.KillNpcSet)
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
	}

	@Test
	void completionSyncUsesTheLightweightPlayerAccount() throws Exception {
		CompiledQuestDefinition definition = definition(1730);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode()) && "complete".equals(candidate.targetNode()))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
	}

	@Test
	void attackNpcScenarioCarriesAuthorityForLuredNpcWatch() throws Exception {
		CompiledQuestDefinition definition = definition(1157);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.AttackNpc
				&& candidate.afterCommit().stream().anyMatch(action ->
					action instanceof com.aionemu.gameserver.questEngine.definition.AfterCommitAction.WatchLuredNpcCoordinate))
			.findFirst().orElseThrow();
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED, row.transitionMatch(), row::toString);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
	}

	@Test
	void equipmentMembershipAndDpConditionsUseCapturedScenarioFacts() throws Exception {
		CompiledQuestDefinition equippedItems = definition(1647);
		assertPreparedTransitionMatches(equippedItems, transitionWithCondition(equippedItems,
			QuestCondition.EquippedItem.class, ignored -> true));

		CompiledQuestDefinition equipmentSets = definition(1990);
		assertPreparedTransitionMatches(equipmentSets, transitionWithCondition(equipmentSets,
			QuestCondition.EquipmentSetEquipped.class, QuestCondition.EquipmentSetEquipped::expected));
		assertPreparedTransitionMatches(equipmentSets, transitionWithCondition(equipmentSets,
			QuestCondition.EquipmentSetEquipped.class, condition -> !condition.expected()));
		assertPreparedTransitionMatches(equipmentSets, transitionWithCondition(equipmentSets,
			QuestCondition.DpAtMax.class, ignored -> true));

		CompiledQuestDefinition membership = definition(1929);
		assertPreparedTransitionMatches(membership, transitionWithCondition(membership,
			QuestCondition.MembershipPermission.class, QuestCondition.MembershipPermission::expected));
		assertPreparedTransitionMatches(membership, transitionWithCondition(membership,
			QuestCondition.MembershipPermission.class, condition -> !condition.expected()));
	}

	@Test
	void recipeAndCraftSkillConditionsUseCapturedScenarioFacts() throws Exception {
		CompiledQuestDefinition recipes = definition(19038);
		assertPreparedTransitionMatches(recipes, transitionWithCondition(recipes,
			QuestCondition.RecipeKnown.class, condition -> !condition.expected()));

		CompiledQuestDefinition craftSkill = definition(1941);
		assertPreparedTransitionMatches(craftSkill, transitionWithCondition(craftSkill,
			QuestCondition.CanGrantCraftSkill.class, ignored -> true));
	}

	@Test
	void npcHpConditionUsesAValueStrictlyBelowTheThreshold() throws Exception {
		CompiledQuestDefinition definition = definition(1006);
		assertPreparedTransitionMatches(definition, transitionWithCondition(definition,
			QuestCondition.NpcHpBelowPercent.class, ignored -> true));
	}

	@Test
	void pvpConditionsCarryConcreteServerFactsForRankAndWildcardWorldRoutes() throws Exception {
		CompiledQuestDefinition ranked = definition(3741);
		assertPreparedTransitionMatches(ranked, ranked.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillRanked)
			.findFirst().orElseThrow());

		CompiledQuestDefinition wildcardWorld = definition(19690);
		assertPreparedTransitionMatches(wildcardWorld, wildcardWorld.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillInWorld kill && kill.worldId() == 0)
			.findFirst().orElseThrow());

		CompiledQuestDefinition recipientZone = QuestDsl.quest(990_002)
			.node("started", QuestDsl.project(QuestStatus.START, Map.of()))
			.node("reward", QuestDsl.project(QuestStatus.REWARD, Map.of()))
			.on(QuestDsl.killInWorld(400010000)).from("started")
			.when(QuestDsl.pvpVictimLevelDelta(-5, 9))
			.when(QuestDsl.pvpRecipientInZone("SULFUR_FORTRESS_400010000"))
			.goTo("reward").compile();
		assertPreparedTransitionMatches(recipientZone, recipientZone.definition().transitions().getFirst());
	}

	@Test
	void completeCountConditionsPrepareBothExpectedBranches() throws Exception {
		var builder = QuestDsl.quest(990_001)
			.progress(QuestDsl.bitField("branch", 0, 1,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT))
			.node("started", QuestDsl.project(QuestStatus.START, QuestDsl.vars("branch", 0)))
			.node("ninth", QuestDsl.project(QuestStatus.REWARD, QuestDsl.vars("branch", 1)))
			.node("other", QuestDsl.project(QuestStatus.REWARD, QuestDsl.vars("branch", 0)));
		builder.on(QuestDsl.bonusApply("COMPLETE_COUNT")).from("started")
			.when(QuestDsl.completeCountIs(9)).goTo("ninth");
		builder.on(QuestDsl.bonusApply("COMPLETE_COUNT")).from("started")
			.when(QuestDsl.completeCountIs(9, false)).goTo("other");
		CompiledQuestDefinition definition = builder.compile();

		for (QuestTransition transition : definition.definition().transitions()) {
			assertPreparedTransitionMatches(definition, transition);
		}
	}

	@Test
	void sourceLessTransitionsChooseAProjectionFromTheirStatusAndVariableConditions() throws Exception {
		CompiledQuestDefinition definition = definition(1929);
		QuestTransition levelUp = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode() == null)
			.filter(transition -> transition.event() instanceof QuestEvent.LevelUp)
			.findFirst().orElseThrow();
		assertPreparedTransitionMatches(definition, levelUp);

		QuestTransition deathRecovery = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode() == null)
			.filter(transition -> transition.event() instanceof QuestEvent.Die)
			.filter(transition -> transition.conditions().stream().anyMatch(condition ->
				condition instanceof QuestCondition.AdvancedClassIs playerClass
					&& playerClass.playerClass() == PlayerClass.GLADIATOR))
			.findFirst().orElseThrow();
		assertPreparedTransitionMatches(definition, deathRecovery);
	}

	@Test
	void unreachableCounterContinuationIsAttributedToItsExecutableSibling() throws Exception {
		CompiledQuestDefinition definition = definition(3118);
		QuestTransition unreachable = definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode()))
			.filter(transition -> "started".equals(transition.targetNode()))
			.filter(transition -> transition.conditions().stream()
				.anyMatch(QuestCondition.VariableBelow.class::isInstance))
			.findFirst().orElseThrow();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(unreachable);
			assertFalse(runtime.unsupportedFacts(), unreachable::toString);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(QuestE2eTransitionMatch.ALTERNATE_TRANSITION_MATCHED,
				runtime.transitionMatch(), unreachable::toString);
			assertEquals("reward", runtime.matchedTransition().targetNode());
		}
	}

	@Test
	void protocolLoopExecutesRealDialogPacketAndRestoresEngineProvider() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 31 && !candidate.afterCommit().isEmpty())
			.findFirst().orElseThrow();
		com.aionemu.gameserver.questEngine.QuestEngine previous =
			com.aionemu.gameserver.lifecycle.GameEngineServices.questEngine();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			int objectId = runtime.state().currentObjectId();
			int npcId = ((com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc) transition.event()).npcId();
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(
					ClientActionRequest.dialog(definition.id(), npcId, objectId, 31));
				assertTrue(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				ServerPacketObservation dialog = outcome.packets().stream()
					.filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW)
					.findFirst().orElseThrow();
				assertEquals(objectId, dialog.targetObjectId());
				assertEquals(definition.id(), dialog.questId());
			}
		}
		assertSame(previous, com.aionemu.gameserver.lifecycle.GameEngineServices.questEngine());
	}

	@Test
	void protocolLoopExecutesRealUseItemPacket() throws Exception {
		CompiledQuestDefinition definition = definition(1970);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem)
			.findFirst().orElseThrow();
		var useItem = (com.aionemu.gameserver.questEngine.definition.QuestEvent.UseItem) transition.event();
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			try (QuestProtocolLoop protocol = new QuestProtocolLoop(runtime)) {
				QuestHeadlessClient.DispatchOutcome outcome = protocol.dispatch(
					ClientActionRequest.useItem(definition.id(), useItem.itemId(), 880_001));
				assertTrue(outcome.handled(), outcome::toString);
				assertFalse(outcome.failed(), outcome::toString);
				assertTrue(outcome.packets().stream().anyMatch(packet ->
					packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW
						&& packet.questId() == definition.id()));
			}
		}
	}

	@Test
	void batchAuditProducesRowsWithoutUsingSequenceAuditInput() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		CompiledQuestDefinition definition = definition(1913);
		var rows = QuestE2eBatchAudit.auditTransition(definition, definition.definition().transitions().getFirst(), oracle);
		assertEquals(definition.id(), rows.questId());
		assertNotNull(rows.status());
	}

	private static <T extends QuestCondition> QuestTransition transitionWithCondition(
			CompiledQuestDefinition definition, Class<T> conditionType,
			java.util.function.Predicate<T> selector) {
		return definition.definition().transitions().stream()
			.filter(transition -> transition.conditions().stream()
				.filter(conditionType::isInstance)
				.map(conditionType::cast)
				.anyMatch(selector))
			.findFirst().orElseThrow();
	}

	private static void assertPreparedTransitionMatches(CompiledQuestDefinition definition,
			QuestTransition transition) throws Exception {
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			assertFalse(runtime.unsupportedFacts(), transition::toString);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), () -> outcome + " audit=" + runtime.auditEvents());
			assertEquals(QuestE2eTransitionMatch.EXPECTED_TRANSITION_MATCHED,
				runtime.transitionMatch(), transition::toString);
		}
	}

	private static void assertTargetlessAutomaticStartDialog(int questId,
			Class<? extends QuestEvent> eventType, ClientResourceOracle oracle) throws Exception {
		CompiledQuestDefinition definition = definition(questId);
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
			.filter(candidate -> eventType.isInstance(candidate.event()))
			.findFirst().orElseThrow();
		QuestE2eAuditRow row = QuestE2eBatchAudit.auditTransition(definition, transition, oracle);
		assertEquals(QuestE2eStatus.PASS, row.status(), row::toString);
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(transition);
			QuestHeadlessClient.DispatchOutcome outcome = runtime.dispatchPrepared();
			assertTrue(outcome.handled(), outcome::toString);
			assertFalse(outcome.failed(), outcome::toString);
			assertEquals(QuestStatus.START, runtime.state().status());
			ServerPacketObservation dialog = outcome.packets().stream()
				.filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW)
				.findFirst().orElseThrow();
			assertEquals(0, dialog.targetObjectId());
			assertEquals(questId, dialog.questId());
			assertTrue(QuestE2ePacketValidator.validate(definition, transition, 0, outcome.packets()).valid());
			List<ServerPacketObservation.Type> packetTypes = outcome.packets().stream()
				.map(ServerPacketObservation::type).toList();
			int syncIndex = packetTypes.indexOf(ServerPacketObservation.Type.QUEST_ACTION);
			int pageIndex = packetTypes.indexOf(ServerPacketObservation.Type.DIALOG_WINDOW);
			assertTrue(syncIndex >= 0 && pageIndex > syncIndex, packetTypes::toString);
		}
	}

	private static void assertTraceOrder(QuestTrace trace, String firstPhase, String firstDetail,
			String secondPhase, String secondDetail, String thirdPhase, String thirdDetail) {
		List<QuestTrace.Entry> entries = trace.entries();
		int first = traceIndex(entries, firstPhase, firstDetail);
		int second = traceIndex(entries, secondPhase, secondDetail);
		int third = traceIndex(entries, thirdPhase, thirdDetail);
		assertTrue(first >= 0 && second > first && third > second, entries::toString);
	}

	private static int traceIndex(List<QuestTrace.Entry> entries, String phase, String detail) {
		for (int index = 0; index < entries.size(); index++) {
			QuestTrace.Entry entry = entries.get(index);
			if (phase.equals(entry.phase()) && detail.equals(entry.detail())) {
				return index;
			}
		}
		return -1;
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestE2eInfrastructureTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) throw new IllegalStateException("missing quest resource " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
