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
 * 锁定 25050 的材料上交、仪式 NPC 生成和唯一领奖 NPC 合同。
 * Locks quest 25050's material turn-in, ritual NPC spawn, and sole reward-NPC contract.
 */
class Quest25050RetailFlowAlignmentTest {
	@Test
	void preservesTheLegacyHandlerStateMachineAndClientPageChain() throws Exception {
		QuestDefinition definition = load();
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 0)), node(definition, "s0").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)), node(definition, "s1").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 2)), node(definition, "s2").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 3)), node(definition, "s3").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 4)),
			node(definition, "reward").projection());

		List<QuestTransition> starts = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.toList();
		assertFalse(starts.isEmpty());
		assertTrue(starts.stream().allMatch(transition -> talk(transition).npcId() == 804915));
		assertDialogPage(definition, "s0", 804915, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT1.id());

		QuestTransition turnIn = route(definition, "s0", "s1", 804915,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0);
		assertEquals(List.of(
			new QuestCondition.HasItem(182215716, 4),
			new QuestCondition.HasItem(182215717, 4),
			new QuestCondition.HasItem(182215718, 4)), turnIn.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182215716, 4),
			new QuestAction.RemoveItem(182215717, 4),
			new QuestAction.RemoveItem(182215718, 4),
			new QuestAction.SetVariable("var0", 1)), turnIn.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), turnIn.afterCommit());

		QuestTransition insufficient = route(definition, "s0", "s0", 804915,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 1);
		assertEquals(List.of(), insufficient.conditions());
		assertEquals(List.of(), insufficient.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			insufficient.afterCommit());

		assertDialogPage(definition, "s1", 804915, QuestDialogAction.SELECT2.id(), QuestDialogPage.SELECT2.id());
		QuestTransition giveOffering = route(definition, "s1", "s2", 804915,
			QuestDialogAction.SETPRO2.id(), null);
		assertEquals(List.of(new QuestAction.GiveItem(182215719, 1),
			new QuestAction.SetVariable("var0", 2)), giveOffering.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), giveOffering.afterCommit());

		QuestTransition altar = route(definition, "s2", "s2", 731553,
			QuestDialogAction.USE_OBJECT.id(), null);
		assertEquals(List.of(new QuestCondition.HasItem(182215719, 1)), altar.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			altar.afterCommit());

		QuestTransition ceremony = route(definition, "s2", "s3", 731553,
			QuestDialogAction.SETPRO3.id(), null);
		assertEquals(List.of(new QuestCondition.HasItem(182215719, 1)), ceremony.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182215719, 1),
			new QuestAction.SetVariable("var0", 3)), ceremony.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.SpawnNpc("ritual-guide", 805160,
				new QuestSpawnLocation.Fixed(220080000, QuestInstanceTarget.currentOrDefault(),
					2046.8f, 1588.8f, 348.4f, (byte) 90)),
			new AfterCommitAction.CloseDialog()), ceremony.afterCommit());
		AfterCommitAction.SpawnNpc ritualGuide = new AfterCommitAction.SpawnNpc("ritual-guide", 805160,
			new QuestSpawnLocation.Fixed(220080000, QuestInstanceTarget.currentOrDefault(),
				2046.8f, 1588.8f, 348.4f, (byte) 90));
		QuestTransition recovery = definition.transitions().stream()
			.filter(transition -> "s3".equals(transition.sourceNode()))
			.filter(transition -> "s3".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
			.findFirst().orElseThrow();
		assertEquals(List.of(), recovery.conditions());
		assertEquals(List.of(), recovery.actions());
		assertEquals(List.of(ritualGuide), recovery.afterCommit());

		assertDialogPage(definition, "s3", 805160, QuestDialogAction.QUEST_SELECT.id(), QuestDialogPage.SELECT4.id());
		assertDialogPage(definition, "s3", 805160, QuestDialogAction.SELECT4_1.id(), QuestDialogPage.SELECT4_1.id());
		QuestTransition succeed = route(definition, "s3", "reward", 805160,
			QuestDialogAction.SET_SUCCEED.id(), null);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), succeed.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.DespawnNpc("ritual-guide"),
			new AfterCommitAction.CloseDialog()), succeed.afterCommit());

		assertDialogPage(definition, "reward", 804915, QuestDialogAction.QUEST_SELECT.id(),
			QuestDialogPage.DEFAULT_SUCCESS.id());
		assertDialogPage(definition, "reward", 804915, QuestDialogAction.SELECT_QUEST_REWARD.id(),
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id());
	}

	@Test
	void completesOnlyAtTheStartNpcWithoutChargingMaterialsAgain() throws Exception {
		QuestDefinition definition = load();
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.toList();
		assertEquals(3, completions.size());
		assertTrue(completions.stream().allMatch(transition -> talk(transition).npcId() == 804915));
		assertTrue(completions.stream().allMatch(transition -> transition.conditions().isEmpty()));
		assertTrue(completions.stream().flatMap(transition -> transition.actions().stream())
			.noneMatch(QuestAction.RemoveItem.class::isInstance));

		List<Integer> selectableItems = List.of(164000066, 164000121, 164000070);
		for (int index = 0; index < selectableItems.size(); index++) {
			QuestTransition completion = route(definition, "reward", "complete", 804915,
				QuestDialogAction.SELECTED_QUEST_REWARD1.id() + index, null);
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 106740, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 9490140, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000231, 10, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 186000237, 15, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", selectableItems.get(index), 12, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.actions());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}

		Set<Integer> forbiddenNpcIds = Set.of(731553, 805160);
		assertTrue(definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode())
				|| "complete".equals(transition.targetNode()))
			.map(Quest25050RetailFlowAlignmentTest::talk)
			.noneMatch(event -> forbiddenNpcIds.contains(event.npcId())));
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		QuestTransition transition = route(definition, source, source, npcId, dialogId, null);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), transition.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			int dialogId, Integer priority) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc event
				&& event.npcId() == npcId && Objects.equals(event.dialogId(), dialogId))
			.filter(transition -> Objects.equals(transition.priority(), priority))
			.findFirst().orElseThrow();
	}

	private static QuestEvent.TalkToNpc talk(QuestTransition transition) {
		return assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/25050.xml";
		try (InputStream input = Quest25050RetailFlowAlignmentTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource)).definition();
		}
	}
}
