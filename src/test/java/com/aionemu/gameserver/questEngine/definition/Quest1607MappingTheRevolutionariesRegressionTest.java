package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定 1607 的四地点组合进度及旧 handler 遗留状态恢复。
 * Locks quest 1607's four-location combination progress and legacy-handler recovery.
 */
class Quest1607MappingTheRevolutionariesRegressionTest {
	private static final int QUEST_ID = 1607;
	private static final int KUOBE_NPC_ID = 204578;
	private static final int REPORT_NPC_ID = 204574;

	private static final String ZONE_A = "LF3_SENSORY_AREA_Q1607_A_210040000";
	private static final String ZONE_B = "LF3_SENSORY_AREA_Q1607_B_210040000";
	private static final String ZONE_C = "LF3_SENSORY_AREA_Q1607_C_210040000";
	private static final String ZONE_D = "LF3_SENSORY_AREA_Q1607_D_210040000";

	@Test
	void advancesFromKuobeDialogToScoutingPhase() throws Exception {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot started = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0, Map.of());

		QuestMutationPlan setpro = plan(definition, started,
			new QuestEvent.TalkToNpc(KUOBE_NPC_ID, QuestDialogAction.SETPRO1.id()));
		assertEquals(QuestStatus.START, setpro.nextStatus());
		assertEquals(1, setpro.nextPackedVariables());
	}

	@Test
	void completesTheReportStateForEveryInvestigationOrder() throws Exception {
		CompiledQuestDefinition definition = definition();
		List<String> zones = List.of(ZONE_A, ZONE_B, ZONE_C, ZONE_D);
		List<List<String>> permutations = new ArrayList<>();
		generatePermutations(zones, 0, permutations);

		assertEquals(24, permutations.size());

		for (List<String> order : permutations) {
			QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 1, Map.of());
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
	void repairsTheLegacyStateBeforeShowingTheReportPage() throws Exception {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot stuck = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 5, Map.of());

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
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())), report.afterCommit());
	}

	@Test
	void completesQuestAtKalendrosFromRewardState() throws Exception {
		CompiledQuestDefinition definition = definition();
		QuestSnapshot reward = new QuestSnapshot(7, QUEST_ID, QuestStatus.REWARD, 1, Map.of());

		QuestMutationPlan talk = plan(definition, reward,
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(QuestStatus.REWARD, talk.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			talk.afterCommit());

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

	private static void generatePermutations(List<String> list, int k, List<List<String>> result) {
		for (int i = k; i < list.size(); i++) {
			java.util.Collections.swap(list, i, k);
			generatePermutations(list, k + 1, result);
			java.util.Collections.swap(list, k, i);
		}
		if (k == list.size() - 1) {
			result.add(new ArrayList<>(list));
		}
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition, QuestSnapshot snapshot,
		QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.map(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition))
			.flatMap(Optional::stream)
			.findFirst()
			.orElseThrow(() -> new AssertionError("no matching quest 1607 route for " + event));
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Objects.requireNonNull(
			Quest1607MappingTheRevolutionariesRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1607.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
