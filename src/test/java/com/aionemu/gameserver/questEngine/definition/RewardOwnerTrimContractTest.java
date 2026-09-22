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
 * 锁定批次 16：领奖 owner 收敛（QE-052）。客户端 quest_summary 领奖行点名的 NPC 就是领奖/completion NPC；
 * 行 0 的交互对象、接取 NPC 与任务目标物件都不得兼任领奖（否则 REWARD 态会出现多个领取入口，客户端在
 * 任务书仍渲染行 0 时就能领奖，且 completion owner 与领奖行 NPC 不再一一对应）。
 * <p>
 * 4712（Escape From The Dredgion）：行 1 = 向 Henir 报告，同族 4713/4714/4715/4716 的 completion owner 全部
 * 是 279042（行内写 STR_DIC_N_Henir）→ Henir = 279042；legacy `_4712Escape_From_The_Dredgion` 在囚犯
 * 798327/798330 处 `defaultCloseDialog(env, 0, 1, true, false)`（写 var0=1 并置 REWARD，囚犯随即 onDelete），
 * REWARD 态只在 279042 处发送 10002/结束对话 → 本批删除囚犯上的 npc-complete，保留其报告入口路由。
 * <p>
 * 2484（Our Man In Elysea）：行 1 = 和 Hippolyta 对话；legacy `_2484OurManInElysea` 只在 203331 处
 * `setStatus(REWARD)`（700267 烽火对象只写 `setQuestVarById(0, 1)`，204407 只负责接取）→ 本批删除
 * 204407/700267 上的 npc-complete，保留三条 NPC_REPORT → reward 的入口路由。
 * <p>
 * Locks batch 16: the reward completion owner is trimmed to the NPC named by the client journal's reward row,
 * while the row-0 interaction routes stay registered. 4712's Henir is resolved to 279042 through the four
 * sibling quests of the Dredgion chain; 2484 completes only on Hippolyta(203331) in the legacy handler.
 */
class RewardOwnerTrimContractTest {

	/**
	 * 每个收口任务：领奖行 NPC（唯一 completion owner）、必须保留的入口路由 owner、必须删除 completion 的 owner。
	 * Per quest: the reward-row NPC (sole completion owner), the entry-route owners kept, and the owners whose
	 * completion block must be gone.
	 */
	private record Contract(int questId, int rewardRowNpc, Set<Integer> entryOwners, Set<Integer> trimmedOwners) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/* 批次 16 收口的两个任务。 / The two quests closed by batch 16. */
	private static final List<Contract> CONTRACTS = List.of(
		new Contract(4712, 279042, Set.of(279042, 798327, 798330), Set.of(798327, 798330)),
		new Contract(2484, 203331, Set.of(204407, 700267, 203331), Set.of(204407, 700267))
	);

	/* Henir 解键旁证：同族这三个任务的行 1 都是“向 STR_DIC_N_Henir 报告”，completion owner 都是 279042。 */
	/* Cross-check for Henir: these three siblings name him in row 1 and complete on 279042. */
	private static final List<Integer> HENIR_ROW_SIBLINGS = List.of(4713, 4714, 4716);

	@Test
	void rewardCompletionOwnerIsUniqueAndIsTheRewardRowNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(Set.of(contract.rewardRowNpc()),
				rewardOwners(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward completion owner must be the reward-row NPC only");
		}
	}

	@Test
	void rowZeroEntryRoutesKeepTheirRegisteredOwners() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(contract.entryOwners(), reportRouteOwners(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " row-0 entry routes (NPC_REPORT -> reward)");
		}
	}

	@Test
	void trimmedOwnersNoLongerRegisterCompletion() throws Exception {
		for (Contract contract : CONTRACTS) {
			String text = resourceText(contract.questId());
			assertEquals(1, countNpcComplete(text),
				() -> "quest " + contract.questId() + " must register exactly one npc-complete block");
			assertTrue(text.contains("<npc-complete npc-id=\"" + contract.rewardRowNpc() + "\""),
				() -> "quest " + contract.questId() + " npc-complete owner");
			for (int trimmed : contract.trimmedOwners()) {
				assertFalse(text.contains("<npc-complete npc-id=\"" + trimmed + "\""),
					() -> "quest " + contract.questId() + " must not complete on " + trimmed
						+ " (row-0 interaction owner / start NPC / object)");
			}
		}
	}

	@Test
	void rewardProjectionPointsAtTheJournalRewardRow() throws Exception {
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
	void henirOwnerKeyResolvesThroughTheDredgionFamily() throws Exception {
		/* 279042 在客户端 NPC 表里没有名字（与 1123 的 STR_DIC_LA12 同型），只能靠同族行内命名 + owner 交叉解键。 */
		/* 279042 has no client NPC name; the Henir key is resolved through the sibling quests' row naming
		   and their completion owner. */
		for (int questId : HENIR_ROW_SIBLINGS) {
			assertEquals(Set.of(279042), rewardOwners(definition(questId).definition()),
				() -> "sibling " + questId + " must complete on 279042 (STR_DIC_N_Henir)");
			assertTrue(resourceText(questId).contains("<npc-complete npc-id=\"279042\""),
				() -> "sibling " + questId + " must register 279042 as its completion owner");
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

	private static Set<Integer> reportRouteOwners(QuestDefinition definition) {
		Set<Integer> owners = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if ("started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
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

	private static int countNpcComplete(String text) {
		int count = 0;
		int index = text.indexOf("<npc-complete ");
		while (index >= 0) {
			count++;
			index = text.indexOf("<npc-complete ", index + 1);
		}
		return count;
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
		try (InputStream input = RewardOwnerTrimContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = RewardOwnerTrimContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
