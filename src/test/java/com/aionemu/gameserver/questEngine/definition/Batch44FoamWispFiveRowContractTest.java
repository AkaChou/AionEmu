package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 44：发光体玻璃瓶族 11118 的五行状态阶梯。
 * <p>
 * 客户端任务书五行、槽位 %0/%3/%6/%9/%12：行 0 向 Cinisca 征询、行 1 交 20 个发光体换玻璃瓶、
 * 行 2 在妹妹 Shulack_LF4_2 身边打开瓶子、行 3 和妹妹对话、行 4 回哥哥 Shulack_LF4_1 报告领奖。
 * 迁移前的 Java handler 声明 {@code npc_ids = {798985, 798963, 798986}}，但正文误写 798984
 * （Shugo_LF4_5，且从未注册监听），迁移把该笔误固化；本批按客户端任务书与 npc_ids 修正为
 * 798986，并把 reward 投影从 0 提到 4。
 * </p>
 * <p>
 * Locks batch 44: five journal rows with one state each, the sister NPC id corrected to 798986,
 * and both pre-migration REWARD saves healed to row 4.
 * </p>
 */
class Batch44FoamWispFiveRowContractTest {

	private static final int QUEST_ID = 11118;
	private static final int SEIJIN = 798985;
	private static final int CINISCA = 798963;
	private static final int SETZKIKI = 798986;
	private static final int WRONG_OWNER = 798984;
	private static final int WISP_ITEM = 182206794;
	private static final int WISP_COUNT = 20;
	private static final int BOTTLE_ITEM = 182206795;

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "s1", QuestStatus.START, 1);
		assertNode(definition, "s2", QuestStatus.START, 2);
		assertNode(definition, "s3", QuestStatus.START, 3);
		assertNode(definition, "reward", QuestStatus.REWARD, 4);

		Set<Integer> rows = new LinkedHashSet<>();
		for (QuestNode candidate : definition.nodes()) {
			Integer row = candidate.projection().variables().get("var0");
			QuestStatus status = candidate.projection().status();
			if (row == null || row > 4 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
				continue;
			}
			rows.add(row);
		}
		assertEquals(Set.of(0, 1, 2, 3, 4), rows, "quest 11118 owns a state per journal row");
	}

	@Test
	void ciniscaChainAdvancesTheFirstTwoRows() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, CINISCA, "started", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertAdvance(definition, CINISCA, "started", QuestDialogAction.SETPRO1, "s1");

		assertPageRoute(definition, CINISCA, "s1", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		List<QuestTransition> turnIn = dialogRoutes(definition, "s1", CINISCA,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).stream()
			.filter(route -> "s2".equals(route.targetNode()))
			.toList();
		assertEquals(1, turnIn.size(), "quest 11118 wisp turn-in must be unique");
		assertEquals(List.of(new QuestCondition.HasItem(WISP_ITEM, WISP_COUNT, true)),
			turnIn.getFirst().conditions(), "quest 11118 must require twenty wisps");
		assertTrue(turnIn.getFirst().actions().contains(new QuestAction.RemoveItem(WISP_ITEM, WISP_COUNT)),
			() -> "quest 11118 must consume the wisps: " + turnIn.getFirst().actions());
		assertTrue(turnIn.getFirst().actions().contains(new QuestAction.GiveItem(BOTTLE_ITEM, 1)),
			() -> "quest 11118 must hand out the bottle: " + turnIn.getFirst().actions());

		// CHECK 成功时状态已切到 s2，而客户端 check_user_item_ok 页的唯一按钮是“结束对话”，
		// 该按钮必须在 s2 上有路由（否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE）。
		List<QuestTransition> okClose = dialogRoutes(definition, "s2", CINISCA, QuestDialogAction.FINISH_DIALOG);
		assertEquals(1, okClose.size(),
			"quest 11118 must route the check_user_item_ok close button on row 2");
		assertTrue(okClose.getFirst().afterCommit().contains(new AfterCommitAction.CloseDialog()),
			"quest 11118 check_user_item_ok close button must close the dialog");
	}

	@Test
	void sisterOwnsRowsTwoAndThreeInsteadOfTheLegacyTypo() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> sisterRoutes = definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == SETZKIKI)
			.toList();
		assertFalse(sisterRoutes.isEmpty(), "quest 11118 must route the sister Shulack_LF4_2 798986");
		assertTrue(definition.transitions().stream()
				.noneMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == WRONG_OWNER),
			"quest 11118 must not keep the legacy Shugo_LF4_5 798984 owner");

		assertAdvance(definition, SETZKIKI, "s2", QuestDialogAction.QUEST_SELECT, "s3");
		assertAdvance(definition, SETZKIKI, "s2", QuestDialogAction.USE_OBJECT, "s3");
		assertPageRoute(definition, SETZKIKI, "s3", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertAdvance(definition, SETZKIKI, "s3", QuestDialogAction.SET_SUCCEED, "reward");
	}

	@Test
	void brotherOwnsTheFinalReportAndReward() throws Exception {
		QuestDefinition definition = definition().definition();
		assertPageRoute(definition, SEIJIN, "reward", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		assertPageRoute(definition, SEIJIN, "reward", QuestDialogAction.USE_OBJECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		List<QuestTransition> reward = dialogRoutes(definition, "reward", SEIJIN,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(1, reward.size(), "quest 11118 reward route must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
			"quest 11118 row 4 must open reward window 1");

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == SEIJIN), "quest 11118 completion must stay on the brother");
		assertTrue(completions.stream().flatMap(route -> route.actions().stream())
				.anyMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 11118 must complete at reward index 0");
	}

	@Test
	void bothStaleRewardSavesHealToRowFour() throws Exception {
		QuestDefinition definition = definition().definition();
		for (int staleRow : List.of(0, 3)) {
			List<QuestTransition> heal = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", staleRow))))
				.toList();
			assertEquals(1, heal.size(), "quest 11118 must heal the REWARD/var0=" + staleRow + " save");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), heal.getFirst().actions(),
				() -> "quest 11118 REWARD/var0=" + staleRow + " heals to row 4");
		}
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
		assertTrue(routes.getFirst().afterCommit().contains(
				new AfterCommitAction.ShowQuestDialog(page.id())),
			() -> "route " + source + " + npc " + npcId + " + " + action + " page");
	}

	private static void assertAdvance(QuestDefinition definition, int npcId, String source,
		QuestDialogAction action, String target) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "advance " + source + " + npc " + npcId + " + " + action + " must be unique");
		assertEquals(target, routes.getFirst().targetNode(),
			() -> "advance " + source + " + npc " + npcId + " + " + action + " target");
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
		try (InputStream input = Batch44FoamWispFiveRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST_ID + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
