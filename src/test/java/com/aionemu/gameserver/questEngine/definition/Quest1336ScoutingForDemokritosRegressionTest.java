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
 * 锁定 1336 的三地点组合进度及旧 handler 遗留状态恢复。
 * Locks quest 1336's three-location combination progress and legacy-handler recovery.
 */
class Quest1336ScoutingForDemokritosRegressionTest {
	private static final int QUEST_ID = 1336;
	private static final int REPORT_NPC_ID = 204006;

	@Test
	void completesTheReportStateForEveryInvestigationOrder() throws Exception {
		CompiledQuestDefinition definition = definition();
		List<List<Integer>> orders = List.of(
			List.of(43, 44, 45),
			List.of(43, 45, 44),
			List.of(44, 43, 45),
			List.of(44, 45, 43),
			List.of(45, 43, 44),
			List.of(45, 44, 43));

		for (List<Integer> order : orders) {
			QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START, 0, Map.of());
			for (int index = 0; index < order.size(); index++) {
				int movieId = order.get(index);
				QuestMutationPlan plan = plan(definition, snapshot, new QuestEvent.MovieEnd(movieId));
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
	void completesQuestAtDemokritosFromRewardState() throws Exception {
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
			new QuestEvent.TalkToNpc(REPORT_NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
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
			.orElseThrow(() -> new AssertionError("no matching quest 1336 route for " + event));
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Objects.requireNonNull(
			Quest1336ScoutingForDemokritosRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1336.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
