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
 * 锁定批次 14：事件族“两行任务书、末行是与领奖 NPC 的对话”的领奖行合同。
 * 两组证据：① 80255/80256 是 80255..80260 活动烟花族里唯一没收口的两个——同族 80257..80260 已在
 * 批次 1-7（commit 7a7d27809）改成 `reward var0=1` + 同形自愈边，本批两个漏网的原因是其行 1 写作
 * 字面中文名（“和帕尔图对话”/“和布巴纳对话”）而没有 `STR_DIC_N_` 键，按“末行 NPC 键”筛选时抓不到；
 * 归属由客户端 NPC 表确认（831163=event_Parutoo、831164=event_Boobanah）且与 npc-complete owner 一致。
 * ② 80601/80606 是德雷得奇安事件链头本，legacy `_80601Fight_Of_The_Navigators` /
 * `_80606The_Good_News_And_Bad` 的击杀分支显式 `setQuestVarById(0, 1)` 之后才 `setStatus(REWARD)`，
 * 即 legacy 领奖行就是 1（客户端表 831831=event_Isda、831832=event_Charmeine，对应行 1“向…报告”）；
 * 但 typed `reward` 投影仍是 0，经无 actions 的 `NPC_REPORT` 进入领奖态的存档因此匹配不到任何
 * reward 路由（`matchesSourceNode` 要求投影变量全等），必须同时补投影与自愈边。
 * <p>
 * Locks batch 14: the event-quest subgroup of the two-row reward-row family. 80255/80256 are the two
 * stragglers of the 80255..80260 event series whose siblings 80257..80260 were aligned by batch 1-7
 * (their journal row uses a literal NPC name instead of a STR_DIC_N_ key, so the earlier selector missed
 * them); 80601/80606 are the Dredgion chain heads whose legacy kill branch wrote `setQuestVarById(0, 1)`
 * before `setStatus(REWARD)`. Both groups now project the reward row 1 and heal stale REWARD/var0=0 saves.
 */
class RewardRowEventTwoRowContractTest {

	private record Contract(int questId, int rowNpcId) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/* 批次 14 收口的 4 个任务（行 1 NPC = 领奖/完成 owner）。 */
	/* The four quests closed by batch 14; the row-1 NPC is the reward completion owner. */
	private static final List<Contract> CONTRACTS = List.of(
		new Contract(80255, 831163),
		new Contract(80256, 831164),
		new Contract(80601, 831831),
		new Contract(80606, 831832)
	);

	/* 活动烟花族已对齐的同族参照（批次 1-7 收口，reward var0=1）。 */
	/* Aligned siblings of the event series (closed by batch 1-7, already projecting reward var0=1). */
	private static final List<Integer> ALIGNED_EVENT_SIBLINGS = List.of(80257, 80258, 80259, 80260);

	/* 德雷得奇安事件链头本：击杀事务必须保留 legacy 的 var0=1（SECTION_0 = 报告行）。 */
	/* Dredgion chain heads: the kill transaction must keep the legacy var0=1 (SECTION_0 = report row). */
	private static final List<Integer> DREDGION_CHAIN_HEADS = List.of(80601, 80606);

	@Test
	void rewardRowIsTheSecondClientRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestNode reward = node(definition, "reward");
			assertEquals(QuestStatus.REWARD, reward.projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(Map.of("var0", REWARD_ROW), reward.projection().variables(),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(Map.of("var0", STALE_ROW), node(definition, "started").projection().variables(),
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
	void alignedSiblingsOfTheEventSeriesShareTheRewardRow() throws Exception {
		for (int questId : ALIGNED_EVENT_SIBLINGS) {
			assertEquals(Map.of("var0", REWARD_ROW),
				node(definition(questId).definition(), "reward").projection().variables(),
				() -> "aligned sibling " + questId + " reward journal row");
		}
	}

	@Test
	void dredgionChainHeadsKeepTheLegacyKillStep() throws Exception {
		for (int questId : DREDGION_CHAIN_HEADS) {
			List<QuestTransition> killRoutes = definition(questId).definition().transitions().stream()
				.filter(route -> "started".equals(route.sourceNode()))
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertEquals(1, killRoutes.size(), () -> "quest " + questId + " kill route into REWARD");
			List<QuestAction.SetVariable> stepWrites = killRoutes.getFirst().actions().stream()
				.filter(QuestAction.SetVariable.class::isInstance)
				.map(QuestAction.SetVariable.class::cast)
				.filter(action -> "var0".equals(action.field()))
				.toList();
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), stepWrites,
				() -> "quest " + questId + " must keep the legacy setQuestVarById(0, 1) step");
			assertTrue(killRoutes.getFirst().conditions().contains(
					new QuestCondition.QuestVariableIs("var0", STALE_ROW)),
				() -> "quest " + questId + " kill route must be gated on the pre-reward row");
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
		try (InputStream input = RewardRowEventTwoRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
