package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 1470 击杀两种克罗梅内模板后的哈克内报告对话合同。
 * Locks quest 1470's Hannet report-dialog contract after either Kromede template is killed.
 */
class Quest1470ClientDialogAlignmentTest {
	private static final Path QUEST_PATH = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1470.xml");
	private static final int HANNET_NPC_ID = 790004;
	private static final List<Integer> KROMEDE_NPC_IDS = List.of(212846, 214621);

	@Test
	void opensTheReportPageAfterEitherKromedeTemplateAndKeepsTheRewardContract() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)), node(definition, "k1").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 1)), node(definition, "reward").projection());

		for (int npcId : KROMEDE_NPC_IDS) {
			QuestTransition kill = transition(definition, "started", "k1", new QuestEvent.KillNpc(npcId));
			assertEquals(List.of(), kill.conditions());
			assertEquals(List.of(), kill.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				kill.afterCommit());
		}

		QuestTransition reportPage = transition(definition, "k1", "k1",
			new QuestEvent.TalkToNpc(HANNET_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), reportPage.conditions());
		assertEquals(List.of(), reportPage.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		int packed = definition.progressLayout().pack(Map.of("var0", 1));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1470, QuestStatus.START, packed, Map.of());
		QuestMutationPlan reportPagePlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(HANNET_NPC_ID, QuestDialogAction.QUEST_SELECT.id()), reportPage).orElseThrow();
		assertEquals(QuestStatus.START, reportPagePlan.nextStatus());
		assertEquals(Map.of("var0", 1), definition.progressLayout().unpack(reportPagePlan.nextPackedVariables()));

		QuestTransition report = transition(definition, "k1", "reward",
			new QuestEvent.TalkToNpc(HANNET_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestMutationPlan reportPlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(HANNET_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()), report).orElseThrow();
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
		assertEquals(Map.of("var0", 1), definition.progressLayout().unpack(reportPlan.nextPackedVariables()));

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(HANNET_NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 1244918, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("TITLE", 18, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 186000003, 40, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 162000050, 20, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 111101657, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode())
				&& target.equals(candidate.targetNode()) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_PATH)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
