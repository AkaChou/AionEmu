package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 12 个收集交付任务的客户端动作链对齐合同：交付判定挂客户端实际发送的
 * CHECK_USER_HAS_QUEST_ITEM(39)，QUEST_SELECT 只显示客户端存在的入口页，
 * 未集齐回落页与 reward owner 归属均以 Aion 5.8 客户端与旧 handler 为准。
 * Locks the client action chain alignment contract for 12 collect turn-in quests: turn-in checks
 * are bound to the client-sent CHECK_USER_HAS_QUEST_ITEM(39), QUEST_SELECT shows only client-owned
 * entry pages, and not-ready fallback pages plus reward ownership follow the Aion 5.8 client and
 * legacy handlers.
 */
class CollectTurnInClientActionAlignmentBatchTest {
	private static final Set<Integer> RAKSANG_QUESTS = Set.of(18739, 18740);

	@Test
	void simpleSelect5QuestsCheckItemsOnClientAction39() throws Exception {
		checkDoubleBranchTurnIn(80482, 831959, 182215419, 1, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(80486, 831961, 182215421, 1, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(1103, 203057, 182200201, 3, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(30312, 799322, 182209715, 20, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(30314, 799226, 186000098, 100, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(30315, 799226, 186000098, 200, "SELECT5", "SELECT6");
		checkDoubleBranchTurnIn(1124, 790001, 182200210, 3, "SELECT5", "SELECT6");
	}

	@Test
	void quest1137ChecksOnlyTheCollectedFossilNotTheWorkItem() throws Exception {
		QuestDefinition definition = definition(1137).definition();
		checkTurnInBranches(definition, 203111, List.of(new QuestCondition.HasItem(182200513, 1)), "SELECT6");
		// 工具物品 182200512 是 work-item，由完成清理回收，不得作为交付条件或被交付移除。
		assertTrue(definition.transitions().stream()
			.flatMap(candidate -> candidate.conditions().stream())
			.noneMatch(condition -> condition instanceof QuestCondition.HasItem hasItem
				&& hasItem.itemId() == 182200512), "work item must not gate turn-in");
		assertTrue(definition.transitions().stream()
			.flatMap(candidate -> candidate.actions().stream())
			.noneMatch(action -> action instanceof QuestAction.RemoveItem remove
				&& remove.itemId() == 182200512), "work item must not be removed at turn-in");
	}

	@Test
	void dualNpcQuest1351KeepsRewardOwnershipOnTheTurnInNpc() throws Exception {
		QuestDefinition definition = definition(1351).definition();

		// 接取 NPC 203965 只保留任务描述页路由，不得拥有交付或完成路由。
		QuestTransition select = talk(definition, "started", "started", 203965,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			select.afterCommit());
		assertTrue(definition.transitions().stream()
			.noneMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203965 && "reward".equals(candidate.targetNode())),
			"203965 must not own reward routes");

		// 交付 NPC 203983：入口 SELECT5，动作 39 双分支，集齐同次交互移除物品并打开奖励窗口。
		QuestTransition entry = talk(definition, "started", "started", 203983,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			entry.afterCommit());
		checkTurnInBranches(definition, 203983, 182201321, 10, "SELECT6");
	}

	@Test
	void raksangQuestsShowTheClientOwnedEntryPageAndFailPage() throws Exception {
		for (int questId : RAKSANG_QUESTS) {
			QuestDefinition definition = definition(questId).definition();
			QuestTransition entry = talk(definition, "started", "started", 804707,
				QuestDialogAction.QUEST_SELECT.id());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
				entry.afterCommit(), "quest " + questId + " entry page");
			checkTurnInBranches(definition, 804707, questId == 18739 ? 182215692 : 182215693,
				questId == 18739 ? 5 : 8, "CHECK_USER_ITEM_FAIL");
		}
	}

	@Test
	void expertTapperQuestsCheckItemsOnTheSelect2PageAction() throws Exception {
		QuestDefinition asmodians = definition(29000).definition();
		QuestTransition entry = talk(asmodians, "started", "started", 204096,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			entry.afterCommit(), "29000 entry page");
		checkTurnInBranches(asmodians, 204096,
			List.of(
				new QuestCondition.HasItem(152003004, 1),
				new QuestCondition.HasItem(152003005, 1),
				new QuestCondition.HasItem(152003006, 1)),
			"CHECK_USER_ITEM_FAIL");

		QuestDefinition elyos = definition(29002).definition();
		for (int npcId : new int[] {204257, 204099}) {
			QuestTransition secondEntry = talk(elyos, "started", "started", npcId,
				QuestDialogAction.QUEST_SELECT.id());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				secondEntry.afterCommit(), "29002 npc " + npcId + " entry page");
			checkTurnInBranches(elyos, npcId,
				List.of(
					new QuestCondition.HasItem(152003007, 1),
					new QuestCondition.HasItem(152003008, 1)),
				"CHECK_USER_ITEM_FAIL");
		}
	}

	@Test
	void stigmaScarChainSpawnsAndDeletesTheMidwayNpcBeforeTheTurnIn() throws Exception {
		QuestDefinition definition = definition(30217).definition();
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));

		QuestTransition spawn = transition(definition, "started", "s1",
			new QuestEvent.TalkToNpc(798941, QuestDialogAction.SETPRO1.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), spawn.actions());
		assertTrue(spawn.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawnNpc
			&& spawnNpc.templateId() == 799506), "SETPRO1 must spawn 799506: " + spawn.afterCommit());

