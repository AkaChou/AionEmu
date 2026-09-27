package com.aionemu.gameserver.questEngine.definition;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 21：精锐兵族 13918（天族）/ 23918（魔族）的链式 0/1 击杀阶梯。
 * <p>
 * 族级判据：两侧客户端 {@code quest_q13918.html} / {@code quest_q23918.html} 的 {@code quest_summary} 都是 6 行
 * （行 0..4 = 依次消灭五只特殊精锐兵，每行 {@code ([%n]/1)}；行 5 = 向 {@code STR_DIC_N_LDF4_Advance_Elger_E}(802350) /
 * {@code STR_DIC_N_LDF4_Advance_Helgund_E}(802353) 报告），而 {@code Quest_unpacked/quest_monster.csv} 的门控是
 * **链式**的：{@code SECTION_0<1; SECTION_5==0}、{@code SECTION_1<1; SECTION_0==1}、
 * {@code SECTION_2<1; SECTION_1==1}、{@code SECTION_3<1; SECTION_2==1}、{@code SECTION_4<1; SECTION_3==1}
 * —— 每槽是 0/1 计数器，行 n 只有前一槽 ==1 时才可见（COUNTER_CHAIN 家族，判别式见
 * {@code .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-sweep.zh-CN.md}）。
 * <p>
 * 旧模型两侧都错：13918 把 var0 当行号跑 step 链（行 2 要求 {@code SECTION_1==1}，而 var1 只在领奖投影里才为 1，
 * 第 3 只之后再没有行能亮）且缺 var4 位域；23918 是 c44c50bd0 建的 5 维组合网格（32 节点自由顺序），
 * 乱序击杀会写出客户端无法显示的状态，还把 {@code fixed-reward-indices="0 1 2"} 误改成 {@code 0 1}
 * （丢掉 metadata 第三条 ITEM 169405255×6），领奖 owner 也仍是接取 NPC（QE-052）。
 * <p>
 * Locks batch 21: the five-slot chained 0/1 ladder of the mirror pair 13918/23918 together with the owner trim
 * and the restored ITEM reward index. Out-of-order kills must not count (the client can only render the elite the
 * player still owes), the offer stays on the offer NPC, and the report/completion stay on the row-5 NPC.
 * <p>
 * P3 裁定（真端优先）：旧 XML 的 EnterWorld 旧档修复边不属于真端形状，已随 XML 退役移除；本门禁反向锁定
 * "不得再引入迁移边"，存量进度按新阶梯重读（判据见 {@code reports/2026-09-24-P3-simple-serial-hunt.zh-CN.md}）。
 * P3 adjudication: the legacy enter-world repair edges are not part of the retail shape and were removed with the
 * XML; this gate locks their absence. The definition now comes from the retail production view, not the XML.
 */
class ChainEliteLadderContractTest {

