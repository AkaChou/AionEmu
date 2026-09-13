package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 28510 的旧版 NPC 责任链、加工奥鲁卡交互和最终领奖页面。
 * Locks quest 28510's legacy NPC ownership, processed-Odella interaction, and final reward page.
 */
class Quest28510ClientDialogAlignmentTest {
	private static final int START_NPC = 804605;
	private static final int KILL_NPC = 700950;
	private static final int OBJECT_NPC = 700953;
	private static final int REWARD_NPC = 203560;
	private static final int WORK_ITEM = 182212021;

	@Test
	void preservesTheLegacyHaramelNpcRolesAndDialogFlow() throws Exception {
		QuestDefinition definition = load();

		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of()), node(definition, "started").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 5)), node(definition, "reward").projection());
		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM, 1)), definition.metadata().questWorkItems());

		Set<Integer> unacceptedNpcIds = routes(definition, "unaccepted").stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(Collectors.toSet());
		assertEquals(Set.of(START_NPC), unacceptedNpcIds);
		assertTrue(routes(definition, "unaccepted", OBJECT_NPC).isEmpty());
		assertTrue(routes(definition, "unaccepted", REWARD_NPC).isEmpty());
		assertTrue(routes(definition, "started", REWARD_NPC).isEmpty());
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		assertTrue(routes(definition, "reward", OBJECT_NPC).isEmpty());

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertEquals("started", accept.targetNode());
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(WORK_ITEM, 1)));

		QuestTransition kill = definition.transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& transition.event().equals(new QuestEvent.KillNpc(KILL_NPC)))
			.findFirst().orElseThrow();
		assertEquals("started", kill.targetNode());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 3)), kill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), kill.afterCommit());

		assertObjectStep(definition, 3, "started", List.of(new QuestAction.SetVariable("var0", 4)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertObjectStep(definition, 4, "started", List.of(new QuestAction.SetVariable("var0", 5)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertObjectStep(definition, 5, "reward", List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		List<QuestTransition> canActRoutes = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == OBJECT_NPC
				&& "ACTION_ITEM_USE".equals(canAct.actionType()))
			.toList();
		assertEquals(3, canActRoutes.size());
		assertTrue(canActRoutes.stream().allMatch(transition ->
			transition.conditions().size() == 1
				&& transition.conditions().getFirst() instanceof QuestCondition.QuestVariableIs));

		assertPage(definition, "reward", REWARD_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertTrue(route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECTED_QUEST_REWARD1)
			.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static void assertObjectStep(QuestDefinition definition, int currentValue, String targetNode,
		List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = routes(definition, "started", OBJECT_NPC).stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(QuestDialogAction.USE_OBJECT.id()).equals(talk.dialogId()))
			.filter(candidate -> candidate.conditions().equals(
				List.of(new QuestCondition.QuestVariableIs("var0", currentValue))))
			.findFirst().orElseThrow();
		assertEquals(targetNode, transition.targetNode());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 28510 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(action.id()).equals(talk.dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return routes(definition, source).stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/28510.xml";
		try (InputStream input = Quest28510ClientDialogAlignmentTest.class.getResourceAsStream(resource)) {
			if (input == null) throw new IllegalStateException("missing quest definition 28510.xml");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
