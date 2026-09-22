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
 * 锁定批次 43：食人花解毒剂族 2239 的三行状态阶梯。
 * <p>
 * 客户端任务书三行、槽位 %0/%3/%6：行 0 和 Vovetirn 对话、行 1 交 3 个食人花外皮 182203228
 * 做出解毒剂 182203227、行 2 把解毒剂交给闯祸的厨师 Gilungk。页链分别是 Vovetirn 的
 * `select2 -> select2_1 -> select2_1_1 -> SETPRO1`、`select3` 的 CHECK 链（成功 `select3_2`
 * 的 SETPRO2 / 失败 `select3_1`）、Gilungk 的 `select4`（SETPRO3）。旧定义两个 NPC 都能接取
 * 与领奖，SETPRO1 直接进 reward，行 1/行 2 没有状态。
 * </p>
 * <p>
 * Locks batch 43: one state per journal row, the peel turn-in hands out the antidote work-item,
 * and only Gilungk owns the row-2 hand-over plus reward window.
 * </p>
 */
class Batch43MalodorAntidoteRowContractTest {

	private static final int QUEST_ID = 2239;
	private static final int GILUNGK = 203613;
	private static final int VOVETIRN = 203630;
	private static final int PEEL_ITEM = 182203228;
	private static final int PEEL_COUNT = 3;
	private static final int ANTIDOTE_ITEM = 182203227;

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
		assertEquals(Set.of(0, 1, 2), rows, "quest 2239 owns a state per journal row");
	}

	@Test
	void acceptChainStaysOnGilungkOnly() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, GILUNGK, "unaccepted", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT1);
		assertPageRoute(definition, GILUNGK, "unaccepted", QuestDialogAction.SELECT1_1,
			QuestDialogPage.SELECT1_1);
		List<QuestTransition> starts = definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id())
			.toList();
		assertEquals(1, starts.size(), "quest 2239 must declare exactly one accept route");
		assertEquals(GILUNGK, ((QuestEvent.TalkToNpc) starts.getFirst().event()).npcId(),
			"quest 2239 accept must stay on Gilungk");
	}

	@Test
	void vovetirnChainAdvancesRowZeroAndCraftsTheAntidote() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, VOVETIRN, "started", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPageRoute(definition, VOVETIRN, "started", QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);
		assertPageRoute(definition, VOVETIRN, "started", QuestDialogAction.SELECT2_1_1,
			QuestDialogPage.SELECT2_1_1);
		List<QuestTransition> advance = dialogRoutes(definition, "started", VOVETIRN,
			QuestDialogAction.SETPRO1);
		assertEquals(1, advance.size(), "quest 2239 row-0 advance must be unique");
		assertEquals("s1", advance.getFirst().targetNode(), "quest 2239 row 0 must advance to row 1");

		assertPageRoute(definition, VOVETIRN, "s1", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT3);
		List<QuestTransition> turnIn = dialogRoutes(definition, "s1", VOVETIRN,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).stream()
			.filter(route -> route.afterCommit().contains(
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_2.id())))
			.toList();
		assertEquals(1, turnIn.size(), "quest 2239 peel turn-in must be unique");
		assertEquals(List.of(new QuestCondition.HasItem(PEEL_ITEM, PEEL_COUNT, true)),
			turnIn.getFirst().conditions(), "quest 2239 must require three peels");
		assertTrue(turnIn.getFirst().actions().contains(new QuestAction.RemoveItem(PEEL_ITEM, PEEL_COUNT)),
			() -> "quest 2239 must consume the peels: " + turnIn.getFirst().actions());

		List<QuestTransition> craft = dialogRoutes(definition, "s1", VOVETIRN, QuestDialogAction.SETPRO2);
		assertEquals(1, craft.size(), "quest 2239 craft route must be unique");
		assertEquals("reward", craft.getFirst().targetNode(), "quest 2239 must advance to row 2");
		assertTrue(craft.getFirst().actions().contains(new QuestAction.GiveItem(ANTIDOTE_ITEM, 1)),
			() -> "quest 2239 must hand out the antidote: " + craft.getFirst().actions());
	}

	@Test
	void rowTwoHandOverStaysOnGilungkWithWindowOne() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, GILUNGK, "reward", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT4);
		List<QuestTransition> handOver = dialogRoutes(definition, "reward", GILUNGK,
			QuestDialogAction.SETPRO3);
		assertEquals(1, handOver.size(), "quest 2239 antidote hand-over must be unique");
		assertEquals(List.of(new QuestCondition.HasItem(ANTIDOTE_ITEM, 1, true)),
			handOver.getFirst().conditions(), "quest 2239 must require the antidote");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), handOver.getFirst().afterCommit(),
			"quest 2239 row 2 must open reward window 1");

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == GILUNGK), "quest 2239 completion must stay on Gilungk");
		assertTrue(completions.stream().flatMap(route -> route.actions().stream())
				.anyMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 2239 must complete at reward index 0");

		boolean strayReward = definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
			.anyMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == VOVETIRN);
		assertTrue(!strayReward, "quest 2239 must not keep a reward window on Vovetirn");
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
		assertEquals(1, stale.size(), "quest 2239 must heal the pre-migration reward save");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
			"quest 2239 heals to the report row");
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
		try (InputStream input = Batch43MalodorAntidoteRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST_ID + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