	/** 任务 / 镜像 / 接取 NPC / 行 5 NPC / 五只精锐兵。 */
	private record Contract(int questId, int mirrorId, int offerNpc, int reportNpc, List<Integer> kills) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(13918, 23918, 802328, 802350,
			List.of(235321, 235322, 235323, 235324, 235325)),
		new Contract(23918, 13918, 802347, 802353,
			List.of(235559, 235560, 235561, 235326, 235327)));

	private static final List<String> FIELDS = List.of("var0", "var1", "var2", "var3", "var4");
	private static final List<String> LADDER_NODES = List.of("started", "k1", "k2", "k3", "k4", "k5");
	private static final int GOLD = 451980;
	private static final int EXP = 7927072;
	private static final int ITEM = 169405255;
	private static final int ITEM_COUNT = 6;

	/** 行 n（0 基）推进后应有的五槽状态：前 n+1 槽为 1。 / Slots set after the kill closing row n (0-based). */
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
			}
			/* SECTION_5 必须保持未声明（客户端行 0 的额外门控 SECTION_5==0）。 */
			/* SECTION_5 must stay undeclared: the client row 0 additionally gates on SECTION_5==0. */
			assertEquals(Set.of("var0", "var1", "var2", "var3", "var4"),
				layout.fields().stream().map(BitField::name).collect(Collectors.toSet()),
				() -> "quest " + contract.questId() + " must declare exactly the five chained slots");
			assertEquals(Set.of("unaccepted", "started", "k1", "k2", "k3", "k4", "k5", "reward", "complete"),
				definition.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				() -> "quest " + contract.questId() + " must not keep the legacy combination grid nodes");

			for (int ones = 0; ones <= 5; ones++) {
				String label = LADDER_NODES.get(ones);
				assertEquals(cumulative(ones), node(definition, label).projection().variables(),
					() -> "quest " + contract.questId() + " node " + label + " projection");
				assertEquals(QuestStatus.START, node(definition, label).projection().status(),
					() -> "quest " + contract.questId() + " node " + label + " status");
			}
			assertEquals(cumulative(5), node(definition, "reward").projection().variables(),
				() -> "quest " + contract.questId() + " reward must saturate all five slots");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward status");
			assertEquals(cumulative(0), node(definition, "complete").projection().variables(),
				() -> "quest " + contract.questId() + " complete must clear the counters");

			/* 镜像同形：两侧五槽与节点集合必须一致。 / Mirror shape: same slots and nodes on both sides. */
			QuestDefinition mirror = definition(contract.mirrorId()).definition();
			assertEquals(definition.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				mirror.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				() -> "mirror pair " + contract.questId() + "/" + contract.mirrorId() + " node shape");
			/* 击杀面权威 = 串行阶梯的 KillNpc 边（真端客户端阶段契约登记）；XML 时代的
			 * {@code metadata.kills} 零售元数据不填、生产代码零消费，漂移登记在 P3 报告。 */
			/* Kill authority is the ladder's KillNpc edges from the retail client stage registry; the
			 * XML-only metadata.kills list is not populated by the retail metadata and is unused in production. */
			assertEquals(contract.kills().stream().sorted().toList(),
				killTargets(definition).stream().sorted().toList(),
				() -> "quest " + contract.questId() + " kill edges must name the five client elites");
			assertEquals(5, killTargets(mirror).size(),
				() -> "mirror quest " + contract.mirrorId() + " must also declare five kill edges");
		}
	}

	@Test
	void eachEliteAdvancesExactlyItsOwnSlot() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			for (int index = 0; index < contract.kills().size(); index++) {
				int slot = index;
				int npcId = contract.kills().get(index);
				List<QuestMutationPlan> plans = plans(compiled, QuestStatus.START, cumulative(index),
					new QuestEvent.KillNpc(npcId));
				assertEquals(1, plans.size(), () -> "quest " + contract.questId() + " elite " + npcId
					+ " must answer on ladder step " + slot);
				QuestMutationPlan plan = plans.getFirst();
				assertEquals(QuestStatus.START, plan.nextStatus(),
					() -> "quest " + contract.questId() + " elite " + npcId + " stays in START");
				assertEquals(cumulative(index + 1), unpack(compiled, plan),
					() -> "quest " + contract.questId() + " elite " + npcId + " must set exactly var" + slot);
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					plan.afterCommit(),
					() -> "quest " + contract.questId() + " elite " + npcId + " must sync PACKET_ONLY");
			}
		}
	}

	@Test
	void outOfOrderOrBackwardKillsHaveNoPlan() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			for (int index = 0; index < contract.kills().size(); index++) {
				int expectedStep = index;
				int npcId = contract.kills().get(index);
				for (int step = 0; step <= 5; step++) {
					if (step == expectedStep) {
						continue;
					}
					int probeStep = step;
					assertTrue(plans(compiled, QuestStatus.START, cumulative(step), new QuestEvent.KillNpc(npcId))
							.isEmpty(),
						() -> "quest " + contract.questId() + " elite " + npcId + " must not count on ladder step "
							+ probeStep + " (client journal row " + expectedStep + ")");
				}
				/* 领奖态下不再计数：所有击杀只在 START 阶梯上响应。 */
				/* No counting in the reward stage: kills only answer on the START ladder. */
				assertTrue(plans(compiled, QuestStatus.REWARD, cumulative(5), new QuestEvent.KillNpc(npcId)).isEmpty(),
					() -> "quest " + contract.questId() + " elite " + npcId + " must not count in REWARD");
			}
		}
	}

	@Test
	void offerAndCompletionStayOnTheRowFiveNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Set.of(contract.offerNpc()), npcIds(definition, "unaccepted", "started"),
				() -> "quest " + contract.questId() + " offer must stay on the offer NPC " + contract.offerNpc());
			assertEquals(Set.of(contract.reportNpc()), npcIds(definition, "k5", "reward"),
				() -> "quest " + contract.questId() + " report must move to the row-5 NPC " + contract.reportNpc());
			assertEquals(Set.of(contract.reportNpc()), npcIds(definition, "reward", "complete"),
				() -> "quest " + contract.questId() + " completion must stay on the row-5 NPC");
			assertTrue(definition.transitions().stream()
					.filter(route -> "complete".equals(route.targetNode()))
					.allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == contract.reportNpc()),
				() -> "quest " + contract.questId() + " must not complete on any other NPC");

			/* 行 5：QUEST_SELECT 打开客户端 report 页 SELECT2；SELECT_QUEST_REWARD 才切到 REWARD 并弹领奖窗口。 */
			/* Row 5: QUEST_SELECT opens the client SELECT2 report page; SELECT_QUEST_REWARD moves to REWARD. */
			QuestTransition reportPage = definition.transitions().stream()
				.filter(route -> "k5".equals(route.sourceNode()) && "k5".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.reportNpc()
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " must answer QUEST_SELECT on the row-5 NPC"));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				reportPage.afterCommit(),
				() -> "quest " + contract.questId() + " must open the client report page SELECT2 from k5");

			QuestTransition rewardHop = definition.transitions().stream()
				.filter(route -> "k5".equals(route.sourceNode()) && "reward".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.reportNpc())
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " must move from k5 to reward on the row-5 NPC"));
			assertTrue(rewardHop.afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " report must refresh the visibility on the reward hop");
			assertTrue(rewardHop.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				() -> "quest " + contract.questId() + " report must open the reward window on the reward hop");

			/* 领奖/报告 owner 不再兼任接取：接取 NPC 上不得有完成或报告路由。 */
			/* The offer NPC must no longer own the report or the completion routes. */
			for (QuestTransition route : definition.transitions()) {
				if (!(route.event() instanceof QuestEvent.TalkToNpc talk) || talk.npcId() != contract.offerNpc()) {
					continue;
				}
				assertFalse("complete".equals(route.targetNode())
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id(),
					() -> "quest " + contract.questId() + " offer NPC must not report or complete (QE-052)");
			}
		}
	}

	@Test
	void completionGrantsTheFullFixedRewardSet() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestReward> rewards = compiled.definition().metadata().rewards();
			assertEquals(3, rewards.size(), () -> "quest " + contract.questId() + " declares three rewards");
			QuestMutationPlan completion = plans(compiled, QuestStatus.REWARD, cumulative(5),
				new QuestEvent.TalkToNpc(contract.reportNpc(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()))
				.stream().findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " must complete from the saturated reward state"));
			assertEquals(QuestStatus.COMPLETE, completion.nextStatus(),
				() -> "quest " + contract.questId() + " completion status");
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("GOLD", 0, GOLD, QuestRewardAmountMode.QUEST_BASE)),
				() -> "quest " + contract.questId() + " must grant the GOLD reward");
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("EXP", 0, EXP, QuestRewardAmountMode.QUEST_BASE)),
				() -> "quest " + contract.questId() + " must grant the EXP reward");
			/* fixed-reward-indices 必须覆盖 metadata 第三条 ITEM（c44c50bd0 把 13918/23918 的 0 1 2 改成 0 1 过）。 */
			/* fixed-reward-indices must cover the third metadata entry; c44c50bd0 wrongly dropped it. */
			assertTrue(completion.requiredActions().contains(
					new QuestAction.GrantReward("ITEM", ITEM, ITEM_COUNT, QuestRewardAmountMode.EXACT)),
				() -> "quest " + contract.questId() + " must grant ITEM " + ITEM + " x" + ITEM_COUNT);
			assertTrue(completion.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance),
				() -> "quest " + contract.questId() + " must complete the quest");
		}
	}

	@Test
	void legacyStepModelSavesAreNotRewrittenByTheRetailShape() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* P3 裁定：旧 step/grid 存档不再迁移，按新阶梯重读；不得再引入 EnterWorld 修复边。 */
			/* P3 decision: stale saves are re-read on the new ladder; no enter-world repair edge may return. */
			for (int done = 2; done <= 5; done++) {
				int killsDone = done;
				assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", done), new QuestEvent.EnterWorld()).isEmpty(),
					() -> "quest " + contract.questId() + " must not carry a step-model heal edge for var0="
						+ killsDone);
			}
			for (int ones : List.of(0, 1)) {
				assertTrue(plans(compiled, QuestStatus.START, cumulative(ones), new QuestEvent.EnterWorld()).isEmpty(),
					() -> "quest " + contract.questId() + " must not rewrite live ladder states on enter-world");
			}
		}
	}

	@Test
	void saturatedRewardStateStillOpensTheClientRewardWindow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestStatus reward = QuestStatus.REWARD;
			/* 正规领奖态（五槽全 1）不得被迁移边命中，且仍能直接打开领奖窗。 */
			/* The legitimate reward state must not be rewritten and must still open the reward window. */
			assertTrue(plans(compiled, reward, cumulative(5), new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not resync a saturated reward state on enter-world");
			List<QuestMutationPlan> preview = plans(compiled, reward, cumulative(5),
				new QuestEvent.TalkToNpc(contract.reportNpc(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
			assertTrue(preview.stream().anyMatch(plan -> plan.afterCommit().contains(
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))),
				() -> "quest " + contract.questId() + " must open the reward window from REWARD");
			/* 旧 step 领奖投影（var0=5/var1..3=1）也不再被"修复"成满槽。 */
			/* Stale reward projections are no longer healed into the saturated counters either. */
			Map<String, Integer> staleReward = Map.of("var0", 5, "var1", 1, "var2", 1, "var3", 1);
			assertTrue(plans(compiled, reward, staleReward, new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not heal the legacy reward projection");
		}
	}

	/** 阶梯的击杀目标集（KillNpc 边）。 / Kill targets carried by the ladder's KillNpc edges. */
	private static Set<Integer> killTargets(QuestDefinition definition) {
		return definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.KillNpc.class::isInstance)
			.map(QuestEvent.KillNpc.class::cast)
			.map(QuestEvent.KillNpc::npcId)
			.collect(Collectors.toSet());
	}

	private static Set<Integer> npcIds(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
			.collect(Collectors.toSet());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
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

	/**
	 * 生产零售视图：13918/23918 已退役 XML，定义只能来自真端 SimpleSerialHunt 表合成。
	 * Production retail view: the XML of both ids is retired, so the definition must come from the
	 * retail SimpleSerialHunt synthesis.
	 */
	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}
}
