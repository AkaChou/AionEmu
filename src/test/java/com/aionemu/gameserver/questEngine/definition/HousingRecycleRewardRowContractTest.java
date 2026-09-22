package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 27：住宅回收箱任务 18805/28805 的领奖行合同（QE-051）。
 * <p>
 * 客户端 quest_summary 都是 3 行：行 0 = 和旧货商主人对话（STR_DIC_N_Shugo_housing_rec /
 * STR_DIC_N_Shugo_housing_drec，任务内 NPC 830520/830521），行 1 = 阅读回收箱
 * （730522/730525），行 2 = 和旧货商主人对话（领奖行）。legacy
 * `_18805Going_Thrifting` / `_28805SomethingOld_SomethingNew` 的证据链：回收箱的
 * `STEP_TO_2 -> defaultCloseDialog(env, 1, 2)` 把 step 推到 2，回到旧货商主人的
 * `SELECT_REWARD` 才 `changeQuestStep(env, 2, 2, true)` —— 旧引擎只置 REWARD、step 停在 2。
 * 迁移后的 reward 投影被写成 1，且 s1 -> reward 的交接又把 var0 写回 1，玩家进入领奖态时
 * 任务书停在“阅读回收箱说明”那一行；本门禁锁定投影 = 2、交接不得回写旧行、
 * REWARD/var0=1 的旧存档进入世界时自愈到 2、正规态 var0=2 不被重放。
 * <p>
 * Locks batch 27: the QE-051 reward-row contract of the housing recycle quests 18805/28805 —
 * reward projection 2 (the client journal's last row), the recycle-box handover must not write
 * the stale row 1 back, and saves persisted at REWARD/var0=1 are repaired to 2 on enter-world.
 */
class HousingRecycleRewardRowContractTest {

	/** 任务 / 领奖 NPC / 回收箱 NPC / 旧投影行 / 正确领奖行。 */
	private record Contract(int questId, int rewardNpc, int boxNpc, int staleRow, int rewardRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(18805, 830520, 730522, 1, 2),
		new Contract(28805, 830521, 730525, 1, 2));

	@Test
	void rewardRowEqualsTheClientJournalLastRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestNode reward = node(definition(contract.questId()).definition(), "reward");
			assertEquals(QuestStatus.REWARD, reward.projection().status(),
				() -> "quest " + contract.questId() + " reward status");
			assertEquals(contract.rewardRow(), reward.projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " reward journal row");
		}
	}

	@Test
	void staleRewardRowIsRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", contract.staleRow())), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", contract.staleRow())),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void saturatedRewardRowIsNotReplayed() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			assertTrue(plans(compiled, QuestStatus.REWARD,
					Map.of("var0", contract.rewardRow()), new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId()
					+ " must not replay the recovery edge for the live reward row");
		}
	}

	@Test
	void rewardHandoverNeverWritesTheStaleRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> handovers = definition.transitions().stream()
				.filter(route -> contract.boxNpc() == dialogNpc(route))
				.toList();
			assertFalse(handovers.isEmpty(), () -> "quest " + contract.questId() + " box handovers");
			for (QuestTransition route : handovers) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " box handover writes a non reward journal row");
					}
				}
			}
			assertTrue(definition.transitions().stream().anyMatch(route ->
					"reward".equals(route.targetNode()) && contract.boxNpc() == dialogNpc(route)),
				() -> "quest " + contract.questId() + " box handover into reward");
		}
	}

	@Test
	void rewardOwnerStaysOnTheJournalRecycleMerchant() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.toList();
			assertFalse(completions.isEmpty(), () -> "quest " + contract.questId() + " completion routes");
			for (QuestTransition route : completions) {
				assertEquals(contract.rewardNpc(), dialogNpc(route),
					() -> "quest " + contract.questId() + " completion owner");
			}
			assertTrue(definition.transitions().stream().anyMatch(route ->
					"reward".equals(route.sourceNode()) && "reward".equals(route.targetNode())
						&& contract.rewardNpc() == dialogNpc(route)),
				() -> "quest " + contract.questId() + " reward entry page owner");
		}
	}

	private static int dialogNpc(QuestTransition route) {
		return route.event() instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : -1;
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, QuestEvent event) {
		QuestSnapshot snapshot = snapshot(compiled, status, variables);
		return compiled.definition().transitions().stream()
			.flatMap(route -> QuestMutationPlanner.plan(compiled, snapshot, event, route).stream())
			.toList();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(packedVariables), Map.of());
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = HousingRecycleRewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
