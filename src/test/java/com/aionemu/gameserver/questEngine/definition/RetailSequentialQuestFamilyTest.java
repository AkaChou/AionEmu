package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks retail sequential dialogs and kill counters for quests 15321, 15590, and 25590.
 */
class RetailSequentialQuestFamilyTest {

	@Test
	void quest15321MatchesRetailTalkHuntSequence() throws Exception {
		QuestDefinition definition = definition(15321);
		assertNode(definition, "s0", 0);
		assertNode(definition, "s1", 1);
		assertNode(definition, "s2", 2);
		assertNode(definition, "s3", 3);
		assertNode(definition, "s4", 4);
		assertNode(definition, "s5", 5);
		assertNode(definition, "s6", 6);
		assertNode(definition, "s7", 7);
		assertNode(definition, "s8", 8);
		assertNode(definition, "s9", 9);
		assertNode(definition, "s10", 10);
		assertNode(definition, "s11", 11);
		assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status());
		assertEquals(Map.of("var0", 12, "var1", 0), node(definition, "reward").projection().variables());
		assertSimpleStart(definition, 805330, "s0");

		assertTalkChain(definition, "s0", "s1", 805332, "SELECT1", "SELECT1_1", "SETPRO1");
		assertTalkChain(definition, "s2", "s3", 805333, "SELECT3", "SELECT3_1", "SETPRO3");
		assertTalkChain(definition, "s4", "s5", 805334, "SELECT5", "SELECT5_1", "SETPRO5");
		assertTalkChain(definition, "s6", "s7", 805335, "SELECT7", "SELECT7_1", "SETPRO7");
		assertTalkChain(definition, "s8", "s9", 805336, "SELECT9", "SELECT9_1", "SETPRO9");
		assertTalkChain(definition, "s10", "s11", 805337, "SELECT11", "SELECT11_1", "SETPRO11");

		assertKillStep(definition, "s1", "s2", 30, 2);
		assertKillStep(definition, "s3", "s4", 30, 4);
		assertKillStep(definition, "s5", "s6", 10, 6);
		assertKillStep(definition, "s7", "s8", 30, 8);
		assertKillStep(definition, "s9", "s10", 30, 10);
		assertKillStep(definition, "s11", "reward", 30, 12);

		assertRewardContract(definition, 805330, "0 1");
		assertTrue(definition.transitions().stream()
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == 805330));
	}

	@Test
	void dailyFragmentReportsUseFourStageOwnersAndReturnToStartNpc() throws Exception {
		assertDailyQuest(15590, 806114, List.of(806224, 806225, 806226, 806227),
			List.of(182215978, 182215979, 182215980, 182215981));
		assertDailyQuest(25590, 806116, List.of(806228, 806229, 806230, 806231),
			List.of(182215982, 182215983, 182215984, 182215985));
	}

	private static void assertDailyQuest(int questId, int startNpc, List<Integer> npcs,
			List<Integer> items) throws Exception {
		QuestDefinition definition = definition(questId);
		for (int stage = 0; stage < 4; stage++) {
			assertDailyNode(definition, "stage" + stage, stage);
		}
		assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status());
		assertSimpleStart(definition, startNpc, "stage0");
		QuestDialogPage[] pages = { QuestDialogPage.SELECT1, QuestDialogPage.SELECT2,
			QuestDialogPage.SELECT3, QuestDialogPage.SELECT4 };
		QuestDialogAction[] actions = { QuestDialogAction.SETPRO1, QuestDialogAction.SETPRO2,
			QuestDialogAction.SETPRO3, QuestDialogAction.SET_SUCCEED };
		for (int stage = 0; stage < 4; stage++) {
			String target = stage < 3 ? "stage" + (stage + 1) : "reward";
			assertTalk(definition, "stage" + stage, target, npcs.get(stage), actions[stage],
				List.of(), List.of(new QuestAction.GiveItem(items.get(stage), 1)),
				List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()));
			assertPage(definition, "stage" + stage, npcs.get(stage), pages[stage]);
		}
		assertRewardContract(definition, startNpc, "0 1 2");
		assertTrue(definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(QuestTransition::event)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(talk -> talk.npcId() == startNpc));
	}

	private static void assertSimpleStart(QuestDefinition definition, int npcId, String target) {
		assertPage(definition, "unaccepted", npcId, QuestDialogAction.QUEST_SELECT);
		assertTalk(definition, "unaccepted", target, npcId, QuestDialogAction.QUEST_ACCEPT_SIMPLE,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_REFUSE_SIMPLE, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
	}

	private static void assertTalkChain(QuestDefinition definition, String source, String target,
			int npcId, String firstPage, String secondPage, String action) {
		assertPage(definition, source, npcId, QuestDialogPage.valueOf(firstPage));
		assertActionPage(definition, source, npcId, QuestDialogAction.valueOf(secondPage),
			QuestDialogPage.valueOf(secondPage));
		assertTalk(definition, source, target, npcId, QuestDialogAction.valueOf(action), List.of(),
			List.of(new QuestAction.SetVariable("var0", Integer.parseInt(target.substring(1))),
				new QuestAction.SetVariable("var1", 0)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertKillStep(QuestDefinition definition, String source, String target,
			int count, int targetValue) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet)
			.toList();
		assertEquals(2, routes.size());
		QuestTransition active = routes.stream().filter(candidate -> candidate.priority() == 1).findFirst().orElseThrow();
		QuestTransition complete = routes.stream().filter(candidate -> candidate.priority() == 0).findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", count - 1)), active.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), active.actions());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", count - 1)), complete.conditions());
		assertEquals(target, complete.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", targetValue),
			new QuestAction.SetVariable("var1", 0)), complete.actions());
	}

	private static void assertRewardContract(QuestDefinition definition, int npcId,
			String fixedRewards) {
		assertPage(definition, "reward", npcId, QuestDialogAction.QUEST_SELECT);
		assertTalk(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD,
			List.of(), List.of(), List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		QuestTransition completion = talk(definition, "reward", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(1, transition.afterCommit().size());
		assertTrue(transition.afterCommit().getFirst() instanceof AfterCommitAction.ShowQuestDialog);
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogPage page) {
		assertActionPage(definition, source, npcId, QuestDialogAction.QUEST_SELECT, page);
	}

	private static void assertActionPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, List.of());
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	/**
	 * 阶段节点只以阶段位段 var0 标识自身：运行期按 source 节点投影匹配路由，把实时击杀计数
	 * var1 钉进投影会让第一只怪之后的每次击杀都匹配不到 source（NO_MATCH，任务不往下）。
	 * Stage nodes identify themselves by the stage field var0 only: matching a route requires its
	 * source projection, so pinning the live kill counter var1 stalls every kill after the first.
	 */
	private static void assertNode(QuestDefinition definition, String label, int var0) {
		QuestNode node = node(definition, label);
		assertEquals(QuestStatus.START, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static void assertDailyNode(QuestDefinition definition, String label, int var0) {
		QuestNode node = node(definition, label);
		assertEquals(QuestStatus.START, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = RetailSequentialQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
