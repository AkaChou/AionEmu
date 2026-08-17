package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 21114 的材料采集、孢子液投放、目标击杀和唯一报告 NPC 合同。
 * Locks quest 21114's material collection, spore-liquid placement, target kill, and sole report-NPC contract.
 */
class Quest21114PoisonedFungiRetailFlowTest {
	private static final int QUEST_NPC_ID = 799282;
	private static final int FUNGIE_PATCH_ID = 700729;

	@Test
	void preservesTheLegacyHandlerStateMachineAndNpcRoles() throws Exception {
		QuestDefinition definition = load();

		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 0)), node(definition, "s0").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)), node(definition, "s1").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 2)), node(definition, "s2").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 3)), node(definition, "s3").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 4)), node(definition, "s4").projection());
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 4)),
			node(definition, "reward").projection());

		assertEquals(Set.of(
			new QuestDrop(700727, 182207860, 100, true, 0),
			new QuestDrop(700728, 182207861, 100, true, 0)), Set.copyOf(definition.metadata().drops()));
		assertEquals(List.of(new QuestItemRequirement(182207862, 1)), definition.metadata().questWorkItems());

		Set<Integer> talkNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(Collectors.toSet());
		assertEquals(Set.of(QUEST_NPC_ID, FUNGIE_PATCH_ID), talkNpcIds);
		assertTrue(definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.allMatch(event -> event.npcId() == QUEST_NPC_ID));
		assertFalse(definition.transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.UseItem));
	}

	@Test
	void collectsMaterialsAndGivesTheSporeLiquidInClientPageOrder() throws Exception {
		QuestDefinition definition = load();
		assertPage(definition, "s0", QUEST_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);

		QuestTransition enough = route(definition, "s0", "s1", QUEST_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0);
		assertEquals(List.of(
			new QuestCondition.HasItem(182207860, 10),
			new QuestCondition.HasItem(182207861, 10)), enough.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182207860, 10),
			new QuestAction.RemoveItem(182207861, 10),
			new QuestAction.SetVariable("var0", 1)), enough.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			enough.afterCommit());

		QuestTransition missing = route(definition, "s0", "s0", QUEST_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 1);
		assertEquals(List.of(), missing.conditions());
		assertEquals(List.of(), missing.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			missing.afterCommit());

		assertPage(definition, "s1", QUEST_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		QuestTransition giveLiquid = route(definition, "s1", "s2", QUEST_NPC_ID,
			QuestDialogAction.SETPRO2, null);
		assertEquals(List.of(
			new QuestAction.GiveItem(182207862, 1),
			new QuestAction.SetVariable("var0", 2)), giveLiquid.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			giveLiquid.afterCommit());
	}

	@Test
	void usesTheAuthoritativeFungiePatchBeforeUnlockingTheTargetKill() throws Exception {
		QuestDefinition definition = load();

		QuestTransition eligibility = definition.transitions().stream()
			.filter(transition -> "s2".equals(transition.sourceNode()))
			.filter(transition -> "s2".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == FUNGIE_PATCH_ID
				&& "ACTION_ITEM_USE".equals(canAct.actionType()))
			.findFirst().orElseThrow();
		assertEquals(List.of(), eligibility.conditions());
		assertEquals(List.of(), eligibility.actions());
		assertEquals(List.of(), eligibility.afterCommit());

		QuestTransition usePatch = route(definition, "s2", "s3", FUNGIE_PATCH_ID,
			QuestDialogAction.USE_OBJECT, null);
		assertEquals(List.of(
			new QuestAction.RemoveItem(182207862, 1),
			new QuestAction.SetVariable("var0", 3)), usePatch.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			usePatch.afterCommit());

		assertPage(definition, "s3", QUEST_NPC_ID, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		QuestTransition confirm = route(definition, "s3", "s4", QUEST_NPC_ID,
			QuestDialogAction.SETPRO4, null);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), confirm.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			confirm.afterCommit());

		QuestTransition kill = definition.transitions().stream()
			.filter(transition -> "s4".equals(transition.sourceNode()))
			.filter(transition -> "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(216563)))
			.findFirst().orElseThrow();
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), kill.afterCommit());
	}

	@Test
	void reportsAndCompletesOnlyAtTheQuestNpc() throws Exception {
		QuestDefinition definition = load();
		assertPage(definition, "reward", QUEST_NPC_ID, QuestDialogAction.USE_OBJECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", QUEST_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.toList();
		assertEquals(16, completions.size());
		assertTrue(completions.stream().allMatch(transition -> talk(transition).npcId() == QUEST_NPC_ID));
		for (QuestTransition completion : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 167620, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 6240944, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), completion.actions());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, source, npcId, action, null);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, Integer priority) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc event
				&& event.npcId() == npcId && Objects.equals(event.dialogId(), action.id()))
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
		String resource = "/aion/data/static_data/quest_definition/quests/21114.xml";
		try (InputStream input = Quest21114PoisonedFungiRetailFlowTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource)).definition();
		}
	}
}
