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
 * 锁定批次 45：三行族 1938（天族顺序阶梯）与 2922（魔族二选一分支阶梯）的行/状态投影。
 * <p>
 * 1938 客户端任务书三行、槽位 %0/%3/%6：行 0 和 Shugo_LF3_1 798069 对话、行 1 调查
 * LF3_Nakaching_E 805836、行 2 向 Likasas 203703 报告领奖（select5 页）。
 * 2922 客户端任务书三行、槽位 %0/%3/%6：行 0 接受 Daskair 204261 的请求、行 1 向 Shugo_DC1_3
 * 798058 定做箱子、行 2 向 Lanse 204108 定做耳坠；两个分支互斥，分别投影成
 * reward1(var0=1) / reward(var0=2)。
 * </p>
 * <p>
 * Locks batch 45: one state per journal row for 1938 and for the mutually exclusive branch rows of
 * 2922, with the branch owners limited to the client-declared NPCs.
 * </p>
 */
class Batch45ThreeRowLadderAndBranchRewardContractTest {

	private static final int Q1938 = 1938;
	private static final int Q1938_LIKASAS = 203703;
	private static final int Q1938_SHUGOS = 798069;
	private static final int Q1938_NAKACHING = 805836;

	private static final int Q2922 = 2922;
	private static final int Q2922_DASKAIR = 204261;
	private static final int Q2922_SHUGODC = 798058;
	private static final int Q2922_LANSE = 204108;

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		QuestDefinition blackCloud = definition(Q1938);
		assertNode(blackCloud, "started", QuestStatus.START, 0);
		assertNode(blackCloud, "s1", QuestStatus.START, 1);
		assertNode(blackCloud, "reward", QuestStatus.REWARD, 2);
		assertEquals(Set.of(0, 1, 2), journalRows(blackCloud, 3),
			"quest 1938 owns a state per journal row");

		QuestDefinition gift = definition(Q2922);
		assertNode(gift, "started", QuestStatus.START, 0);
		assertNode(gift, "reward1", QuestStatus.REWARD, 1);
		assertNode(gift, "reward", QuestStatus.REWARD, 2);
		assertEquals(Set.of(0, 1, 2), journalRows(gift, 3),
			"quest 2922 projects both exclusive branch rows to reward states");
	}

	@Test
	void blackCloudRowsFollowTheShugoThenNakachingChain() throws Exception {
		QuestDefinition definition = definition(Q1938);
		assertPageRoute(definition, Q1938_SHUGOS, "started", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPageRoute(definition, Q1938_SHUGOS, "started", QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);
		assertAdvance(definition, Q1938_SHUGOS, "started", QuestDialogAction.SETPRO1, "s1");

		assertPageRoute(definition, Q1938_NAKACHING, "s1", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT3);
		assertPageRoute(definition, Q1938_NAKACHING, "s1", QuestDialogAction.SELECT3_1,
			QuestDialogPage.SELECT3_1);
		assertAdvance(definition, Q1938_NAKACHING, "s1", QuestDialogAction.SETPRO2, "reward");
	}

	@Test
	void blackCloudReportsAndRewardsOnlyOnLikasas() throws Exception {
		QuestDefinition definition = definition(Q1938);
		assertPageRoute(definition, Q1938_LIKASAS, "reward", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT5);
		List<QuestTransition> reward = dialogRoutes(definition, "reward", Q1938_LIKASAS,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(1, reward.size(), "quest 1938 report route must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
			"quest 1938 row 2 must open reward window 1");

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == Q1938_LIKASAS), "quest 1938 completion must stay on Likasas");
		assertTrue(completions.stream().flatMap(route -> route.actions().stream())
				.anyMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 1938 must complete at reward index 0");
	}

	@Test
	void giftChoicesFreezeTheBoxOrTheEarringRow() throws Exception {
		QuestDefinition definition = definition(Q2922);
		assertAdvance(definition, Q2922_DASKAIR, "started", QuestDialogAction.SETPRO10, "reward1");
		assertAdvance(definition, Q2922_DASKAIR, "started", QuestDialogAction.SETPRO20, "reward");

		assertPageRoute(definition, Q2922_SHUGODC, "reward1", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPageRoute(definition, Q2922_SHUGODC, "reward1", QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertPageRoute(definition, Q2922_LANSE, "reward", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT3);
		assertPageRoute(definition, Q2922_LANSE, "reward", QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
	}

	@Test
	void giftWrongNpcPagesAndCompletionStayOnTheChosenBranch() throws Exception {
		QuestDefinition definition = definition(Q2922);
		assertPageRoute(definition, Q2922_LANSE, "reward1", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT3_1);
		assertPageRoute(definition, Q2922_SHUGODC, "reward", QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2_1);
		assertEquals(1, dialogRoutes(definition, "reward1", Q2922_LANSE,
			QuestDialogAction.FINISH_DIALOG).size(),
			"quest 2922 must close the wrong-NPC page on the box branch");
		assertEquals(1, dialogRoutes(definition, "reward", Q2922_SHUGODC,
			QuestDialogAction.FINISH_DIALOG).size(),
			"quest 2922 must close the wrong-NPC page on the earring branch");

		assertTrue(completion(definition, "reward1", "complete").stream()
				.allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == Q2922_SHUGODC),
			"quest 2922 box branch must complete on Shugo_DC1_3");
		assertTrue(completion(definition, "reward", "complete").stream()
				.allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == Q2922_LANSE),
			"quest 2922 earring branch must complete on Lanse");
		assertTrue(definition.transitions().stream()
				.flatMap(route -> route.actions().stream())
				.filter(action -> action instanceof QuestAction.CompleteQuest)
				.allMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 2922 branches share the single reward group index 0");
	}

	@Test
	void staleSavesHealBackToTheirJournalRow() throws Exception {
		QuestDefinition blackCloud = definition(Q1938);
		assertHeal(blackCloud, QuestStatus.REWARD, 0, "reward", 2);

		QuestDefinition gift = definition(Q2922);
		assertHeal(gift, QuestStatus.START, 10, "reward1", 1);
		assertHeal(gift, QuestStatus.START, 20, "reward", 2);
		assertHeal(gift, QuestStatus.REWARD, 10, "reward1", 1);
		assertHeal(gift, QuestStatus.REWARD, 20, "reward", 2);
		assertHeal(gift, QuestStatus.REWARD, 0, "reward1", 1);
	}

	private static Set<Integer> journalRows(QuestDefinition definition, int lastRow) {
		Set<Integer> rows = new LinkedHashSet<>();
		for (QuestNode candidate : definition.nodes()) {
			Integer row = candidate.projection().variables().get("var0");
			QuestStatus status = candidate.projection().status();
			if (row == null || row > lastRow || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
				continue;
			}
			rows.add(row);
		}
		return rows;
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int row) {
		QuestNode node = definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), () -> "node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"), () -> "node " + label + " row");
	}

	private static void assertHeal(QuestDefinition definition, QuestStatus status, int staleRow,
		String target, int healedRow) {
		List<QuestTransition> heal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> target.equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(status),
				new QuestCondition.QuestVariableIs("var0", staleRow))))
			.toList();
		assertEquals(1, heal.size(),
			() -> "must heal the " + status + "/var0=" + staleRow + " save into " + target);
		assertEquals(List.of(new QuestAction.SetVariable("var0", healedRow)), heal.getFirst().actions(),
			() -> status + "/var0=" + staleRow + " heals to journal row " + healedRow);
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

	private static List<QuestTransition> completion(QuestDefinition definition, String source,
		String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.toList();
	}

	private static QuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch45ThreeRowLadderAndBranchRewardContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
