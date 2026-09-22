package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 42：布鲁斯特豪宁献花族 4033 的三行状态阶梯。
 * <p>
 * 客户端任务书三行、槽位 %0/%3/%6：行 0 采集 5 个水仙花交给 Heintz 换花环、
 * 行 1 把花环献给墓碑物件 700379、行 2 回 Heintz 报告领奖。页链分别是
 * `select2 -> select2_1 -> select2_1_1 -> SETPRO1`、墓碑 `select3 -> select3_1`、
 * Heintz `select4 -> SELECT_QUEST_REWARD`。旧定义把 SETPRO1 做成 started 自环、
 * 墓碑 SELECT3_1 直接从 started 进 reward，行 1/行 2 没有状态。
 * </p>
 * <p>
 * Locks batch 42: one state per journal row, the wreath work-item bridges rows 0/1, and the
 * tombstone interaction stays on the row-1 state with its ACTION_ITEM_USE eligibility.
 * </p>
 */
class Batch42TombstoneFlowerRowContractTest {

	private static final int QUEST_ID = 4033;
	private static final int HEINTZ = 205155;
	private static final int TOMBSTONE = 700379;
	private static final int FLOWER_ITEM = 152000463;
	private static final int FLOWER_COUNT = 5;
	private static final int WREATH_ITEM = 182209042;

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "s1", QuestStatus.START, 1);
		assertNode(definition, "reward", QuestStatus.REWARD, 2);

		Set<Integer> rows = new LinkedHashSet<>();
		for (QuestNode candidate : definition.nodes()) {
			Integer row = candidate.projection().variables().get("var0");
			QuestStatus status = candidate.projection().status();
			if (row == null || row > 2 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
				continue;
			}
			rows.add(row);
		}
		assertEquals(Set.of(0, 1, 2), rows, "quest 4033 owns a state per journal row");
	}

	@Test
	void flowerTurnInChainAdvancesRowOne() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, HEINTZ, "started", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);

		List<QuestTransition> turnIn = dialogRoutes(definition, "started", HEINTZ,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).stream()
			.filter(route -> route.afterCommit().contains(
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())))
			.toList();
		assertEquals(1, turnIn.size(), "quest 4033 flower turn-in must be unique");
		assertEquals(List.of(new QuestCondition.HasItem(FLOWER_ITEM, FLOWER_COUNT, true)),
			turnIn.getFirst().conditions(), "quest 4033 must require five flowers");
		assertTrue(turnIn.getFirst().actions().contains(new QuestAction.RemoveItem(FLOWER_ITEM, FLOWER_COUNT)),
			() -> "quest 4033 must consume the flowers: " + turnIn.getFirst().actions());

		assertPageRoute(definition, HEINTZ, "started", QuestDialogAction.SELECT2_1_1,
			QuestDialogPage.SELECT2_1_1);
		List<QuestTransition> craft = dialogRoutes(definition, "started", HEINTZ, QuestDialogAction.SETPRO1);
		assertEquals(1, craft.size(), "quest 4033 craft route must be unique");
		assertEquals("s1", craft.getFirst().targetNode(), "quest 4033 must advance to row 1");
		assertTrue(craft.getFirst().actions().contains(new QuestAction.GiveItem(WREATH_ITEM, 1)),
			() -> "quest 4033 must hand out the wreath: " + craft.getFirst().actions());
		assertTrue(craft.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			"quest 4033 row-0 advance must refresh visibility");
	}

	@Test
	void tombstoneRitualStaysOnRowOneAndAdvancesRowTwo() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, TOMBSTONE, "s1", QuestDialogAction.USE_OBJECT, QuestDialogPage.SELECT3);

		List<QuestTransition> offer = dialogRoutes(definition, "s1", TOMBSTONE, QuestDialogAction.SELECT3_1);
		assertEquals(1, offer.size(), "quest 4033 tombstone offer must be unique");
		assertEquals("reward", offer.getFirst().targetNode(), "quest 4033 must advance to row 2");
		assertEquals(List.of(new QuestCondition.HasItem(WREATH_ITEM, 1, true)),
			offer.getFirst().conditions(), "quest 4033 must require the wreath");
		assertTrue(offer.getFirst().actions().contains(new QuestAction.RemoveItem(WREATH_ITEM, 1)),
			() -> "quest 4033 must consume the wreath: " + offer.getFirst().actions());

		boolean canActOnRowOne = definition.transitions().stream()
			.anyMatch(route -> "s1".equals(route.sourceNode())
				&& route.event() instanceof QuestEvent.CanAct canAct
				&& canAct.templateId() == TOMBSTONE
				&& "ACTION_ITEM_USE".equals(canAct.actionType()));
		assertTrue(canActOnRowOne,
			"quest 4033 tombstone interaction must declare ACTION_ITEM_USE eligibility on row 1");
	}

	@Test
	void rowTwoReportOpensSelectFourAndWindowOne() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, HEINTZ, "reward", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		List<QuestTransition> reward = dialogRoutes(definition, "reward", HEINTZ,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(1, reward.size(), "quest 4033 reward route must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
			"quest 4033 row 2 must open reward window 1");

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == HEINTZ), "quest 4033 completion must stay on Heintz");
		assertTrue(completions.stream().flatMap(route -> route.actions().stream())
				.anyMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 4033 must complete at reward index 0");
	}

	@Test
	void staleRewardSaveHealsToRowTwo() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> stale = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "reward".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 0))))
			.toList();
		assertEquals(1, stale.size(), "quest 4033 must heal the pre-migration reward save");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
			"quest 4033 heals to the report row");
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int row) {
		QuestNode node = definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), () -> "node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"), () -> "node " + label + " row");
	}

	private static void assertPageRoute(QuestDefinition definition, int npcId, String source,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			routes.getFirst().afterCommit(),
			() -> "route " + source + " + npc " + npcId + " + " + action + " page");
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
	}

	private static CompiledQuestDefinition definition() throws IOException {
		try (InputStream input = Batch42TombstoneFlowerRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST_ID + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
