package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定 1661 的三地点组合进度及旧 handler 遗留状态恢复。
 * Locks quest 1661's three-location combination progress and legacy-handler recovery.
 */
class Quest1661TheKrallScoutingMissionRegressionTest {
	private static final int QUEST_ID = 1661;
	private static final int REPORT_NPC_ID = 204600;

	private static final String ZONE_A = "LF3_SENSORY_AREA_Q1661_A_210040000";
	private static final String ZONE_B = "LF3_SENSORY_AREA_Q1661_B_210040000";
	private static final String ZONE_C = "LF3_SENSORY_AREA_Q1661_C_210040000";

	@Test
	void completesTheReportStateForEveryInvestigationOrder() throws Exception {
		CompiledQuestDefinition definition = definition();
		List<List<String>> orders = List.of(
			List.of(ZONE_A, ZONE_B, ZONE_C),
			List.of(ZONE_A, ZONE_C, ZONE_B),
			List.of(ZONE_B, ZONE_A, ZONE_C),
			List.of(ZONE_B, ZONE_C, ZONE_A),
			List.of(ZONE_C, ZONE_A, ZONE_B),
			List.of(ZONE_C, ZONE_B, ZONE_A));

		for (List<String> order : orders) {
			QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0, Map.of());
			for (int index = 0; index < order.size(); index++) {
				String zone = order.get(index);
				QuestMutationPlan plan = plan(definition, snapshot, new QuestEvent.EnterZone(zone));
				QuestStatus expectedStatus = index == order.size() - 1 ? QuestStatus.REWARD : QuestStatus.START;
				assertEquals(expectedStatus, plan.nextStatus(), order.toString());
				QuestStateSyncMode expectedSync = index == order.size() - 1
					? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
				assertEquals(new AfterCommitAction.SyncQuestState(expectedSync),
					plan.afterCommit().getFirst(), order.toString());
				snapshot = new QuestSnapshot(7, QUEST_ID, plan.nextStatus(), plan.nextPackedVariables(), Map.of());
			}
			assertEquals(1, snapshot.packedVariables(), order.toString());
		}
	}

	@Test
	void repairsTheLegacyAllLocationsMaskBeforeShowingTheReportPage() throws Exception {
		CompiledQuestDefinition definition = definition();
		for (int var : List.of(112, 1)) {
			QuestSnapshot stuck = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, var, Map.of());

			QuestMutationPlan enterWorld = plan(definition, stuck, new QuestEvent.EnterWorld());
			assertEquals(QuestStatus.REWARD, enterWorld.nextStatus());
			assertEquals(1, enterWorld.nextPackedVariables());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				enterWorld.afterCommit());

			QuestMutationPlan report = plan(definition, stuck,
				new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(QuestStatus.REWARD, report.nextStatus());
			assertEquals(1, report.nextPackedVariables());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())), report.afterCommit());
		}
	}

	@Test
	void completesQuestAtPhyrosFromRewardState() throws Exception {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot reward = new QuestSnapshot(7, QUEST_ID, QuestStatus.REWARD, 1, Map.of());

		QuestMutationPlan talk = plan(definition, reward,
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(QuestStatus.REWARD, talk.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			talk.afterCommit());

		QuestMutationPlan useObject = plan(definition, reward,
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(QuestStatus.REWARD, useObject.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			useObject.afterCommit());

		QuestMutationPlan selectReward = plan(definition, reward,
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, selectReward.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			selectReward.afterCommit());

		QuestMutationPlan complete = plan(definition, reward,
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.SELECTED_QUEST_NOREWARD.id()));
		assertEquals(QuestStatus.COMPLETE, complete.nextStatus());
		assertEquals(1, complete.nextPackedVariables());
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition, QuestSnapshot snapshot,
		QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.map(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition))
			.flatMap(Optional::stream)
			.findFirst()
			.orElseThrow(() -> new AssertionError("no matching quest 1661 route for " + event));
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Objects.requireNonNull(
			Quest1661TheKrallScoutingMissionRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1661.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
