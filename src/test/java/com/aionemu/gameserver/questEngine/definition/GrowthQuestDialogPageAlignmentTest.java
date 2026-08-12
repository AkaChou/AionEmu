package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthQuestDialogPageAlignmentTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final List<Integer> ABBEY_KILL_QUESTS = List.of(
		19673, 19674, 19675, 19676, 19680, 19681, 19682,
		29673, 29674, 29675, 29676, 29680, 29681, 29682);
	private static final List<ItemQuest> ABBEY_ITEM_QUESTS = List.of(
		new ItemQuest(19672, 806698, 186000476, 10),
		new ItemQuest(19677, 806698, 186000476, 50),
		new ItemQuest(19684, 806698, 186000477, 3),
		new ItemQuest(29672, 806700, 186000476, 10),
		new ItemQuest(29677, 806700, 186000476, 50));
	private static final List<WelcomeQuest> ABBEY_WELCOME_QUESTS = List.of(
		new WelcomeQuest(19671, 806698, 806699),
		new WelcomeQuest(19683, 806698, 806708),
		new WelcomeQuest(29671, 806700, 806701));

	@Test
	void deliveryOnlyGrowthQuestsUseTheClientSelect5Page() throws Exception {
		for (int questId = 80369; questId <= 80386; questId++) {
			assertDialogPage(compile(questId), "started", 31, 2375);
		}
		for (int questId = 80487; questId <= 80538; questId++) {
			assertDialogPage(compile(questId), "started", 31, 2375);
		}
	}

	@Test
	void abbeyGrowthQuestsUseTheClientSelectNoneStartPage() throws Exception {
		for (int questId : ABBEY_KILL_QUESTS) {
			int npcId = questId < 20000 ? 806698 : 806700;
			assertDialogPage(compile(questId), "unaccepted", npcId, 31, 4762);
		}
		for (ItemQuest quest : ABBEY_ITEM_QUESTS) {
			assertDialogPage(compile(quest.id()), "unaccepted", quest.npcId(), 31, 4762);
		}
		for (WelcomeQuest quest : ABBEY_WELCOME_QUESTS) {
			assertDialogPage(compile(quest.id()), "unaccepted", quest.instructorNpcId(), 31, 4762);
		}
	}

	@Test
	void abbeyKillGrowthQuestsBecomeRewardReadyOnTheFirstKill() throws Exception {
		for (int questId : ABBEY_KILL_QUESTS) {
			QuestDefinition definition = compile(questId);
			List<QuestTransition> killRoutes = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
					|| transition.event() instanceof QuestEvent.KillNpcSet)
				.toList();
			assertEquals(1, killRoutes.size(), "quest " + questId + " kill route");
			assertEquals("reward", killRoutes.getFirst().targetNode(), "quest " + questId + " kill target");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				killRoutes.getFirst().afterCommit(), "quest " + questId + " kill sync");
			QuestNode rewardNode = definition.nodes().stream()
				.filter(node -> "reward".equals(node.label()))
				.findFirst().orElseThrow();
			assertEquals(1, rewardNode.projection().variables().get("var0"),
				"quest " + questId + " reward projection");

			int npcId = questId < 20000 ? 806698 : 806700;
			assertDialogPage(definition, "reward", npcId, 31, 10002);
		}
	}

	@Test
	void abbeyItemGrowthQuestsUseTheLegacyItemCheckProtocol() throws Exception {
		for (ItemQuest quest : ABBEY_ITEM_QUESTS) {
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);

			List<QuestTransition> checks = talkRoutes(definition, "started", quest.npcId(), 39);
			assertEquals(2, checks.size(), "quest " + quest.id() + " item check branches");
			QuestTransition success = checks.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals("reward", success.targetNode(), "quest " + quest.id() + " item check target");
			assertEquals(List.of(new QuestCondition.HasItem(quest.itemId(), quest.count(), true)),
				success.conditions(), "quest " + quest.id() + " item check condition");
			assertEquals(List.of(new QuestAction.RemoveItem(quest.itemId(), quest.count())),
				success.actions(), "quest " + quest.id() + " item removal");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(10000)),
				success.afterCommit(), "quest " + quest.id() + " successful check page");

			QuestTransition failure = checks.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals("started", failure.targetNode(), "quest " + quest.id() + " failed check target");
			assertTrue(failure.conditions().isEmpty(), "quest " + quest.id() + " failed check fallback");
			assertTrue(failure.actions().isEmpty(), "quest " + quest.id() + " failed check actions");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)),
				failure.afterCommit(), "quest " + quest.id() + " failed check page");
		}
	}

	@Test
	void welcomeQuestsUseTheSellerThenInstructorProtocol() throws Exception {
		for (WelcomeQuest quest : ABBEY_WELCOME_QUESTS) {
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "started", quest.sellerNpcId(), 31, 1011);
			assertDialogPage(definition, "started", quest.sellerNpcId(), 1012, 1012);
			assertDialogPage(definition, "reward", quest.instructorNpcId(), 31, 10002);

			QuestTransition reward = singleTalkRoute(definition, "started", quest.sellerNpcId(), 10255);
			assertEquals("reward", reward.targetNode());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), reward.afterCommit());
			assertTrue(talkRoutes(definition, "unaccepted", quest.sellerNpcId(), 31).isEmpty());
		}
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int dialogId, int pageId) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		QuestTransition route = singleTalkRoute(definition, source, npcId, dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), route.afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId, int dialogId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, dialogId);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		return routes.getFirst();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
	}

	private record ItemQuest(int id, int npcId, int itemId, int count) {
	}

	private record WelcomeQuest(int id, int instructorNpcId, int sellerNpcId) {
	}
}
