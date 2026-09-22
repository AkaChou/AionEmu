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
 * 锁定批次 31：Gelkmaros 三行交接族（21217 / 21244 / 21249）的客户端任务书行阶梯。
 * <p>
 * 族级判据：客户端 quest_summary 恰好 3 行（行 0 交给/转达第一名 NPC、行 1 交给/转达第二名 NPC、行 2 交给或向第三名
 * NPC 报告并领奖），legacy handler 用 {@code defaultCloseDialog(0, 1)} 与 {@code setQuestVar(2) +
 * defaultCloseDialog(2, 2, true, false)} 把 packed step 从 0 推到 1、再推到 2 并置 REWARD；迁移后的旧定义把四个
 * NPC 全部塌陷成“接取 + 一步领奖”，行 1/行 2 永远拿不到 START/REWARD 状态（审计 MISSING_TAIL_ROWS +
 * ROW_WITHOUT_STATE）。本批按 legacy 事件链与客户端页按钮链重建 {@code started(0) -> s1(1) -> reward(2)}，
 * 领奖 owner 收敛到第三行点名的 NPC，并补旧存档 {@code REWARD/var0=0 -> 2} 的 enter-world 自愈边。
 * <p>
 * Locks batch 31: Gelkmaros three-row hand-over shards whose migration collapsed the ladder into a single
 * accept-and-claim step. The ladder, the reward owner and the stale-save heal route are pinned against the
 * legacy event order and the client dialog pages.
 */
class Batch31GelkmarosRowLadderContractTest {

	/** 任务 / 接取 NPC / 行 0 的 NPC / 行 1 的 NPC / 行 2 的领奖 NPC。 */
	private record Contract(int questId, int acceptNpc, int rowZeroNpc, int rowOneNpc, int rewardNpc) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(21217, 799316, 799239, 798713, 799226),
		new Contract(21244, 799317, 799318, 799320, 799317),
		new Contract(21249, 799416, 799416, 799529, 799417)
	);

	private static final int REWARD_ROW = 2;
	private static final int STALE_ROW = 0;
	private static final int REPORT_ITEM_21217 = 182207890;
	private static final int SCROLL_21244 = 182207924;

	/** 行 1 在客户端页上收口的动作：21217/21244 是 SETPRO2，21249 是 SET_SUCCEED。 */
	private static final Map<Integer, QuestDialogAction> ROW_ONE_ACTION = Map.of(
		21217, QuestDialogAction.SETPRO2,
		21244, QuestDialogAction.SETPRO2,
		21249, QuestDialogAction.SET_SUCCEED
	);

	/** 行 2 的领奖页：21217/21244 是 select5，21249 是 select_success(=DEFAULT_SUCCESS)。 */
	private static final Map<Integer, QuestDialogPage> REWARD_PAGE = Map.of(
		21217, QuestDialogPage.SELECT5,
		21244, QuestDialogPage.SELECT5,
		21249, QuestDialogPage.DEFAULT_SUCCESS
	);

	@Test
	void everyShardProjectsThreeJournalRows() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(0, node(definition, "started").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " started row");
			assertEquals(1, node(definition, "s1").projection().variables().get("var0"),
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
			assertEquals(Set.of(0, 1, REWARD_ROW), rows,
				() -> "quest " + contract.questId() + " must own a state per client journal row");
		}
	}

	@Test
	void shardWalksTheLegacyActionLadder() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> firstRow = routes(definition, "started", "s1");
			assertEquals(1, firstRow.size(),
				() -> "quest " + contract.questId() + " advances into journal row 1 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(
					contract.rowZeroNpc(), QuestDialogAction.SETPRO1.id(), 0), firstRow.getFirst().event(),
				() -> "quest " + contract.questId() + " row 0 ends on the client SETPRO1 page");
			assertTrue(firstRow.getFirst().conditions().stream()
					.anyMatch(condition -> condition.equals(new QuestCondition.QuestVariableIs("var0", 0))),
				() -> "quest " + contract.questId() + " row 0 is gated on var0=0");

			List<QuestTransition> secondRow = routes(definition, "s1", "reward");
			assertEquals(1, secondRow.size(),
				() -> "quest " + contract.questId() + " advances into the reward row exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.rowOneNpc(),
					ROW_ONE_ACTION.get(contract.questId()).id(), 0), secondRow.getFirst().event(),
				() -> "quest " + contract.questId() + " row 1 ends on the client hand-over page");
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)),
				secondRow.getFirst().actions().stream()
					.filter(action -> action instanceof QuestAction.SetVariable).toList(),
				() -> "quest " + contract.questId() + " row 1 persists the reward row");
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
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				REWARD_PAGE.get(contract.questId()).id())), showPage.afterCommit(),
				() -> "quest " + contract.questId() + " opens the client reward page");

			QuestTransition claim = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					contract.rewardNpc(), QuestDialogAction.SELECT_QUEST_REWARD.id(), 0)))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " has no reward-window route"));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), claim.afterCommit(),
				() -> "quest " + contract.questId() + " opens the reward window");
		}
	}

	@Test
	void workItemLifecycleFollowsTheLegacyHandler() throws Exception {
		QuestDefinition report = definition(21217).definition();
		assertTrue(report.metadata().questWorkItems()
				.contains(new QuestItemRequirement(REPORT_ITEM_21217, 1)),
			() -> "21217 declares the report as a quest work item");
		assertTrue(claimRoute(report, 799226).actions()
				.contains(new QuestAction.RemoveItem(REPORT_ITEM_21217, 1)),
			() -> "21217 consumes the report when the reward window opens");

		QuestDefinition scroll = definition(21244).definition();
		assertTrue(scroll.metadata().questWorkItems()
				.contains(new QuestItemRequirement(SCROLL_21244, 1)),
			() -> "21244 declares the sealed scroll as a quest work item");
		assertTrue(routes(scroll, "s1", "reward").getFirst().actions()
				.contains(new QuestAction.GiveItem(SCROLL_21244, 1)),
			() -> "21244 hands over the scroll on journal row 1");
		assertTrue(claimRoute(scroll, 799317).actions()
				.contains(new QuestAction.RemoveItem(SCROLL_21244, 1)),
			() -> "21244 consumes the scroll when the reward window opens");
	}

	@Test
	void staleCollapsedRewardSavesHealToTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", STALE_ROW))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + contract.questId() + " heal route for the collapsed row " + STALE_ROW);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
				() -> "quest " + contract.questId() + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + contract.questId() + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", STALE_ROW)), heal).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " healed status");
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " healed journal row");
		}
	}

	@Test
	void collapsedHandoverNoLongerJumpsStraightToReward() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep the collapsed started -> reward jump");
			assertTrue(routes(definition, "s1", "s1").stream()
					.noneMatch(route -> route.actions().contains(new QuestAction.SetVariable("var0", 0))),
				() -> "quest " + contract.questId() + " must not write the stale row from the ladder");
		}
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
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch31GelkmarosRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
