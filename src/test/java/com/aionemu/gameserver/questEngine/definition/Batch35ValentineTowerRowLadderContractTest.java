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
 * 锁定批次 35：情人节巧克力塔镜像族（50019 Elyos / 51019 Asmodian）的三行任务书阶梯。
 * <p>
 * 族判据：客户端 quest_q30504/quest_q30554.html 的 quest_summary 三行、可见槽位 %0/%3/%6（「槽位 = 3 × 状态号」）
 * ——行 0“找到并救出 Paios(799536)”（挪开柱子物件 701098 = IDYun_Column_Q30504）、行 1“和 Paios 对话”（客户端
 * select2 页，按钮 SET_SUCCEED）、行 2“和 Lition(205438) 对话”（客户端 select_success 页，按钮
 * SELECT_QUEST_REWARD；quest_complete 文案是“把派奥斯的死讯告诉李迪安”）。retail 只登记一个
 * {@code ACTION action_ids="701098"} 步骤，领奖合同是 REWARD 态 31 -&gt; DEFAULT_SUCCESS(10002)、1009 -&gt; 奖励窗口。
 * <p>
 * 迁移前既无 Java handler 也无脚本，迁移把三行塌陷成「started 传 SET_SUCCEED(205438) 直达 reward」（而 205438 的
 * 客户端页里没有 SET_SUCCEED 按钮）、reward 投影抄成 0、{@code var0} 只有 1 bit（max=1）、柱子物件从未接进 IR。
 * 本批重建 started(0)/s1(1)/reward(2)、owner 收敛到客户端末行点名的 205438，并补 0/1 两条自愈边。
 * <p>
 * Locks batch 34: the Rentus Base Paios rescue pair, whose journal owns three rows while the migrated
 * definition collapsed them into a single unreachable SET_SUCCEED jump and dropped the column object.
 */
class Batch35ValentineTowerRowLadderContractTest {

