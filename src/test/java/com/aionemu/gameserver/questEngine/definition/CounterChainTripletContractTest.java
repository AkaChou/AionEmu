package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 25：COUNTER_CHAIN 三槽族（18033/28033/28313）与 2842 的饱和领奖投影。
 * <p>
 * 族级判据：这三个任务的客户端 {@code Quest_unpacked/quest_monster.csv} 各有三条**链式** 0/1 计数记录
 * （行 0 = {@code SECTION_0<1; SECTION_5==0}、行 1 = {@code SECTION_1<1}、行 2 = {@code SECTION_2<1}），
 * 任务书 {@code quest_summary} 也是 3 行（行 0..2 = 依次消灭三组目标，行 3 不存在：报告与领奖同在末行）。
 * 旧定义把三条记录压成一个 {@code counter-grid}（单维 var0 required=1，任意一只怪即可满足）或一条
 * “步骤号”单槽（28313 写 var0 = 已完成组数 0..3），SECTION_1/SECTION_2 永远为 0 —— 客户端任务书第 2、3 个
 * 计数永远显示 0/1、后续行永远沉不下去（QE-053 症状）。本门禁按 13918/23918 的已验收模板锁死：
 * var0..var2 各占 {@code 6n}、每只（组）怪只推自己那一槽、乱序/回看不计数、领奖投影三槽全 1。
 * <p>
 * 2842（天族镜像 1841）：客户端门控是 {@code SECTION_0<39; SECTION_5==0} 的单行狩猎计数，var0 是 0..39 的
 * 击杀数；reward 投影必须携带饱和值 39，否则 {@code QuestMutationPlanner#matchesSourceNode} 的逐字段全等会把
 * 领奖态存档挡在所有 reward 路由之外（玩家在领奖阶段卡死）。天族 1841 早已是 39，本门禁同时锁死镜像一致。
 * <p>
 * Locks batch 25: the three-slot chained ladders of 18033/28033/28313 (each client row owns one 6-bit counter
 * slot, each target advances exactly its own slot, out-of-order kills do not count, the saturated reward
 * projection is all ones) plus 2842's 39-kill reward projection, which must match its Elyos mirror 1841.
 */
class CounterChainTripletContractTest {

	/** 任务 / 接取 NPC / 末行报告与领奖 NPC / 三组击杀目标 / 旧模型领奖投影的 var0。 */
	private record Contract(int questId, int offerNpc, int reportNpc, List<List<Integer>> killGroups,
			int legacyRewardVar0, boolean legacyStepModel) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(18033, 801037, 801281,
			List.of(List.of(230744), List.of(230745), List.of(230749)), 1, false),
		new Contract(28033, 801047, 801280,
			List.of(List.of(230744), List.of(230745), List.of(230749)), 1, false),
		new Contract(28313, 804821, 804821,
			List.of(List.of(217371, 246131, 248077), List.of(217373, 246132, 248078),
				List.of(217376, 246133, 248079)), 3, true));

	private static final List<String> FIELDS = List.of("var0", "var1", "var2");
	private static final int GOLD = 903960;
	private static final int EXP = 9133366;
	private static final int ITEM = 186000469;
	private static final int ITEM_COUNT = 30;

	/** 前 ones 个槽为 1 的三槽状态（行 n 打完后的状态）。 / Slots set after the kill closing row n. */
	private static Map<String, Integer> cumulative(int ones) {
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (int index = 0; index < FIELDS.size(); index++) {
			variables.put(FIELDS.get(index), index < ones ? 1 : 0);
		}
		return variables;
	}

	@Test
	void everyClientRowOwnsOneChainedCounterSlot() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			for (int index = 0; index < FIELDS.size(); index++) {
				String fieldName = FIELDS.get(index);
				int section = index;
				BitField field = layout.field(fieldName);
				assertNotNull(field, () -> "quest " + contract.questId() + " must declare " + fieldName);
				assertEquals(6 * section, field.offset(),
					() -> "quest " + contract.questId() + " " + fieldName + " must map SECTION_" + section);
				assertEquals(6, field.width(),
					() -> "quest " + contract.questId() + " " + fieldName + " keeps 6 bits for legacy saves");
			}
			/* SECTION_3 必须保持未声明：客户端三条记录只声明到 SECTION_2。 */
			/* SECTION_3 must stay undeclared: the client only gates on SECTION_0..2. */
			assertEquals(Set.copyOf(FIELDS),
				layout.fields().stream().map(BitField::name).collect(Collectors.toSet()),
				() -> "quest " + contract.questId() + " must declare exactly three counter slots");

			assertEquals(cumulative(0), projection(definition, "started"),
				() -> "quest " + contract.questId() + " started must project the empty ladder");
			for (int ones = 1; ones <= FIELDS.size(); ones++) {
				int expected = ones;
				assertEquals(cumulative(ones), projection(definition, "k" + ones),
					() -> "quest " + contract.questId() + " node k" + expected + " projection");
			}
			assertEquals(cumulative(3), projection(definition, "reward"),
				() -> "quest " + contract.questId() + " reward must project the saturated ladder");
			assertEquals(cumulative(0), projection(definition, "complete"),
				() -> "quest " + contract.questId() + " complete must reset the ladder");
		}
	}

	@Test
	void eachTargetAdvancesExactlyItsOwnSlot() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			for (int index = 0; index < contract.killGroups().size(); index++) {
				int slot = index;
				for (int npcId : contract.killGroups().get(index)) {
					List<QuestMutationPlan> plans = plans(compiled, QuestStatus.START, cumulative(index),
						new QuestEvent.KillNpc(npcId));
					assertEquals(1, plans.size(), () -> "quest " + contract.questId() + " target " + npcId
						+ " must answer on ladder step " + slot);
					QuestMutationPlan plan = plans.getFirst();
					assertEquals(QuestStatus.START, plan.nextStatus(),
						() -> "quest " + contract.questId() + " target " + npcId + " stays in START");
					assertEquals(cumulative(index + 1), unpack(compiled, plan),
						() -> "quest " + contract.questId() + " target " + npcId + " must set exactly var" + slot);
					assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						plan.afterCommit(),
						() -> "quest " + contract.questId() + " target " + npcId + " must sync PACKET_ONLY");
				}
			}
		}
	}

	@Test
	void outOfOrderOrBackwardKillsHaveNoPlan() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			for (int index = 0; index < contract.killGroups().size(); index++) {
				int expectedStep = index;
				for (int npcId : contract.killGroups().get(index)) {
					for (int step = 0; step <= FIELDS.size(); step++) {
						if (step == expectedStep) {
							continue;
						}
						int probeStep = step;
						assertTrue(plans(compiled, QuestStatus.START, cumulative(step),
								new QuestEvent.KillNpc(npcId)).isEmpty(),
							() -> "quest " + contract.questId() + " target " + npcId
								+ " must not count on ladder step " + probeStep
								+ " (client journal row " + expectedStep + ")");
					}
					/* 领奖态下不再计数：所有击杀只在 START 阶梯上响应。 */
					/* No counting in the reward stage: kills only answer on the START ladder. */
					assertTrue(plans(compiled, QuestStatus.REWARD, cumulative(3),
							new QuestEvent.KillNpc(npcId)).isEmpty(),
						() -> "quest " + contract.questId() + " target " + npcId + " must not count in REWARD");
				}
			}
		}
	}

	@Test
	void reportAndCompletionStayOnTheJournalRowNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Set.of(contract.offerNpc()), talkNpcIds(definition, "unaccepted", "started"),
				() -> "quest " + contract.questId() + " offer must stay on the offer NPC " + contract.offerNpc());
			assertEquals(Set.of(contract.reportNpc()), talkNpcIds(definition, "k3", "reward"),
				() -> "quest " + contract.questId() + " report must move to the row-1 NPC "
					+ contract.reportNpc());
			assertEquals(Set.of(contract.reportNpc()), talkNpcIds(definition, "reward", "complete"),
				() -> "quest " + contract.questId() + " completion must stay on the row-1 NPC "
					+ contract.reportNpc());
			if (contract.offerNpc() == contract.reportNpc()) {
				/* 28313 的客户端行 0（接取）与行 1（向 Nineveh 报告）点名同一个 NPC，保持原样。 */
				/* 28313's client row 0 (accept) and row 1 (report to Nineveh) name the same NPC. */
				continue;
			}
			/* QE-052：接取 NPC 不再兼任报告或领奖（18033/28033 旧定义把 801037/801047 当报告 NPC）。 */
			/* QE-052: the offer NPC no longer owns the report or the completion. */
			for (QuestTransition route : definition.transitions()) {
				if (!(route.event() instanceof QuestEvent.TalkToNpc talk) || talk.npcId() != contract.offerNpc()) {
					continue;
				}
				assertFalse("complete".equals(route.targetNode())
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id(),
					() -> "quest " + contract.questId() + " offer NPC must not report or complete");
			}
		}
	}

	@Test
	void armyCompletionsGrantTheFullFixedRewardSet() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (contract.legacyStepModel()) {
				/* 28313 走按职业展开的奖励分支，另见 ninevehKeepsEveryClassRewardBranch。 */
				/* 28313 owns class-expanded reward branches; see ninevehKeepsEveryClassRewardBranch. */
				continue;
			}
			CompiledQuestDefinition compiled = definition(contract.questId());
			assertEquals(3, compiled.definition().metadata().rewards().size(),
				() -> "quest " + contract.questId() + " declares three rewards");
			QuestMutationPlan completion = plans(compiled, QuestStatus.REWARD, cumulative(3),
				new QuestEvent.TalkToNpc(contract.reportNpc(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()))
				.stream().findFirst().orElseThrow(() -> new AssertionError("quest " + contract.questId()
					+ " must complete from the saturated reward state"));
			assertEquals(QuestStatus.COMPLETE, completion.nextStatus(),
				() -> "quest " + contract.questId() + " completion status");
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("GOLD", 0, GOLD, QuestRewardAmountMode.QUEST_BASE)),
				() -> "quest " + contract.questId() + " must grant the GOLD reward");
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("EXP", 0, EXP, QuestRewardAmountMode.QUEST_BASE)),
				() -> "quest " + contract.questId() + " must grant the EXP reward");
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("ITEM", ITEM, ITEM_COUNT, QuestRewardAmountMode.EXACT)),
				() -> "quest " + contract.questId() + " must grant ITEM " + ITEM + " x" + ITEM_COUNT);
			assertTrue(completion.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance),
				() -> "quest " + contract.questId() + " must complete the quest");
		}
	}

	@Test
	void legacySavesHealOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestMutationPlan> heals = plans(compiled, QuestStatus.REWARD,
				Map.of("var0", contract.legacyRewardVar0()), new QuestEvent.EnterWorld());
			assertEquals(1, heals.size(), () -> "quest " + contract.questId()
				+ " legacy reward projection must heal on enter-world");
			assertEquals(cumulative(3), unpack(compiled, heals.getFirst()),
				() -> "quest " + contract.questId() + " must heal to the saturated (1,1,1) projection");
			assertEquals(QuestStatus.REWARD, heals.getFirst().nextStatus(),
				() -> "quest " + contract.questId() + " must stay in REWARD while healing");
			assertTrue(heals.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " heal must refresh visibility");

			/* 正规领奖态不得被自愈边重写。 */
			/* The legitimate reward state must not be rewritten by the heal edge. */
			assertTrue(plans(compiled, QuestStatus.REWARD, cumulative(3), new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not resync a saturated reward state");

			/* 旧 step 模型（28313）的 START 阶梯存档：var0=2 -> k2、var0=3 -> k3；var0=1 与新 k1 同形。 */
			/* Legacy step-model START saves (28313): var0=2 -> k2, var0=3 -> k3; var0=1 already equals k1. */
			for (int done = 1; done <= 3; done++) {
				int killsDone = done;
				List<QuestMutationPlan> repairs = plans(compiled, QuestStatus.START, Map.of("var0", done),
					new QuestEvent.EnterWorld());
				if (!contract.legacyStepModel() || done == 1) {
					assertTrue(repairs.isEmpty(), () -> "quest " + contract.questId()
						+ " must not migrate START var0=" + killsDone + " (already a ladder state)");
					continue;
				}
				assertEquals(1, repairs.size(), () -> "quest " + contract.questId()
					+ " must migrate START var0=" + killsDone);
				assertEquals(cumulative(done), unpack(compiled, repairs.getFirst()),
					() -> "quest " + contract.questId() + " var0=" + killsDone + " maps onto k" + killsDone);
				assertEquals(QuestStatus.START, repairs.getFirst().nextStatus(),
					() -> "quest " + contract.questId() + " migration must stay in START");
			}
		}
	}

	@Test
	void ninevehKeepsEveryClassRewardBranch() throws Exception {
		QuestDefinition definition = definition(28313).definition();
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertEquals(27, completions.size(), "28313 must keep all 27 class-expanded reward branches");
		Set<PlayerClass> classes = completions.stream()
			.flatMap(route -> route.conditions().stream())
			.filter(QuestCondition.AdvancedClassIs.class::isInstance)
			.map(QuestCondition.AdvancedClassIs.class::cast)
			.map(QuestCondition.AdvancedClassIs::playerClass)
			.collect(Collectors.toSet());
		assertEquals(11, classes.size(), "28313 reward branches must cover 11 advanced classes");
		for (QuestTransition route : completions) {
			assertTrue(route.conditions().stream().anyMatch(QuestCondition.AdvancedClassIs.class::isInstance),
				() -> "28313 reward branch " + route.event() + " must stay class-gated");
			assertTrue(route.actions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward reward
					&& "ITEM".equals(reward.kind())),
				() -> "28313 reward branch " + route.event() + " must grant its class item");
			assertTrue(route.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance),
				() -> "28313 reward branch " + route.event() + " must complete the quest");
		}
		Set<Integer> dialogs = completions.stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).dialogId())
			.collect(Collectors.toSet());
		assertEquals(Set.of(QuestDialogAction.SELECTED_QUEST_REWARD1.id(),
				QuestDialogAction.SELECTED_QUEST_REWARD2.id(), QuestDialogAction.SELECTED_QUEST_REWARD3.id(),
				QuestDialogAction.SELECTED_QUEST_REWARD4.id(), QuestDialogAction.SELECTED_QUEST_REWARD5.id(),
				QuestDialogAction.SELECTED_QUEST_REWARD6.id()),
			dialogs, "28313 must keep the six client reward selections");
	}

	@Test
	void treasureChamberRewardProjectionIsSaturated() throws Exception {
		CompiledQuestDefinition compiled = definition(2842);
		QuestDefinition definition = compiled.definition();
		BitField field = definition.progressLayout().field("var0");
		assertNotNull(field, "2842 must declare var0");
		assertEquals(39, field.maxValue(), "2842 var0 must be able to hold the 39-kill counter");
		assertEquals(39, projection(definition, "reward").get("var0"),
			"2842 reward projection must carry the saturated 39-kill counter");
		/* 天族镜像 1841 早已是 39：两侧必须一致，否则领奖态只有一侧可用。 */
		/* The Elyos mirror 1841 already ships 39; both sides must agree or only one can be rewarded. */
		assertEquals(39, projection(definition(1841).definition(), "reward").get("var0"),
			"1841/2842 mirror pair must share the saturated reward projection");

		QuestMutationPlan completion = plans(compiled, QuestStatus.REWARD, Map.of("var0", 39),
			new QuestEvent.TalkToNpc(266568, QuestDialogAction.SELECTED_QUEST_REWARD1.id()))
			.stream().findFirst().orElseThrow(() -> new AssertionError(
				"2842 must complete from the saturated reward state"));
		assertEquals(QuestStatus.COMPLETE, completion.nextStatus(), "2842 completion status");
		assertTrue(completion.requiredActions().contains(
				new QuestAction.GrantReward("EXP", 0, 2068277, QuestRewardAmountMode.QUEST_BASE)),
			"2842 must grant the EXP reward");
		assertTrue(completion.requiredActions().contains(
				new QuestAction.GrantReward("AP", 0, 700, QuestRewardAmountMode.QUEST_BASE)),
			"2842 must grant the AP reward");
	}

	private static Map<String, Integer> projection(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label))
			.projection().variables();
	}

	private static Set<Integer> talkNpcIds(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
			.collect(Collectors.toSet());
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, QuestEvent event) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		QuestSnapshot snapshot = new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(packedVariables), Map.of());
		return compiled.definition().transitions().stream()
			.flatMap(route -> QuestMutationPlanner.plan(compiled, snapshot, event, route).stream())
			.toList();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = CounterChainTripletContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
