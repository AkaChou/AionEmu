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
 * 锁定批次 17：圣灵守护者武器事件族的领奖行（80290/80291/80294/80295）。
 * <p>
 * 全库扫描迁移前 handler 的进入 REWARD 调用（useQuestItem / defaultCloseDialog / checkQuestItems /
 * checkQuestItemsSimple / changeQuestStep，651 个任务）后，“nextStep 被迁移丢弃且等于客户端任务书末行”
 * 只剩 15300/25300（QE-045 基线，专门门禁锁定）与 80291/80295（本批收口）；收口后扫描结果为空。
 * <p>
 * 武器变体：客户端 2 行（行 0 交付 5 个 relic_weapon_30、行 1 从 EVENT_Zephyrin/EVENT_Lilyolin 领取武器），
 * legacy `checkQuestItems(env, 0, 1, true, 5, 0)` 把 packed step 从 0 推到 1 并置 REWARD，迁移把
 * nextStep=1 丢了（投影停在 0）→ 投影改 1 + REWARD/var0=0 自愈边。
 * 护甲变体：客户端只有 1 行（交付 10 个 relic_armor_30），投影却是 1（领奖态落在不存在的行号上，
 * 审计 STATE_OUT_OF_RANGE）→ 投影改 0 + REWARD/var0=1 自愈边。
 * <p>
 * Locks batch 17: the Durable Daevanion Weapon event family. The weapon variants keep the legacy
 * hand-in step (checkQuestItems 0 -> 1) as the reward journal row; the armour variants only have one
 * client journal row and must stay on it.
 */
class DurableDaevanionWeaponRewardRowContractTest {

	/** 领奖行 NPC（唯一 completion owner）、领奖行号、旧存档行号。 */
	private record Contract(int questId, int ownerNpc, int rewardRow, int staleRow) {
	}

	/* 武器变体：legacy checkQuestItems(env, 0, 1, true, 5, 0)。 / Weapon variants with the legacy 0 -> 1 step. */
	private static final List<Contract> WEAPON_VARIANTS = List.of(
		new Contract(80291, 831384, 1, 0),
		new Contract(80295, 831387, 1, 0)
	);

	/* 护甲变体：客户端只有 1 行，投影必须落在行 0。 / Armour variants with a single journal row. */
	private static final List<Contract> ARMOUR_VARIANTS = List.of(
		new Contract(80290, 831384, 0, 1),
		new Contract(80294, 831387, 0, 1)
	);

	/* 同族 NPC：831384 = event_Zephyrin（天族），831387 = event_Lilyolin（魔族）。 */
	private static final Map<Integer, Integer> FAMILY_OWNER = Map.of(80290, 831384, 80291, 831384,
		80294, 831387, 80295, 831387);

	/* 同族配对：80290/80291 共用天族事件 NPC，80294/80295 共用魔族事件 NPC。 */
	private static final Map<Integer, Integer> FAMILY_SIBLINGS = Map.of(80290, 80291, 80291, 80290,
		80294, 80295, 80295, 80294);

	/* QE-045 基线（Quest15300And25300RewardProjectionTest 锁定）：保留进入 REWARD 前的 packed step。 */
	private static final Map<Integer, Integer> LEGACY_STEP_BASELINE = Map.of(15300, 13, 25300, 13);

	@Test
	void weaponVariantsProjectTheSecondJournalRow() throws Exception {
		for (Contract contract : WEAPON_VARIANTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Map.of("var0", 1), node(definition, "reward").projection().variables(),
				() -> "quest " + contract.questId() + " weapon variant must land on journal row 1");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(Map.of("var0", 0), node(definition, "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state stays on row 0");
		}
	}

	@Test
	void armourVariantsStayOnTheSingleJournalRow() throws Exception {
		for (Contract contract : ARMOUR_VARIANTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Map.of("var0", 0), node(definition, "reward").projection().variables(),
				() -> "quest " + contract.questId() + " armour variant has a single journal row");
			for (QuestNode candidate : definition.nodes()) {
				if ("COMPLETE".equals(candidate.projection().status().name())) {
					continue;
				}
				assertTrue(candidate.projection().variables().getOrDefault("var0", 0) <= 0,
					() -> "quest " + contract.questId() + " must not expose row 1 client-side");
			}
		}
	}

	@Test
	void rewardCompletionOwnerIsTheSingleFamilyNpc() throws Exception {
		for (Map.Entry<Integer, Integer> entry : FAMILY_OWNER.entrySet()) {
			int questId = entry.getKey();
			assertEquals(Set.of(entry.getValue()), rewardOwners(definition(questId).definition()),
				() -> "quest " + questId + " reward completion owner must be the family NPC only");
			String text = resourceText(questId);
			assertEquals(1, countNpcComplete(text),
				() -> "quest " + questId + " must register exactly one npc-complete block");
			assertTrue(text.contains("<npc-complete npc-id=\"" + entry.getValue() + "\""),
				() -> "quest " + questId + " npc-complete owner");
		}
	}

	@Test
	void familySiblingsShareTheirOwnerAndRowShape() throws Exception {
		/* 护甲/武器变体共用同一个事件 NPC（831384 或 831387），只是任务书行数不同。 */
		/* Siblings share the event NPC and differ only in journal row count. */
		for (Map.Entry<Integer, Integer> entry : FAMILY_OWNER.entrySet()) {
			int questId = entry.getKey();
			int sibling = FAMILY_SIBLINGS.get(questId);
			assertEquals(entry.getValue(), FAMILY_OWNER.get(sibling),
				() -> "quest " + questId + " and " + sibling + " must share the event NPC");
		}
		assertEquals(1, node(definition(80291).definition(), "reward").projection().variables().get("var0"),
			"weapon variant journal row");
		assertEquals(0, node(definition(80290).definition(), "reward").projection().variables().get("var0"),
			"armour variant journal row");
	}

	@Test
	void staleRewardRowsAreHealedOnEnterWorld() throws Exception {
		for (Contract contract : allContracts()) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", contract.staleRow()))))
				.toList();
			assertEquals(1, matches.size(), () -> "quest " + contract.questId() + " heal route for stale row "
					+ contract.staleRow());
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())), heal.actions(),
				() -> "quest " + contract.questId() + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + contract.questId() + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", contract.staleRow())), heal).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " healed status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " healed journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : allContracts()) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " reward route writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void legacyNextStepBaselineQuestsKeepTheirPackedStep() throws Exception {
		/* 15300/25300 的 legacy 是 changeQuestStep(13, 14, true)，但客户端验收值是进入前的 packed step 13
		   （Quest15300And25300RewardProjectionTest 锁定）；这两个任务是本族扫描里唯一必须保留旧 step 的成员，
		   没有客户端观测不得按“末行 14”改。 */
		/* 15300/25300 keep the pre-REWARD packed step 13 (test-locked); do not push them to row 14. */
		for (Map.Entry<Integer, Integer> entry : LEGACY_STEP_BASELINE.entrySet()) {
			assertEquals(Map.of("var0", entry.getValue()),
				node(definition(entry.getKey()).definition(), "reward").projection().variables(),
				() -> "quest " + entry.getKey() + " keeps the legacy reward packed step");
		}
	}

	private static List<Contract> allContracts() {
		return java.util.stream.Stream.concat(WEAPON_VARIANTS.stream(), ARMOUR_VARIANTS.stream()).toList();
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
		try (InputStream input = DurableDaevanionWeaponRewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = DurableDaevanionWeaponRewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
