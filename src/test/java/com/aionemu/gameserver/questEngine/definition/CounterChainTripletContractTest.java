package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 28313 的真端网格三槽合同与 2842/1841 的饱和领奖投影。
 * <p>
 * 28313 已在 P0c-9 退役为真端文件驱动（{@code <class>_selectable_reward} 职业奖励区块落地后采纳）。
 * 客户端合同不变：{@code Quest_unpacked/quest_monster.csv} 三条 0/1 计数记录（行 0..2 各占
 * {@code SECTION_n<1}），真端形状是三槽 6 位计数网格（var0/var1/var2 @ 0/6/12），每只（组）怪只推
 * 自己那一槽、**各维度独立推进（乱序击杀也计数——真端网格语义）**、领奖投影三槽全 1
 * （REWARD/4161）、完成段按 11 职业条件展开 27 件职业物品 × 确认段 dialogId 8..23。
 * 旧 XML 的"步骤号单槽 + 乱序不计数 + EnterWorld 存档自愈边"是历史错误/历史形状，已按
 * 真端权威退役；自愈边按 P0c-6 先例登记进 p0c6-legacy-save-normalization.tsv（可选 DB 归一化）。
 * 节点定位一律按 (状态, 打包投影)，不再依赖旧标签（started/k1..k3）。
 * <p>
 * 2842（天族镜像 1841）：客户端门控是 {@code SECTION_0<39; SECTION_5==0} 的单行狩猎计数，var0 是 0..39 的
 * 击杀数；reward 投影必须携带饱和值 39，否则 {@code QuestMutationPlanner#matchesSourceNode} 的逐字段全等会把
 * 领奖态存档挡在所有 reward 路由之外（玩家在领奖阶段卡死）。天族 1841 早已是 39，本门禁同时锁死镜像一致。
 * <p>
 * Locks 28313's retail three-slot grid contract (retail/client authoritative since P0c-9) plus 2842's
 * 39-kill reward projection, which must match its Elyos mirror 1841. Nodes are located by
 * (status, packed projection); the legacy step-model labels and EnterWorld heal edges are gone by design.
 */
class CounterChainTripletContractTest {

	/** 任务 / 接取 NPC / 末行报告与领奖 NPC / 三组击杀目标 / 旧模型领奖投影的 var0。 */
	private record Contract(int questId, int offerNpc, int reportNpc, List<List<Integer>> killGroups,
			int legacyRewardVar0, boolean legacyStepModel) {
	}

	/**
	 * 18033/28033 已在 P0c-4 退役为真端区域发放（{@code _area_}）：真端形状是"产性格子
	 * （{@code a0b0c0..a1b1c1}）+ SystemGrant 边"，不是本 XML 的链式 {@code k1..k3} 阶梯，
	 * 也不再有点名接取 NPC；其真端 IR 由 {@code retail-simple-hunt-adjudicated-ir-fingerprints.tsv} 冻结，
	 * 区域绑定与系统发放边由 {@code RetailSystemGrantDispatchTest} 守。本门禁只锁仍由 XML 承载的行。
	 * 18033/28033 retired to the retail area grant in P0c-4: the retail shape is a product grid plus a
	 * {@code SystemGrant} edge (no offer NPC), frozen separately; this gate keeps the XML-owned rows.
	 */
	private static final List<Contract> CONTRACTS = List.of(
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
	void everyClientRowOwnsOneCounterSlot() throws Exception {
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
					() -> "quest " + contract.questId() + " " + fieldName + " keeps 6 bits");
			}
			/* SECTION_3 必须保持未声明：客户端三条记录只声明到 SECTION_2。 */
			/* SECTION_3 must stay undeclared: the client only gates on SECTION_0..2. */
			assertEquals(Set.copyOf(FIELDS),
				layout.fields().stream().map(BitField::name).collect(Collectors.toSet()),
				() -> "quest " + contract.questId() + " must declare exactly three counter slots");

			/* 真端网格：三槽 {0,1} 全组合 8 个 START 节点 + NONE/0 + REWARD/4161 + COMPLETE/0。 */
			/* Retail grid: all eight {0,1}^3 START combos plus NONE/0, REWARD/4161, COMPLETE/0. */
			for (int var0 = 0; var0 <= 1; var0++) {
				for (int var1 = 0; var1 <= 1; var1++) {
					for (int var2 = 0; var2 <= 1; var2++) {
						Map<String, Integer> state = Map.of("var0", var0, "var1", var1, "var2", var2);
						assertEquals(state, projectionAt(definition, QuestStatus.START,
								packedOf(layout, state)),
							() -> "quest " + contract.questId() + " must own the grid node " + state);
					}
				}
			}
			assertEquals(cumulative(3), projectionAt(definition, QuestStatus.REWARD, 4161),
				() -> "quest " + contract.questId() + " reward must project the saturated ladder");
			assertEquals(cumulative(0), projectionAt(definition, QuestStatus.COMPLETE, 0),
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
	void outOfOrderKillsAdvanceOnlyTheirOwnSlot() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 真端网格语义：各维度独立推进——任意网格状态下，组 i 的怪把 var_i 0→1、
			 * 其余槽保持不变；旧 XML 的"乱序不计数"链式约束已按真端权威退役。 */
			/* Retail grid semantics: dimensions advance independently — a group-i kill flips var_i
			 * from any grid state; the legacy "out-of-order kills do not count" chain is retired. */
			for (int index = 0; index < contract.killGroups().size(); index++) {
				int slot = index;
				for (int npcId : contract.killGroups().get(index)) {
					for (int var0 = 0; var0 <= 1; var0++) {
						for (int var1 = 0; var1 <= 1; var1++) {
							for (int var2 = 0; var2 <= 1; var2++) {
								Map<String, Integer> state = Map.of("var0", var0, "var1", var1, "var2", var2);
								if (state.get("var" + slot) == 1) {
									continue;
								}
								String at = state.values().toString();
								List<QuestMutationPlan> advance = plans(compiled, QuestStatus.START, state,
									new QuestEvent.KillNpc(npcId));
								assertEquals(1, advance.size(), () -> "quest " + contract.questId()
									+ " target " + npcId + " must advance its own slot from grid state " + at);
								Map<String, Integer> expected = new LinkedHashMap<>(state);
								expected.put("var" + slot, 1);
								assertEquals(expected, unpack(compiled, advance.getFirst()),
									() -> "quest " + contract.questId() + " target " + npcId
										+ " must flip exactly var" + slot);
								assertEquals(List.of(new AfterCommitAction.SyncQuestState(
										QuestStateSyncMode.PACKET_ONLY)), advance.getFirst().afterCommit(),
									() -> "quest " + contract.questId() + " target " + npcId
										+ " must sync PACKET_ONLY");
							}
						}
					}
					/* 领奖态下不再计数：所有击杀只在 START 网格上响应。 */
					/* No counting in the reward stage: kills only answer on the START grid. */
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
			/* 真端形状按 (状态, 打包投影) 定位边：客户端任务书行 0（接取）/行 1（报告+领奖）
			 * 都点名 Nineveh(804821)，真端合成定义必须保持同一 NPC 归属。 */
			/* Retail shape, located by (status, packed projection): client journal rows 0 (accept)
			 * and 1 (report + reward) both name Nineveh(804821); the retail definition keeps that. */
			assertEquals(Set.of(contract.offerNpc()),
				talkNpcIdsByStatus(definition, QuestStatus.NONE, null, QuestStatus.START),
				() -> "quest " + contract.questId() + " offer must stay on the offer NPC " + contract.offerNpc());
			assertEquals(Set.of(contract.reportNpc()),
				talkNpcIdsByStatus(definition, QuestStatus.START, 4161, QuestStatus.REWARD),
				() -> "quest " + contract.questId() + " report must move to the row-1 NPC "
					+ contract.reportNpc());
			assertEquals(Set.of(contract.reportNpc()),
				talkNpcIdsByStatus(definition, QuestStatus.REWARD, 4161, QuestStatus.COMPLETE),
				() -> "quest " + contract.questId() + " completion must stay on the row-1 NPC "
					+ contract.reportNpc());
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
	void legacySaveHealEdgesAreRetiredWithTheXml() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 真端网格不表达旧 XML 的 EnterWorld 存档自愈边（步骤号→行号归一）：登录边按 P0c-6 先例
			 * 登记进 p0c6-legacy-save-normalization.tsv（可选一次性 DB 归一化），不再编入定义。
			 * The retail grid drops the legacy EnterWorld save-heal edges; they are registered
			 * per the P0c-6 precedent instead of being compiled. */
			for (int step = 0; step <= FIELDS.size(); step++) {
				int probeStep = step;
				assertTrue(plans(compiled, QuestStatus.START, cumulative(step),
						new QuestEvent.EnterWorld()).isEmpty(),
					() -> "quest " + contract.questId() + " must not carry an enter-world heal edge on ladder step "
						+ probeStep);
			}
			assertTrue(plans(compiled, QuestStatus.REWARD, cumulative(3),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not carry an enter-world heal edge in REWARD");
		}
	}

	@Test
	void ninevehKeepsEveryClassRewardBranch() throws Exception {
		QuestDefinition definition = definition(28313).definition();
		ProgressLayout layout = definition.progressLayout();
		/* 完成段 = 源投影 REWARD/4161、目标投影 COMPLETE/0 的全部确认路由（真端规范确认段 8..23）。 */
		/* Completions = every confirm route from REWARD/4161 to COMPLETE/0 (retail confirm range 8..23). */
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> projectionStatus(definition, layout, route.sourceNode()) == QuestStatus.REWARD
				&& projectionStatus(definition, layout, route.targetNode()) == QuestStatus.COMPLETE)
			.toList();
		Set<Integer> confirmDialogs = completions.stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).dialogId())
			.collect(Collectors.toSet());
		Set<Integer> fullConfirmRange = new java.util.TreeSet<>();
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			fullConfirmRange.add(id);
		}
		assertEquals(fullConfirmRange, confirmDialogs, "28313 must keep the retail confirm range 8..23");
		/* 27 个 (职业, 物品) 分支：真端表逐职业点名的 27 件物品全部保留（× 16 确认 id = 432 条路由）。 */
		/* 27 (class, item) branches: every retail-named class item survives (×16 confirm ids = 432 routes). */
		Map<String, Set<Integer>> itemsByClass = new LinkedHashMap<>();
		for (QuestTransition route : completions) {
			PlayerClass playerClass = route.conditions().stream()
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(QuestCondition.AdvancedClassIs::playerClass)
				.findFirst()
				.orElseThrow(() -> new AssertionError("28313 reward branch " + route.event()
					+ " must stay class-gated"));
			List<Integer> classItems = route.actions().stream()
				.filter(action -> action instanceof QuestAction.GrantReward reward
					&& "ITEM".equals(reward.kind()))
				.map(action -> ((QuestAction.GrantReward) action).id())
				.toList();
			assertEquals(1, classItems.size(), () -> "28313 reward branch " + route.event()
				+ " must grant exactly its class item");
			itemsByClass.computeIfAbsent(playerClass.name(), ignored -> new java.util.TreeSet<>())
				.add(classItems.getFirst());
			assertTrue(route.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance),
				() -> "28313 reward branch " + route.event() + " must complete the quest");
		}
		assertEquals(11, itemsByClass.size(), "28313 reward branches must cover 11 advanced classes");
		assertEquals(27, itemsByClass.values().stream().mapToInt(Set::size).sum(),
			"28313 must keep all 27 class reward items");
	}

	/** 按 (状态, 打包投影) 找节点投影（真端网格标签与旧标签不同）。 / Node lookup by (status, packed). */
	private static Map<String, Integer> projectionAt(QuestDefinition definition, QuestStatus status,
			int packed) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == status
				&& definition.progressLayout().pack(node.projection().variables()) == packed)
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing node " + status + "/" + packed))
			.projection().variables();
	}

	private static int packedOf(ProgressLayout layout, Map<String, Integer> variables) {
		return layout.pack(variables);
	}

	private static QuestStatus projectionStatus(QuestDefinition definition, ProgressLayout layout,
			String label) {
		return definition.nodes().stream().filter(node -> label.equals(node.label()))
			.findFirst().map(node -> node.projection().status())
			.orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static Set<Integer> talkNpcIdsByStatus(QuestDefinition definition, QuestStatus sourceStatus,
			Integer sourcePacked, QuestStatus targetStatus) {
		return definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.filter(route -> {
				var source = nodeByLabel(definition, route.sourceNode());
				var target = nodeByLabel(definition, route.targetNode());
				return source != null && target != null
					&& source.projection().status() == sourceStatus
					&& target.projection().status() == targetStatus
					&& (sourcePacked == null || definition.progressLayout()
						.pack(source.projection().variables()) == sourcePacked);
			})
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
			.collect(Collectors.toSet());
	}

	private static QuestNode nodeByLabel(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> label.equals(node.label()))
			.findFirst().orElse(null);
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

	private static CompiledQuestDefinition definition(int questId) {
		// 生产视图：XML 目录 + 真端 overlay（退役行返回真端形状，未退役行返回 XML）。
		// Production view: XML directory plus the retail overlay.
		return ProductionQuestDefinitions.definition(questId);
	}
}
