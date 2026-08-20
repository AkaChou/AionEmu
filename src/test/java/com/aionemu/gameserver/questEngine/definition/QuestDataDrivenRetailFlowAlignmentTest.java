package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定两个多步骤 data-driven 任务的旧 handler 状态机、NPC 角色和计数器边界。
 * Locks the legacy-handler state machine, NPC roles, and counter boundaries for two multi-step data-driven quests.
 */
class QuestDataDrivenRetailFlowAlignmentTest {
	@Test
	void quest25321KeepsTalkStepsSeparateFromSixHuntCounters() throws Exception {
		QuestDefinition definition = load(25321);
		assertOnlyNpcStart(definition, 805342);
		assertOnlyNpcComplete(definition, 805342);
		for (int index = 0; index <= 11; index++) {
			assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", index)),
				node(definition, "s" + index).projection());
		}
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 12, "var1", 0)),
			node(definition, "reward").projection());

		assertDialogPage(definition, "s0", 805344, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT1.id());
		assertDialogPage(definition, "s0", 805344, QuestDialogAction.SELECT1_1.id(), QuestDialogPage.SELECT1_1.id());
		assertTalk(definition, "s0", "s1", 805344, QuestDialogAction.SETPRO1.id());
		assertDialogPage(definition, "s2", 805345, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT3.id());
		assertDialogPage(definition, "s2", 805345, QuestDialogAction.SELECT3_1.id(), QuestDialogPage.SELECT3_1.id());
		assertTalk(definition, "s2", "s3", 805345, QuestDialogAction.SETPRO3.id());
		assertDialogPage(definition, "s4", 805346, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT5.id());
		assertDialogPage(definition, "s4", 805346, QuestDialogAction.SELECT5_1.id(), QuestDialogPage.SELECT5_1.id());
		assertTalk(definition, "s4", "s5", 805346, QuestDialogAction.SETPRO5.id());
		assertDialogPage(definition, "s6", 805347, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT7.id());
		assertDialogPage(definition, "s6", 805347, QuestDialogAction.SELECT7_1.id(), QuestDialogPage.SELECT7_1.id());
		assertTalk(definition, "s6", "s7", 805347, QuestDialogAction.SETPRO7.id());
		assertDialogPage(definition, "s8", 805348, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT9.id());
		assertDialogPage(definition, "s8", 805348, QuestDialogAction.SELECT9_1.id(), QuestDialogPage.SELECT9_1.id());
		assertTalk(definition, "s8", "s9", 805348, QuestDialogAction.SETPRO9.id());
		assertDialogPage(definition, "s10", 805349, QuestDialogAction.QUEST_SELECT.id(), 6500);
		assertDialogPage(definition, "s10", 805349, 6501, 6501);
		assertTalk(definition, "s10", "s11", 805349, QuestDialogAction.SETPRO11.id());

		assertCounter(definition, "s1", "s2", 29,
			ids(219693, 219694, 219695, 219696, 219697, 219698));
		assertCounter(definition, "s3", "s4", 29,
			ids(219778, 219779, 219780, 219781, 219782, 219783, 219784, 219786));
		assertCounter(definition, "s5", "s6", 9,
			ids(236363, 236364, 236365, 236366, 236367, 236368, 236369, 236370, 236371, 236372,
				236373, 236374, 236375, 236376, 236377, 236378, 236379, 236380, 236381, 236382,
				236383, 236384, 236385, 236386, 236387, 236388, 236389, 236390, 236586, 236587,
				236588, 236589, 236590, 236591, 236592, 236593, 236594, 236595, 236596, 236597,
				236598, 236599, 236600, 236601, 236602, 236603, 236604, 236605, 236606, 236607,
				236608, 236609, 236610, 236611, 236612, 236613));
		assertCounter(definition, "s7", "s8", 29,
			ids(234694, 234696, 234697, 234699, 234701, 234702, 234703));
		assertCounter(definition, "s9", "s10", 29,
			ids(234244, 234246, 234247, 234503, 234505, 234517));
		assertCounter(definition, "s11", "reward", 29, ids(234269, 234271, 234272));

		QuestTransition report = route(definition, "reward", "reward", 805342,
			QuestDialogAction.SELECT_QUEST_REWARD.id(), null);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
	}

	@Test
	void quest25306KeepsCollectionInteractionAndFiveHuntStepsInOrder() throws Exception {
		QuestDefinition definition = load(25306);
		assertOnlyNpcStart(definition, 805339);
		assertOnlyNpcComplete(definition, 805339);
		assertEquals(List.of(new QuestItemRequirement(182215876, 1)), definition.metadata().questWorkItems());
		assertEquals(List.of(
			new QuestDrop(702829, 182215852, 100, true, 1),
			new QuestDrop(702862, 182215922, 100, true, 1)), definition.metadata().drops());
		assertEquals(Set.of(
			new QuestEvent.CanAct(702829, "ACTION_ITEM_USE"),
			new QuestEvent.CanAct(702862, "ACTION_ITEM_USE")), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.CanAct)
			.map(QuestTransition::event)
			.collect(java.util.stream.Collectors.toSet()));
		for (int objectId : List.of(702829, 702862)) {
			QuestTransition useObject = route(definition, "s1", "s1", objectId,
				QuestDialogAction.USE_OBJECT.id(), null);
			assertEquals(List.of(), useObject.conditions());
			assertEquals(List.of(), useObject.actions());
			assertEquals(List.of(), useObject.afterCommit());
		}

		assertDialogPage(definition, "s0", 805340, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT1.id());
		assertDialogPage(definition, "s0", 805340, QuestDialogAction.SELECT1_1.id(), QuestDialogPage.SELECT1_1.id());
		QuestTransition startConversation = route(definition, "s0", "s1", 805340,
			QuestDialogAction.SETPRO1.id(), null);
		assertEquals(List.of(new QuestAction.GiveItem(152231954, 1), new QuestAction.SetVariable("var0", 1)),
			startConversation.actions());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			node(definition, "s1").projection());

		QuestTransition collect = route(definition, "s1", "s2", 805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0);
		assertEquals(List.of(
			new QuestCondition.HasItem(182215851, 150),
			new QuestCondition.HasItem(182215852, 10),
			new QuestCondition.HasItem(182215922, 10),
			new QuestCondition.HasItem(182215853, 3)), collect.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182215851, 150),
			new QuestAction.RemoveItem(182215852, 10),
			new QuestAction.RemoveItem(182215922, 10),
			new QuestAction.RemoveItem(182215853, 3),
			new QuestAction.SetVariable("var0", 2)), collect.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), collect.afterCommit());
		QuestTransition missingItems = route(definition, "s1", "s1", 805340,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 1);
		assertEquals(List.of(), missingItems.conditions());
		assertEquals(List.of(), missingItems.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			missingItems.afterCommit());

		assertDialogPage(definition, "s1", 805340, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT2.id());
		assertDialogPage(definition, "s1", 805340, QuestDialogAction.SELECT2_1.id(), QuestDialogPage.SELECT2_1.id());
		assertDialogPage(definition, "s1", 805340, 1438, QuestDialogPage.SELECT2_2.id());
		assertDialogPage(definition, "s1", 805340, 1523, QuestDialogPage.SELECT2_3.id());
		assertDialogPage(definition, "s1", 805340, 1608, QuestDialogPage.SELECT2_4.id());
		assertDialogPage(definition, "s2", 805340, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT3.id());

		assertTalk(definition, "s2", "s3", 805340, QuestDialogAction.SETPRO3.id());
		assertCounter(definition, "s3", "s4", 59,
			ids(234711, 234712, 234713, 234714, 234715, 234716, 234717, 234718, 234719));
		assertCounter(definition, "s4", "s5", 59,
			ids(234292, 234294, 234295, 234296, 234298, 234528, 234529));
		assertCounter(definition, "s5", "s6", 29, ids(234260, 234262, 234264, 234512));
		assertSingleKillStep(definition, "s6", "s7", ids(232853, 233491, 233544, 233859, 234190));
		assertCounter(definition, "s7", "s8", 4, ids(231073, 231130, 236277));

		assertDialogPage(definition, "s8", 805340, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT9.id());
		assertDialogPage(definition, "s8", 805340, QuestDialogAction.SELECT9_1.id(), QuestDialogPage.SELECT9_1.id());
		assertDialogPage(definition, "s8", 805340, QuestDialogAction.SELECT9_1_1.id(), QuestDialogPage.SELECT9_1_1.id());
		QuestTransition workItem = route(definition, "s8", "s9", 805340,
			QuestDialogAction.SETPRO9.id(), null);
		assertTrue(workItem.actions().contains(new QuestAction.GiveItem(182215876, 1)));
		assertDialogPage(definition, "s9", 805339, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT10.id());
		assertDialogPage(definition, "s9", 805339, QuestDialogAction.SELECT10_1.id(), QuestDialogPage.SELECT10_1.id());
		QuestTransition finalReport = route(definition, "s9", "reward", 805339,
			QuestDialogAction.SELECT_QUEST_REWARD.id(), null);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 10)), finalReport.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			finalReport.afterCommit());
		assertTrue(definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.filter(transition -> transition.sourceNode().equals("unaccepted"))
			.map(QuestDataDrivenRetailFlowAlignmentTest::talk)
			.noneMatch(event -> event.npcId() == 805340 || event.npcId() == 702829 || event.npcId() == 702862));
	}

	private static void assertCounter(QuestDefinition definition, String source, String target,
			int thresholdBeforeCompletion, Set<Integer> npcIds) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.toList();
		assertEquals(2, routes.size());
		assertTrue(routes.stream().allMatch(transition ->
			transition.event().equals(new QuestEvent.KillNpcSet(npcIds))));
		QuestTransition continuing = routes.stream()
			.filter(transition -> source.equals(transition.targetNode())).findFirst().orElseThrow();
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", thresholdBeforeCompletion)),
			continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());
		QuestTransition completion = routes.stream()
			.filter(transition -> target.equals(transition.targetNode())).findFirst().orElseThrow();
		assertEquals(0, completion.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", thresholdBeforeCompletion)),
			completion.conditions());
		int targetVar0 = node(definition, target).projection().variables().get("var0");
		assertEquals(List.of(new QuestAction.SetVariable("var0", targetVar0),
			new QuestAction.SetVariable("var1", 0)), completion.actions());
		QuestStateSyncMode completionSync = "reward".equals(target)
			? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(completionSync)), completion.afterCommit());
		assertTrue(definition.nodes().stream()
			.filter(node -> source.equals(node.label()))
			.allMatch(node -> !node.projection().variables().containsKey("var1")));
	}

	private static void assertSingleKillStep(QuestDefinition definition, String source, String target,
			Set<Integer> npcIds) {
		QuestTransition transition = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet)
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.KillNpcSet(npcIds), transition.event());
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 7),
			new QuestAction.SetVariable("var1", 0)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertTalk(QuestDefinition definition, String source, String target, int npcId, int dialogId) {
		QuestTransition transition = route(definition, source, target, npcId, dialogId, null);
		int targetValue = "reward".equals(target) ? 10 : Integer.parseInt(target.substring(1));
		assertEquals(List.of(new QuestAction.SetVariable("var0", targetValue)),
			transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), transition.afterCommit());
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		QuestTransition transition = route(definition, source, source, npcId, dialogId, null);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), transition.afterCommit());
	}

	private static void assertOnlyNpcStart(QuestDefinition definition, int npcId) {
		List<QuestTransition> starts = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.toList();
		assertFalse(starts.isEmpty());
		assertTrue(starts.stream().allMatch(transition -> talk(transition).npcId() == npcId));
	}

	private static void assertOnlyNpcComplete(QuestDefinition definition, int npcId) {
		List<QuestTransition> complete = definition.transitions().stream()
			.filter(transition -> "complete".equals(transition.targetNode()))
			.toList();
		assertFalse(complete.isEmpty());
		assertTrue(complete.stream().allMatch(transition -> talk(transition).npcId() == npcId));
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			int dialogId, Integer priority) {
		return definition.transitions().stream()
			.filter(transition -> Objects.equals(source, transition.sourceNode()))
			.filter(transition -> Objects.equals(target, transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc event
				&& event.npcId() == npcId && Objects.equals(event.dialogId(), dialogId))
			.filter(transition -> Objects.equals(transition.priority(), priority))
			.findFirst().orElseThrow();
	}

	private static QuestEvent.TalkToNpc talk(QuestTransition transition) {
		return assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
	}

	private static Set<Integer> ids(Integer... ids) {
		return Set.of(ids);
	}

	private static QuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestDataDrivenRetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource)).definition();
		}
	}
}
