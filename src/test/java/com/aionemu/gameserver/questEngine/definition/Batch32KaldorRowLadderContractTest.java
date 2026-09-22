package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 32：卡多尔迎新族（13800 New Lands to Behold / 23800 A Full New World）的两阶段任务书行阶梯。
 * <p>
 * 族级判据：客户端 quest_summary 恰好 3 行（行 0“带上书信去找阵营传送点”、行 1“移动到卡多尔，和迎宾 NPC 对话”、
 * 行 2“再次和迎宾 NPC 对话并领奖”）；客户端页链给出两段推进（select2 -&gt; SELECT2_1 -&gt; SETPRO1，
 * select3 -&gt; SELECT3_1 -&gt; SELECT3_1_1 -&gt; SETPRO2）与领奖页 select5 -&gt; SELECT_QUEST_REWARD；
 * retail {@code data_driven_quest} 只登记传送点一个 TALK step，领奖合同是 REWARD 态 {@code 31 -> DEFAULT_SUCCESS -> 1009}；
 * 迁移前 handler 在传送点的 STEP_TO_1 把 packed step 推到 1、在迎宾 NPC 的 SELECT_REWARD 直接置 REWARD（落盘 1，
 * 跳过 select3 段）。旧定义把三个 NPC 全塌陷成“接取 + 一步领奖”，reward 投影停在 0，行 1/行 2 永远拿不到状态
 * （审计 MISSING_TAIL_ROWS + ROW_WITHOUT_STATE）。本批按客户端页链重建 {@code started(0) -> s1(1) -> reward(2)}，
 * 领奖 owner 收敛到末行点名的迎宾 NPC，并补旧存档 {@code REWARD/var0=0} 与 {@code REWARD/var0=1} 两条 enter-world 自愈边。
 * <p>
 * Locks batch 32: the Kaldor welcome shards whose migration collapsed the three-row journal into a single
 * accept-and-claim step. The client page chain owns two advance actions, so the ladder, the reward owner,
 * the letter lifecycle and both stale-save heal routes are pinned against the client journal.
 */
class Batch32KaldorRowLadderContractTest {

