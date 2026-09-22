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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 15：残余“两行、末行是与领奖 NPC 的对话”族的领奖行合同（1123 / 2484）。
 * 1123 的行 1 写 `STR_DIC_LA12`（客户端 actor 名命名空间，不在 `STR_DIC_N_*` 键里，所以审计的
 * `last_row_npc_matches_quest` 是 False）；本批用同族交叉证据解键：1006/1122/1124/30507 四个任务的
 * `npc-complete` owner 全部是 790001，而客户端 NPC 表 790001 = Pernos，即 STR_DIC_LA12 = Pernos，
 * 与 1123 自己的 start/completion owner 自洽。2484 的行 1 是 Hippolyta（203331），legacy
 * `_2484OurManInElysea` 在烽火对象 700267 处 `setQuestVarById(0, 1)`、随后在 203331 处 `setStatus(REWARD)`，
 * 因此领奖行必须投影 1；它的三条 `NPC_REPORT`（204407/700267/203331）都不得改写行号。
 * <p>
 * Locks batch 15: the last two safely repairable quests of the two-row reward-row family. 1123's row 1
 * names STR_DIC_LA12, resolved to Pernos(790001) through the four sibling quests 1006/1122/1124/30507
 * whose completion owner is 790001; 2484's legacy handler wrote `setQuestVarById(0, 1)` on the beacon
 * object 700267 before entering REWARD at Hippolyta(203331).
 */
class RewardRowResidualTwoRowContractTest {

	private record Contract(int questId, int rowNpcId) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/* 批次 15 收口的两个任务：行 1 NPC 与 legacy/定义登记的报告 owner 集合。 */
	/* The two quests closed by batch 15 with their row-1 NPC and the registered report owners. */
	private static final List<Contract> CONTRACTS = List.of(
		new Contract(1123, 790001),
		new Contract(2484, 203331)
	);

	/* STR_DIC_LA12 解键旁证：这四个任务末行都写 STR_DIC_LA12，completion owner 都是 790001 = Pernos。 */
	/* Namespace cross-check for STR_DIC_LA12: these four quests name it in their last journal row and
	   all of them complete on 790001 = Pernos. */
	private static final List<Integer> PERNOS_ROW_SIBLINGS = List.of(1006, 1122, 1124, 30507);

	@Test
	void rewardRowIsTheSecondClientRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Map.of("var0", REWARD_ROW), node(definition, "reward").projection().variables(),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(Map.of("var0", STALE_ROW), node(definition, "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state keeps journal row 0");
		}
	}

	@Test
	void rewardOwnerIsTheSecondRowNpc() throws Exception {
		/* 本批只收口行投影：行 1 NPC 必须是领奖 owner 之一，此时 2484 另有烽火对象 700267 与接取 NPC
		   204407 的备用完成路径；批次 16 已按 legacy 归属把 owner 收敛到行 1 NPC（见
		   RewardOwnerTrimContractTest 的唯一性断言）。 */
		/* This batch only aligns the journal row: the row-1 NPC must be one of the reward completion owners;
		   at that time 2484 still kept the beacon object 700267 and the start NPC 204407 as alternate paths.
		   Batch 16 trimmed those owners to the reward-row NPC (uniqueness is locked by
		   RewardOwnerTrimContractTest). */
		for (Contract contract : CONTRACTS) {
			assertTrue(rewardOwners(definition(contract.questId()).definition())
					.contains(contract.rowNpcId()),
				() -> "quest " + contract.questId() + " reward completion owner must include the row-1 NPC");
		}
	}

	@Test
	void rewardEntryRoutesKeepTheirRegisteredOwners() throws Exception {
		/* 1123：REWARD 由 LF1 感应区影片推进进入（客户端行 0 的目标就是“到 FLA07 寻找踪迹”），
		   全任务唯一 NPC 是 790001。批次 46 起入口拆成两步：enter-zone 只播片自环，行 1 在客户端
		   影片结束回调（movie-end 11）落盘——过场遮罩期间下发状态刷新会被客户端丢弃（用户真机反馈
		   “看完剧情后没有推进到下一步”，详见 Batch46MovieEndRowAdvanceContractTest）。
		   Batch 46 splits the entry: the LF1 zone only plays movie 11, and the reward row lands on the
		   client movie-end callback. */
		List<QuestTransition> tuttyRoutes = definition(1123).definition().transitions().stream()
			.filter(route -> "started".equals(route.sourceNode()))
			.filter(route -> "reward".equals(route.targetNode()))
			.toList();
		assertEquals(1, tuttyRoutes.size(), "1123 reward entry route");
		assertEquals(new QuestEvent.MovieEnd(11), tuttyRoutes.getFirst().event(),
			"1123 must land REWARD on the LF1 sensory-area movie-end callback");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			tuttyRoutes.getFirst().afterCommit(), "1123 reward entry after-commit");
		List<QuestTransition> tuttyZone = definition(1123).definition().transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.EnterZone)
			.toList();
		assertEquals(1, tuttyZone.size(), "1123 must keep the LF1_SENSORY_AREA_Q1123 zone entry");
		assertEquals(List.of(new AfterCommitAction.PlayMovie(11)), tuttyZone.getFirst().afterCommit(),
			"1123 zone entry only plays movie 11");

		/* 2484：legacy 登记了 204407（接取）、700267（烽火对象写 var0=1）、203331（Hippolyta 领奖）三条 talk 路线。 */
		/* 2484 keeps the three legacy talk routes: 204407 (start), 700267 (beacon writes var0=1), 203331 (Hippolyta). */
		Set<Integer> reportNpcs = new LinkedHashSet<>();
		for (QuestTransition transition : definition(2484).definition().transitions()) {
			if ("started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk) {
				reportNpcs.add(talk.npcId());
			}
		}
		assertEquals(Set.of(204407, 700267, 203331), reportNpcs, "2484 report route owners");
	}

	@Test
	void staleRewardRowsAreHealedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", STALE_ROW))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + contract.questId() + " heal route for stale row " + STALE_ROW);
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
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(REWARD_ROW, value, () -> "quest " + contract.questId()
							+ " reward route writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void pernosRowKeyResolvesThroughTheMigratedFamily() throws Exception {
		/* STR_DIC_LA12 不在客户端 NPC 名表里，只能靠“末行命名 + completion owner”交叉解键。 */
		/* STR_DIC_LA12 is absent from the client NPC name table; the key is resolved by matching the
		   journal row naming against the completion owner across the family. */
		for (int questId : PERNOS_ROW_SIBLINGS) {
			/* 1122 的完成走 choice/select 型事务而不是 TalkToNpc，故这里直接读原文件的 npc-complete owner。 */
			/* 1122 completes through choice/select transitions rather than TalkToNpc, so read the raw
			   npc-complete owner from the resource text. */
			assertTrue(resourceText(questId).contains("<npc-complete npc-id=\"790001\""),
				() -> "sibling " + questId + " must complete on 790001 (STR_DIC_LA12 = Pernos)");
		}
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static Set<Integer> rewardOwners(QuestDefinition definition) {
		Set<Integer> owners = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if ("reward".equals(transition.sourceNode()) && "complete".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk) {
				owners.add(talk.npcId());
			}
		}
		return owners;
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

	private static String resourceText(int questId) throws IOException {
		try (InputStream input = RewardRowResidualTwoRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = RewardRowResidualTwoRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
