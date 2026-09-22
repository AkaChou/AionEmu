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

/**
 * 锁定批次 13：5.8“两行任务书、末行是与领奖 NPC 的对话”族的领奖行合同。
 * 这 6 个任务的客户端 quest_summary 恰好 2 行：行 0 是“前往/交付/进入/接取对话”，行 1 是
 * “（再次）与 X 对话 / 向 X 报告”，且行 1 点名的 NPC 就是定义里的领奖/完成 NPC
 * （1926 Latri=203894、2938 Izwin=204267、39003 DF2a_Nevma_G_LHM=800504、49003 dromik=800505、
 * 80989/80990 IDRUN_Entrance_guide=836196）。修理前 reward 投影停在 0，REWARD 态在任务书里仍
 * 渲染已完成的行 0，行 1 永远拿不到 START/REWARD 状态；本批把 reward 投影改成 1，并补一条无
 * source 的 enter-world 自愈边（REWARD/var0=0 → 1），否则旧存档会因
 * {@code QuestMutationPlanner#matchesSourceNode} 的“投影变量必须全等”语义匹配不到任何领奖路由。
 * 同形的 13965/23965/15674/25674 属 QE-045 锁（f6aff952a“保留 legacy packed step” +
 * LegacyRewardStepProjectionRegressionTest），本批只登记证据、不改基线，故这里显式断言其保持 0。
 * <p>
 * Locks batch 13: the 5.8 "two-row journal whose second row talks to the reward NPC" family. All six
 * quests have exactly two client quest_summary rows and the second row names the definition's reward
 * completion NPC, yet the reward projection still stopped at row 0. The reward projection now equals
 * row 1 and a source-less enter-world heal edge (REWARD/var0=0 -&gt; 1) repairs persisted reward saves,
 * because the planner only matches source nodes whose declared variables are equal. The four QE-045
 * locked siblings (13965/23965/15674/25674) intentionally keep their legacy packed step 0.
 */
class RewardRowTwoRowTalkFamilyContractTest {

	private record Contract(int questId, int rowNpcId, boolean keepsLegacyEntryRoute) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/* 批次 13 收口的 6 个任务；keepsLegacyEntryRoute 表示是否保留迁移期的 REWARD/var0=1 入口边。 */
	/* The six quests closed by batch 13; keepsLegacyEntryRoute marks the migrated REWARD/var0=1 entry route. */
	private static final List<Contract> CONTRACTS = List.of(
		new Contract(1926, 203894, true),
		new Contract(2938, 204267, true),
		new Contract(39003, 800504, false),
		new Contract(49003, 800505, false),
		new Contract(80989, 836196, false),
		new Contract(80990, 836196, false)
	);

	/* QE-045 锁的同形姊妹任务：reward 投影必须停在 legacy packed step 0，本批不得改动。 */
	/* QE-045 locked same-shape siblings: the reward projection must stay at the legacy packed step 0. */
	private static final List<Integer> QE045_LOCKED_SIBLINGS = List.of(13965, 23965, 15674, 25674);

	@Test
	void rewardRowIsTheSecondClientRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestNode reward = node(definition(contract.questId()).definition(), "reward");
			assertEquals(QuestStatus.REWARD, reward.projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(Map.of("var0", REWARD_ROW), reward.projection().variables(),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(Map.of("var0", STALE_ROW),
				node(definition(contract.questId()).definition(), "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state keeps journal row 0");
		}
	}

	@Test
	void rewardOwnerIsTheSecondRowNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(Set.of(contract.rowNpcId()),
				rewardOwners(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward completion owner");
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
	void migratedRewardEntryRoutesAreKeptOrAbsent() throws Exception {
		for (Contract contract : CONTRACTS) {
			List<QuestTransition> matches = enterWorldRecoveries(
				definition(contract.questId()).definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", REWARD_ROW))))
				.toList();
			assertEquals(contract.keepsLegacyEntryRoute() ? 1 : 0, matches.size(),
				() -> "quest " + contract.questId() + " migrated REWARD entry route count");
			matches.forEach(route -> {
				assertEquals(List.of(), route.actions(),
					() -> "quest " + contract.questId() + " entry route actions");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit(),
					() -> "quest " + contract.questId() + " entry route after-commit");
			});
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
	void qe045LockedSiblingsKeepTheirLegacyBaseline() throws Exception {
		for (int questId : QE045_LOCKED_SIBLINGS) {
			assertEquals(Map.of("var0", STALE_ROW),
				node(definition(questId).definition(), "reward").projection().variables(),
				() -> "QE-045 locked quest " + questId + " must keep the legacy packed step");
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

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = RewardRowTwoRowTalkFamilyContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
