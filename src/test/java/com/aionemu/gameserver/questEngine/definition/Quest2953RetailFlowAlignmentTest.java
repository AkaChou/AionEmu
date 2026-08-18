package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 校验任务 2953 的客户端对话路径、物品生命周期与唯一 NPC 归属。
 * Verifies quest 2953 client dialog routes, item lifecycle, and exclusive NPC ownership.
 */
class Quest2953RetailFlowAlignmentTest {
	private static final int START_AND_REWARD_NPC = 204191;
	private static final int DELIVERY_NPC = 204071;
	private static final int SUPPLY_REQUEST = 182207039;

	@Test
	void followsTheClientDeliveryPagesAndLegacyItemLifecycle() throws Exception {
		QuestDefinition definition = load();
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "reward", QuestStatus.REWARD, 1);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);
		assertEquals(List.of(new QuestItemRequirement(SUPPLY_REQUEST, 1)),
			definition.metadata().questWorkItems());

		QuestTransition accept = route(definition, "unaccepted", "started", START_AND_REWARD_NPC,
			QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(SUPPLY_REQUEST, 1)), accept.actions());
		assertNull(accept.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
		assertFalse(hasRoute(definition, "unaccepted", DELIVERY_NPC));

		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);

		QuestTransition delivery = route(definition, "started", "reward", DELIVERY_NPC,
			QuestDialogAction.SETPRO1);
		assertEquals(List.of(new QuestCondition.HasItem(SUPPLY_REQUEST, 1)), delivery.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(SUPPLY_REQUEST, 1)), delivery.actions());
		assertNull(delivery.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), delivery.afterCommit());

		assertPage(definition, "reward", START_AND_REWARD_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT5);
		assertPage(definition, "reward", START_AND_REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertFalse(hasRoute(definition, "reward", DELIVERY_NPC));

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == START_AND_REWARD_NPC)
			.toList();
		assertEquals(16, completions.size());
		for (QuestTransition completion : completions) {
			assertEquals(List.of(), completion.conditions());
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 500, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 150, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), completion.actions());
			assertNull(completion.priority());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, source, npcId, action);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertNull(transition.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size());
		return matches.getFirst();
	}

	private static boolean hasRoute(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/2953.xml";
		try (InputStream input = Quest2953RetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource)).definition();
		}
	}
}
