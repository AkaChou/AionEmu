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
 * 锁定批次 40：三行「接取 -&gt; 和行 0 NPC 对话 -&gt; 和行 1 NPC 对话 -&gt; 向行 2 NPC 报告领奖」族
 * （11072/21081/24150）。
 * <p>
 * 三家客户端任务书都是三行、槽位 %0/%3/%6，页链同型：接取 NPC 的 `select1` 链，
 * 行 0 的 NPC 走 `select2 -&gt; select2_1 -&gt; SETPRO1`，行 1 的 NPC 走
 * `select3 -&gt; select3_1 -&gt; SETPRO2`，行 2 的 NPC 走 `select5 -&gt; SELECT_QUEST_REWARD`。
 * 迁移把三家都塌陷成“每个任务 NPC 都能接取 + 都能领奖”的扁平模板，只有 SELECT2/SETPRO1，
 * 行 1/行 2 没有状态，21081/24150 还会在行 0 的 NPC 处直接进领奖态。
 * </p>
 * <p>
 * Locks batch 40: one talking owner per journal row. Accept stays on the accept NPC, rows 0/1 advance
 * through SETPRO1/SETPRO2, and row 2 owns the client select5 page plus reward window 1.
 * </p>
 */
class Batch40ThreeNpcTalkLadderContractTest {

	private record TalkQuest(int questId, int acceptNpc, int row0Npc, int row1Npc, int row2Npc) {
	}

	private static final List<TalkQuest> FAMILY = List.of(
		new TalkQuest(11072, 798937, 798907, 798960, 798937),
		new TalkQuest(21081, 799225, 799332, 799217, 799202),
		new TalkQuest(24150, 204702, 204733, 204734, 204702)
	);

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertNode(definition, contract.questId(), "started", QuestStatus.START, 0);
			assertNode(definition, contract.questId(), "s1", QuestStatus.START, 1);
			assertNode(definition, contract.questId(), "reward", QuestStatus.REWARD, 2);

			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode candidate : definition.nodes()) {
				Integer row = candidate.projection().variables().get("var0");
				QuestStatus status = candidate.projection().status();
				if (row == null || row > 2 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
			}
			assertEquals(Set.of(0, 1, 2), rows,
				() -> "quest " + contract.questId() + " owns a state per journal row");
		}
	}

	@Test
	void acceptChainStaysOnTheAcceptNpcOnly() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> starts = definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_1.id())
				.toList();
			assertEquals(1, starts.size(),
				() -> "quest " + contract.questId() + " must declare exactly one accept route");
			QuestEvent.TalkToNpc accept = (QuestEvent.TalkToNpc) starts.getFirst().event();
			assertEquals(contract.acceptNpc(), accept.npcId(),
				() -> "quest " + contract.questId() + " accept must stay on the accept NPC");
			assertEquals("started", starts.getFirst().targetNode(),
				() -> "quest " + contract.questId() + " accept must enter the first row state");
		}
	}

	@Test
	void rowOwnersDriveTheClientPageChain() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			assertPageRoute(definition, contract.row0Npc(), "started",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPageRoute(definition, contract.row0Npc(), "started",
				QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertAdvance(definition, contract.row0Npc(), "started", QuestDialogAction.SETPRO1, "s1");

			assertPageRoute(definition, contract.row1Npc(), "s1",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
			assertPageRoute(definition, contract.row1Npc(), "s1",
				QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
			assertAdvance(definition, contract.row1Npc(), "s1", QuestDialogAction.SETPRO2, "reward");

			assertPageRoute(definition, contract.row2Npc(), "reward",
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
			List<QuestTransition> reward = dialogRoutes(definition, "reward", contract.row2Npc(),
				QuestDialogAction.SELECT_QUEST_REWARD);
			assertEquals(1, reward.size(),
				() -> "quest " + contract.questId() + " reward route must be unique");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), reward.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " row 2 must open reward window 1");
		}
	}

	@Test
	void rewardOwnerHoldsTheCompletionAndOtherNpcsDoNot() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.toList();
			assertTrue(completions.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.row2Npc()),
				() -> "quest " + contract.questId() + " completion must stay on the row-2 NPC");
			assertTrue(completions.stream().flatMap(route -> route.actions().stream())
					.anyMatch(new QuestAction.CompleteQuest(0)::equals),
				() -> "quest " + contract.questId() + " must complete at reward index 0");
			Set<Integer> nonOwnerNpcs = Set.of(contract.row0Npc(), contract.row1Npc());
			boolean strayReward = definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
				.anyMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& nonOwnerNpcs.contains(talk.npcId()));
			assertTrue(!strayReward,
				() -> "quest " + contract.questId() + " must not keep row-0/row-1 reward windows");
		}
	}

	@Test
	void staleRewardSaveHealsToRowTwo() throws Exception {
		for (TalkQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> stale = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0))))
				.toList();
			assertEquals(1, stale.size(),
				() -> "quest " + contract.questId() + " must heal the pre-migration reward save");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
				() -> "quest " + contract.questId() + " heals to the report row");
		}
	}

	private static void assertNode(QuestDefinition definition, int questId, String label,
		QuestStatus status, int row) {
		QuestNode node = definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(),
			() -> "quest " + questId + " node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"),
			() -> "quest " + questId + " node " + label + " row");
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

	private static void assertAdvance(QuestDefinition definition, int npcId, String source,
		QuestDialogAction action, String target) {
		List<QuestTransition> routes = dialogRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(),
			() -> "advance " + source + " + npc " + npcId + " + " + action + " must be unique");
		assertEquals(target, routes.getFirst().targetNode(),
			() -> "advance " + source + " + npc " + npcId + " + " + action + " target");
		assertTrue(routes.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			() -> "advance " + source + " + npc " + npcId + " + " + action + " refreshes visibility");
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == action.id())
			.toList();
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch40ThreeNpcTalkLadderContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
