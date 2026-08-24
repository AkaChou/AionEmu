package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.client.ServerPacketObservation;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestJourneyRunner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 以任务 1913、14047 的 Aion 5.8 客户端验收路径锁定持续无头会话、
 * 真实按钮和完整奖励合同。
 * Locks persistent headless sessions, real button requests, and complete reward contracts to the Aion 5.8 client
 * acceptance journeys for quests 1913 and 14047.
 */
class QuestGoldenJourneyTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static ClientResourceOracle oracle;

	@BeforeAll
	static void loadClientEvidence() throws Exception {
		oracle = ClientResourceOracle.load(CLIENT_MAPPING);
	}

	@Test
	void quest1913CompletesThroughOneContinuousClientJourney() throws Exception {
		CompiledQuestDefinition definition = definition(1913);
		QuestTransition ingress = talk(definition, "unaccepted", 203758, 31);
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, ingress, oracle)) {
			assertStep(journey.interact(203758, 31), QuestStatus.NONE, 0, 1011);
			assertStep(journey.clickVisibleAction(1012), QuestStatus.NONE, 0, 1012);
			assertStep(journey.clickVisibleAction(1007), QuestStatus.NONE, 0, 4);
			assertStep(journey.clickVisibleAction(1002), QuestStatus.START, 0, 1003);
			assertStep(journey.clickVisibleAction(1008), QuestStatus.START, 0, 10);

			assertStep(journey.interact(203726, 31), QuestStatus.START, 0, 1352);
			QuestJourneyRunner.Step teleport = assertStep(journey.clickVisibleAction(10000),
				QuestStatus.START, 1, 0);
			assertTraceOrder(teleport, "STATE", "publish:START", "WORLD", "teleport:210030000:1");

			assertStep(journey.interact(203097, 31), QuestStatus.START, 1, 2375);
			assertStep(journey.clickVisibleAction(1009), QuestStatus.REWARD, 1, 5);
			QuestJourneyRunner.Step completion = assertStep(journey.clickNativeAction(8),
				QuestStatus.COMPLETE, 0, 10);

			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 14046, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 160001273, 5, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.committedActions());
			assertSame(ingress, journey.preparedTransition());
			assertPacketsObservedOnce(journey);
		}
	}

	@Test
	void quest14047CompletesThroughOneContinuousClientJourney() throws Exception {
		CompiledQuestDefinition definition = definition(14047);
		QuestTransition ingress = transition(definition, "unaccepted", QuestEvent.LevelUp.class);
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, ingress, oracle)) {
			assertStep(journey.emitWorldEvent(new QuestEvent.LevelUp()), QuestStatus.START, 0, 0);
			complete14047Prelude(journey);
			complete14047InstanceAndReward(journey);
			assertSame(ingress, journey.preparedTransition());
			assertPacketsObservedOnce(journey);
		}
	}

	@Test
	void quest14047RelogRecoveryRebuildsBothFlightStagesAndStillCompletes() throws Exception {
		CompiledQuestDefinition definition = definition(14047);
		QuestTransition ingress = transition(definition, "unaccepted", QuestEvent.LevelUp.class);
		try (QuestJourneyRunner journey = new QuestJourneyRunner(definition, ingress, oracle)) {
			assertStep(journey.emitWorldEvent(new QuestEvent.LevelUp()), QuestStatus.START, 0, 0);
			complete14047Prelude(journey);
			advanceFirstFlight(journey);
			advanceSecondFlight(journey);

			assertStep(journey.emitWorldEvent(new QuestEvent.EnterWorld()), QuestStatus.START, 3, 0);
			advanceFirstFlight(journey);
			advanceSecondFlight(journey);
			finish14047AfterFlights(journey);
			assertSame(ingress, journey.preparedTransition());
			assertPacketsObservedOnce(journey);
		}
	}

	private static void complete14047Prelude(QuestJourneyRunner journey) {
		QuestJourneyRunner.Step firstNpc = assertStep(journey.interact(203704, 31),
			QuestStatus.START, 0, 1011);
		assertDialogTarget(firstNpc);
		assertStep(journey.clickVisibleAction(1012), QuestStatus.START, 0, 1012);
		assertStep(journey.clickVisibleAction(10000), QuestStatus.START, 1, 0);

		assertStep(journey.interact(798154, 31), QuestStatus.START, 1, 1352);
		assertStep(journey.clickVisibleAction(1353), QuestStatus.START, 1, 1353);
		assertStep(journey.clickVisibleAction(10001), QuestStatus.START, 2, 0);

		assertStep(journey.interact(204574, 31), QuestStatus.START, 2, 1693);
		assertStep(journey.clickVisibleAction(1694), QuestStatus.START, 2, 1694);
		assertStep(journey.clickVisibleAction(10002), QuestStatus.START, 3, 0);
	}

	private static void complete14047InstanceAndReward(QuestJourneyRunner journey) {
		advanceFirstFlight(journey);
		advanceSecondFlight(journey);
		finish14047AfterFlights(journey);
	}

	private static void advanceFirstFlight(QuestJourneyRunner journey) {
		assertStep(journey.interact(802051, 31), QuestStatus.START, 3, 2034);
		assertStep(journey.clickVisibleAction(2035), QuestStatus.START, 3, 2035);
		QuestJourneyRunner.Step flight = assertStep(journey.clickVisibleAction(10009),
			QuestStatus.START, 4, 0);
		assertPacketOrder(flight, ServerPacketObservation.Type.QUEST_ACTION,
			ServerPacketObservation.Type.DIALOG_WINDOW);
		assertTraceOrder(flight, "STATE", "publish:START", "WORLD", "flight:71001");
	}

	private static void advanceSecondFlight(QuestJourneyRunner journey) {
		assertStep(journey.interact(802052, 31), QuestStatus.START, 4, 2375);
		QuestJourneyRunner.Step movie = assertStep(journey.clickVisibleAction(2376),
			QuestStatus.START, 4, 2376);
		assertPacketOrder(movie, ServerPacketObservation.Type.PLAY_MOVIE,
			ServerPacketObservation.Type.DIALOG_WINDOW);
		QuestJourneyRunner.Step flight = assertStep(journey.clickVisibleAction(10010),
			QuestStatus.START, 5, 0);
		assertPacketOrder(flight, ServerPacketObservation.Type.QUEST_ACTION,
			ServerPacketObservation.Type.DIALOG_WINDOW);
		assertTraceOrder(flight, "STATE", "publish:START", "WORLD", "flight:72001");
	}

	private static void finish14047AfterFlights(QuestJourneyRunner journey) {
		QuestJourneyRunner.Step kill = assertStep(journey.emitWorldEvent(new QuestEvent.KillNpc(214599)),
			QuestStatus.START, 6, 0);
		assertPacketOrder(kill, ServerPacketObservation.Type.QUEST_ACTION,
			ServerPacketObservation.Type.PLAY_MOVIE);

		assertStep(journey.interact(802051, 31), QuestStatus.START, 6, 3057);
		assertStep(journey.clickVisibleAction(3058), QuestStatus.START, 6, 3058);
		assertStep(journey.clickVisibleAction(10255), QuestStatus.REWARD, 6, 0);
		assertStep(journey.useObject(278500), QuestStatus.REWARD, 6, 5);
		QuestJourneyRunner.Step completion = assertStep(journey.clickNativeAction(8),
			QuestStatus.COMPLETE, 6, 10);
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 8325278, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("AP", 0, 3000, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("TITLE", 46, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 187000035, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 186000469, 20, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.committedActions());
	}

	private static QuestJourneyRunner.Step assertStep(QuestJourneyRunner.Step step, QuestStatus status,
			int var0, int page) {
		assertTrue(step.outcome().handled(), step::toString);
		assertFalse(step.outcome().failed(), () -> step + " failure=" + step.outcome().failure());
		assertEquals(status, step.status(), step::toString);
		assertEquals(page, step.page(), step::toString);
		assertEquals(var0, unpackVar0(step.packedVariables()), step::toString);
		return step;
	}

	private static int unpackVar0(int packedVariables) {
		return packedVariables & 0b111;
	}

	private static void assertDialogTarget(QuestJourneyRunner.Step step) {
		ServerPacketObservation dialog = step.outcome().packets().stream()
			.filter(packet -> packet.type() == ServerPacketObservation.Type.DIALOG_WINDOW && packet.dialogId() > 0)
			.findFirst().orElseThrow();
		assertEquals(step.objectId(), dialog.targetObjectId(), step::toString);
	}

	private static void assertPacketsObservedOnce(QuestJourneyRunner journey) {
		int emitted = journey.steps().stream().mapToInt(step -> step.outcome().packets().size()).sum();
		assertEquals(emitted, journey.observedPacketCount());
	}

	private static void assertPacketOrder(QuestJourneyRunner.Step step, ServerPacketObservation.Type first,
			ServerPacketObservation.Type second) {
		List<ServerPacketObservation.Type> types = step.outcome().packets().stream()
			.map(ServerPacketObservation::type).toList();
		assertTrue(types.indexOf(first) >= 0 && types.indexOf(second) > types.indexOf(first), types::toString);
	}

	private static void assertTraceOrder(QuestJourneyRunner.Step step, String firstPhase, String firstDetail,
			String secondPhase, String secondDetail) {
		int first = traceIndex(step, firstPhase, firstDetail);
		int second = traceIndex(step, secondPhase, secondDetail);
		assertTrue(first >= 0 && second > first, step.trace()::toString);
	}

	private static int traceIndex(QuestJourneyRunner.Step step, String phase, String detail) {
		for (int index = 0; index < step.trace().size(); index++) {
			var entry = step.trace().get(index);
			if (phase.equals(entry.phase()) && detail.equals(entry.detail())) {
				return index;
			}
		}
		return -1;
	}

	private static QuestTransition talk(CompiledQuestDefinition definition, String source, int npcId, int actionId) {
		return definition.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(actionId).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(CompiledQuestDefinition definition, String source,
			Class<? extends QuestEvent> eventType) {
		return definition.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && eventType.isInstance(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestGoldenJourneyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