	/** 任务 / 巧克力塔物件 / 交付物 / 术古（客户端末行点名的领奖 NPC）。 */
	private record Contract(int questId, int towerObject, int chocolate, int npc) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(50019, 701466, 182215172, 202549),
		new Contract(51019, 701467, 182215178, 202549)
	);

	private static final int REWARD_NPC = 202549;
	private static final int REWARD_ROW = 2;
	private static final int MIDDLE_ROW = 1;
	private static final int STALE_ROW = 0;

	@Test
	void everyShardProjectsThreeJournalRows() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(STALE_ROW, node(definition, "started").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " started row");
			assertEquals(MIDDLE_ROW, node(definition, "s1").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " s1 row");
			assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " reward row");
			assertEquals(QuestStatus.START, node(definition, "s1").projection().status(),
				() -> "quest " + contract.questId() + " s1 status");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward status");
			assertTrue(definition.progressLayout().field("var0").maxValue() >= REWARD_ROW,
				() -> "quest " + contract.questId() + " must widen var0 so the reward row fits");
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
				assertEquals(row == REWARD_ROW ? QuestStatus.REWARD : QuestStatus.START, status,
					() -> "quest " + contract.questId() + " row " + row + " status");
			}
			assertEquals(Set.of(0, MIDDLE_ROW, REWARD_ROW), rows,
				() -> "quest " + contract.questId() + " owns a state per journal row");
		}
	}

	@Test
	void turnInAdvancesIntoTheMiddleRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> turnIn = routes(definition, "started", "s1");
			assertEquals(1, turnIn.size(),
				() -> "quest " + contract.questId() + " advances into row 1 exactly once (item report)");
			assertEquals(new QuestEvent.TalkToNpc(
				contract.npc(), QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0), turnIn.getFirst().event(),
				() -> "quest " + contract.questId() + " row 0 ends on the client hand-in button");
			assertTrue(turnIn.getFirst().actions().contains(
					new QuestAction.RemoveItem(contract.chocolate(), 3)),
				() -> "quest " + contract.questId() + " consumes the collected chocolate");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
				turnIn.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " shows the client check_ok page");
			assertTrue(turnIn.stream().flatMap(route -> route.conditions().stream())
					.anyMatch(condition -> condition instanceof QuestCondition.HasItem hasItem
						&& hasItem.itemId() == contract.chocolate()),
				() -> "quest " + contract.questId() + " gates the item report on the collected chocolate");
			assertTrue(definition.metadata().questWorkItems().stream()
					.noneMatch(item -> item.count() == 0),
				() -> "quest " + contract.questId() + " declares no empty work item");
		}
	}

	@Test
	void towerDecorationFinishesTheMiddleRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> decorate = routes(definition, "s1", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					contract.towerObject(), QuestDialogAction.USE_OBJECT.id(), 0)))
				.toList();
			assertEquals(1, decorate.size(),
				() -> "quest " + contract.questId() + " wires the chocolate tower exactly once");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), decorate.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " refreshes the journal after decorating");

			List<QuestTransition> fallback = routes(definition, "s1", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					REWARD_NPC, QuestDialogAction.QUEST_SELECT.id(), 0)))
				.toList();
			assertEquals(1, fallback.size(),
				() -> "quest " + contract.questId() + " keeps a soft-lock guard when the tower is missing");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				fallback.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " opens the reward entry page from the guard");
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
			assertEquals(Set.of(REWARD_NPC), completionNpcs,
				() -> "quest " + contract.questId() + " completes on the journal reward-row NPC");
			assertTrue(routes(definition, "reward", "complete").size() >= 1,
				() -> "quest " + contract.questId() + " keeps a reward -> complete route");
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> contract.towerObject() == talkNpc(route)
						&& "complete".equals(route.targetNode())),
				() -> "quest " + contract.questId() + " must not let the tower claim");
		}
	}

	@Test
	void rewardEntryAndClaimSeatOnTheJournalNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestTransition entry = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					REWARD_NPC, QuestDialogAction.QUEST_SELECT.id(), 0)))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " has no reward entry page"));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				entry.afterCommit(),
				() -> "quest " + contract.questId() + " opens the 10002 entry page");
			assertTrue(routes(definition, "reward", "reward").stream()
				.anyMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(
					REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD.id(), 0))),
				() -> "quest " + contract.questId() + " keeps the 1009 claim route on the same NPC");
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
					() -> "quest " + contract.questId() + " heal route for stale row " + staleRow);
				QuestTransition heal = matches.getFirst();
				assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
					() -> "quest " + contract.questId() + " heal writes the reward row");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
					() -> "quest " + contract.questId() + " heal refreshes the journal");
				assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");

				Map<String, Integer> stale = new LinkedHashMap<>(
					compiled.definition().progressLayout().unpack(0));
				stale.put("var0", staleRow);
				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(compiled, stale), heal)
					.orElseThrow();
				assertEquals(QuestStatus.REWARD, plan.nextStatus(),
					() -> "quest " + contract.questId() + " healed status for row " + staleRow);
				assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
					() -> "quest " + contract.questId() + " healed row for " + staleRow);
			}
		}
	}

	@Test
	void collapsedJumpNoLongerSkipsTheJournalRows() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep the collapsed started -> reward jump");
			assertTrue(routes(definition, "started", "reward").stream()
					.noneMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(
						REWARD_NPC, QuestDialogAction.SET_SUCCEED.id(), 0))),
				() -> "quest " + contract.questId() + " must not invent SET_SUCCEED on the journal NPC");
			assertEquals(List.of(new AfterCommitAction.CloseDialog()),
				routes(definition, "s1", "s1").stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
						REWARD_NPC, QuestDialogAction.SETPRO2.id(), 0)))
					.findFirst().orElseThrow(() -> new AssertionError(
						"quest " + contract.questId() + " keeps the client SETPRO2 confirm button"))
					.afterCommit(),
				() -> "quest " + contract.questId() + " keeps the check_ok confirm as a no-op close");
		}
	}

	private static int talkNpc(QuestTransition route) {
		return route.event() instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : -1;
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
		try (InputStream input = Batch35ValentineTowerRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