	/** 任务 / 接取 NPC / 行 0 的传送点 NPC / 行 1 与行 2 的迎宾 NPC / 书信物品。 */
	private record Contract(int questId, int acceptNpc, int rowZeroNpc, int rewardNpc, int letterItem) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(13800, 804699, 804782, 802431, 182215482),
		new Contract(23800, 804719, 804753, 802433, 182215490)
	);

	private static final int REWARD_ROW = 2;
	private static final int MIDDLE_ROW = 1;
	private static final int STALE_ROW = 0;

	@Test
	void everyShardProjectsThreeJournalRows() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(0, node(definition, "started").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " started row");
			assertEquals(MIDDLE_ROW, node(definition, "s1").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " s1 row");
			assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " reward row");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(QuestStatus.START, node(definition, "s1").projection().status(),
				() -> "quest " + contract.questId() + " s1 node status");
		}
	}

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode node : definition.nodes()) {
				Integer row = node.projection().variables().get("var0");
				QuestStatus status = node.projection().status();
				if (row == null || row > REWARD_ROW
						|| (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
				QuestStatus expected = row == REWARD_ROW ? QuestStatus.REWARD : QuestStatus.START;
				assertEquals(expected, status,
					() -> "quest " + contract.questId() + " row " + row + " node status");
			}
			assertEquals(Set.of(0, MIDDLE_ROW, REWARD_ROW), rows,
				() -> "quest " + contract.questId() + " must own a state per client journal row");
		}
	}

	@Test
	void twoStageClientPageLadderPersistsEachRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();

			List<QuestTransition> firstRow = routes(definition, "started", "s1");
			assertEquals(1, firstRow.size(),
				() -> "quest " + contract.questId() + " advances into journal row 1 exactly once");
			QuestTransition handOver = firstRow.getFirst();
			assertEquals(new QuestEvent.TalkToNpc(
					contract.rowZeroNpc(), QuestDialogAction.SETPRO1.id(), 0), handOver.event(),
				() -> "quest " + contract.questId() + " row 0 ends on the client SETPRO1 page");
			assertTrue(handOver.conditions().stream()
					.anyMatch(condition -> condition.equals(new QuestCondition.QuestVariableIs("var0", STALE_ROW))),
				() -> "quest " + contract.questId() + " row 0 is gated on var0=0");
			assertEquals(List.of(new QuestAction.SetVariable("var0", MIDDLE_ROW)), handOver.actions(),
				() -> "quest " + contract.questId() + " row 0 persists the middle row");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), handOver.afterCommit(),
				() -> "quest " + contract.questId() + " row 0 stays packet-only before the briefing");

			List<QuestTransition> secondRow = routes(definition, "s1", "reward");
			assertEquals(1, secondRow.size(),
				() -> "quest " + contract.questId() + " advances into the reward row exactly once");
			QuestTransition briefing = secondRow.getFirst();
			assertEquals(new QuestEvent.TalkToNpc(
					contract.rewardNpc(), QuestDialogAction.SETPRO2.id(), 0), briefing.event(),
				() -> "quest " + contract.questId() + " row 1 ends on the client SETPRO2 page");
			assertTrue(briefing.conditions().stream()
					.anyMatch(condition -> condition.equals(new QuestCondition.QuestVariableIs("var0", MIDDLE_ROW))),
				() -> "quest " + contract.questId() + " row 1 is gated on var0=1");
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), briefing.actions().stream()
					.filter(action -> action instanceof QuestAction.SetVariable).toList(),
				() -> "quest " + contract.questId() + " row 1 persists the reward row");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), briefing.afterCommit(),
				() -> "quest " + contract.questId() + " row 1 refreshes the quest list before the claim");
		}
	}

	@Test
	void rewardOwnerIsTheThirdJournalRowNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			Set<Integer> completionNpcs = new LinkedHashSet<>();
			definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.forEach(route -> {
					if (route.event() instanceof QuestEvent.TalkToNpc talk) {
						completionNpcs.add(talk.npcId());
					}
				});
			assertEquals(Set.of(contract.rewardNpc()), completionNpcs,
				() -> "quest " + contract.questId() + " completion owner stays on the reward-row NPC");
			assertTrue(routes(definition, "reward", "complete").size() >= 1,
				() -> "quest " + contract.questId() + " keeps a reward -> complete route");
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> contract.rowZeroNpc() == talkNpc(route)
						&& "complete".equals(route.targetNode())),
				() -> "quest " + contract.questId() + " must not let the zone teleport NPC claim");
		}
	}

	@Test
	void rewardWindowButtonsKeepTheirClientRoutes() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestTransition showPage = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					contract.rewardNpc(), QuestDialogAction.QUEST_SELECT.id(), 0)))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " has no reward-page route"));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
				showPage.afterCommit(),
				() -> "quest " + contract.questId() + " opens the client reward page");

			QuestTransition claim = claimRoute(definition, contract.rewardNpc());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), claim.afterCommit(),
				() -> "quest " + contract.questId() + " opens the reward window");
		}
	}

	@Test
	void letterLifecycleFollowsTheClientHandover() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(definition.metadata().questWorkItems()
					.contains(new QuestItemRequirement(contract.letterItem(), 1)),
				() -> "quest " + contract.questId() + " declares the letter as a quest work item");
			assertTrue(routes(definition, "unaccepted", "started").stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
						contract.acceptNpc(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id(), 0)))
					.findFirst().orElseThrow(() -> new AssertionError(
						"quest " + contract.questId() + " has no accept route"))
					.actions().contains(new QuestAction.GiveItem(contract.letterItem(), 1)),
				() -> "quest " + contract.questId() + " hands over the letter on accept");
			assertTrue(claimRoute(definition, contract.rewardNpc()).actions()
					.contains(new QuestAction.RemoveItem(contract.letterItem(), 1)),
				() -> "quest " + contract.questId() + " consumes the letter when the reward window opens");
		}
	}

	@Test
	void staleCollapsedRewardSavesHealToTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> recoveries = enterWorldRecoveries(compiled.definition());
			for (int staleRow : List.of(STALE_ROW, MIDDLE_ROW)) {
				List<QuestTransition> matches = recoveries.stream()
					.filter(route -> route.conditions().equals(List.of(
						new QuestCondition.StatusIs(QuestStatus.REWARD),
						new QuestCondition.QuestVariableIs("var0", staleRow))))
					.toList();
				assertEquals(1, matches.size(),
					() -> "quest " + contract.questId() + " heal route for the collapsed row " + staleRow);
				QuestTransition heal = matches.getFirst();
				assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
					() -> "quest " + contract.questId() + " heal actions for row " + staleRow);
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
					() -> "quest " + contract.questId() + " heal after-commit for row " + staleRow);
				assertNull(heal.priority(),
					() -> "quest " + contract.questId() + " heal priority for row " + staleRow);

				Map<String, Integer> staleVariables = new LinkedHashMap<>(
					compiled.definition().progressLayout().unpack(0));
				staleVariables.put("var0", staleRow);
				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, staleVariables), heal).orElseThrow();
				assertEquals(QuestStatus.REWARD, plan.nextStatus(),
					() -> "quest " + contract.questId() + " healed status for row " + staleRow);
				assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
					() -> "quest " + contract.questId() + " healed journal row for row " + staleRow);
			}
		}
	}

	@Test
	void collapsedHandoverNoLongerJumpsStraightToReward() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep the collapsed started -> reward jump");
			assertTrue(routes(definition, "started", "started").stream()
					.noneMatch(route -> route.actions().contains(new QuestAction.SetVariable("var0", REWARD_ROW))),
				() -> "quest " + contract.questId() + " must not jump rows inside the accept row");
			assertTrue(routes(definition, "s1", "s1").stream()
					.noneMatch(route -> route.actions().contains(new QuestAction.SetVariable("var0", STALE_ROW))),
				() -> "quest " + contract.questId() + " must not write the stale row from the ladder");
		}
	}

	private static int talkNpc(QuestTransition route) {
		return route.event() instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : -1;
	}

	private static QuestTransition claimRoute(QuestDefinition definition, int npcId) {
		return routes(definition, "reward", "reward").stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
				npcId, QuestDialogAction.SELECT_QUEST_REWARD.id(), 0)))
			.findFirst().orElseThrow(() -> new AssertionError("missing claim route for " + npcId));
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static List<QuestTransition> enterWorldRecoveries(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(variables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch32KaldorRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