		QuestTransition despawn = transition(definition, "s1", "s2",
			new QuestEvent.TalkToNpc(799506, QuestDialogAction.SETPRO2.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), despawn.actions());
		assertEquals(List.of(
			new AfterCommitAction.DeleteInteractionNpc(false),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), despawn.afterCommit());

		// 最终判定挂点 NPC 对话（客户端在 s2 阶段打开对话即判定），集齐显示 SELECT3(1693)。
		QuestTransition ready = transition(definition, "s2", "reward",
			new QuestEvent.TalkToNpc(798909, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(Integer.valueOf(0), ready.priority());
		assertEquals(List.of(
			new QuestCondition.HasItem(182209618, 1),
			new QuestCondition.HasItem(182209619, 1)), ready.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())), ready.afterCommit());
		QuestTransition notReady = transition(definition, "s2", "s2",
			new QuestEvent.TalkToNpc(798909, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(Integer.valueOf(1), notReady.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			notReady.afterCommit());

		// 1693 页的确认按钮动作 39 在奖励状态打开奖励窗口。
		QuestTransition rewardWindow = talk(definition, "reward", "reward", 798909,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardWindow.afterCommit());
	}

	@Test
	void quest18745KeepsTheRewardOwnerExclusive() throws Exception {
		QuestDefinition definition = definition(18745).definition();
		checkTurnInBranches(definition, 804707, 182215943, 1, "CHECK_USER_ITEM_FAIL");
		for (int npcId : new int[] {206378, 206379, 206380, 702958}) {
			assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && "started".equals(candidate.sourceNode())
					&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				"npc " + npcId + " must not own started dialog routes");
			assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && "reward".equals(candidate.sourceNode())),
				"npc " + npcId + " must not own reward routes");
		}
	}

	private static void checkDoubleBranchTurnIn(int questId, int npcId, int itemId, int count,
			String entryPage, String failPage) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		QuestTransition entry = talk(definition, "started", "started", npcId,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			pageIdOf(entryPage))), entry.afterCommit(),
			"quest " + questId + " entry page");
		checkTurnInBranches(definition, npcId, itemId, count, failPage);
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, int itemId, int count,
			String failPage) {
		checkTurnInBranches(definition, npcId, List.of(new QuestCondition.HasItem(itemId, count)), failPage);
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, List<QuestCondition> conditions,
			String failPage) {
		QuestTransition success = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(0), success.priority());
		assertEquals(conditions, success.conditions());
		List<QuestAction> removals = conditions.stream()
			.map(condition -> (QuestCondition.HasItem) condition)
			.map(hasItem -> (QuestAction) new QuestAction.RemoveItem(hasItem.itemId(), hasItem.count()))
			.toList();
		assertEquals(removals, success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			success.afterCommit());
		QuestTransition failure = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(1), failure.priority());
		assertEquals(List.of(), failure.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageIdOf(failPage))),
			failure.afterCommit());
	}

	private static int pageIdOf(String pageName) {
		return switch (pageName) {
			case "SELECT1" -> QuestDialogPage.SELECT1.id();
			case "SELECT2" -> QuestDialogPage.SELECT2.id();
			case "SELECT5" -> QuestDialogPage.SELECT5.id();
			case "SELECT6" -> QuestDialogPage.SELECT6.id();
			case "CHECK_USER_ITEM_FAIL" -> QuestDialogPage.CHECK_USER_ITEM_FAIL.id();
			default -> throw new IllegalArgumentException("unknown page " + pageName);
		};
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = CollectTurnInClientActionAlignmentBatchTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
