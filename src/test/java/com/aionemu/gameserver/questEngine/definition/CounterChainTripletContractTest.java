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
 * 锁定 28313 的原版网格三槽合同与 2842/1841 的饱和领奖投影。
 * <p>
 * 28313 已在 P0c-9 退役为原版文件驱动（{@code <class>_selectable_reward} 职业奖励区块落地后采纳）。
 * 客户端合同不变：{@code Quest_unpacked/quest_monster.csv} 三条 0/1 计数记录（行 0..2 各占
 * {@code SECTION_n<1}），原版形状是三槽 6 位计数网格（var0/var1/var2 @ 0/6/12），每只（组）怪只推
 * 自己那一槽、**各维度独立推进（乱序击杀也计数——原版网格语义）**、领奖投影三槽全 1
 * （REWARD/4161）、完成段按 11 职业条件展开 27 件职业物品 × 确认段 dialogId 8..23。
 * 旧 XML 的"步骤号单槽 + 乱序不计数 + EnterWorld 存档自愈边"是历史错误/历史形状，已按
 * 原版权威退役；自愈边按 P0c-6 先例登记进 p0c6-legacy-save-normalization.tsv（可选 DB 归一化）。
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
	 * 18033/28033 已在 P0c-4 退役为原版区域发放（{@code _area_}）：原版形状是"产性格子
	 * （{@code a0b0c0..a1b1c1}）+ SystemGrant 边"，不是本 XML 的链式 {@code k1..k3} 阶梯，
	 * 也不再有点名接取 NPC；其原版 IR 由 {@code retail-simple-hunt-adjudicated-ir-fingerprints.tsv} 冻结，
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
	void armyCompletionsGrantTheFullFixedRewardSet() {
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


	/** 按 (状态, 打包投影) 找节点投影（原版网格标签与旧标签不同）。 / Node lookup by (status, packed). */
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
		// 生产视图：XML 目录 + 原版 overlay（退役行返回原版形状，未退役行返回 XML）。
		// Production view: XML directory plus the retail overlay.
		return ProductionQuestDefinitions.definition(questId);
	}
}
