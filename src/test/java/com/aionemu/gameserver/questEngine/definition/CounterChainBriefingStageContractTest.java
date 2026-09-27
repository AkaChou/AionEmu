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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 26：简报阶段（SECTION_5 标志位）+ Named/Boss 双层计数族（24112 / 30600 / 30610）。
 * <p>
 * 族级判据：客户端 quest_monster 把任务书第一/第二个击杀行写成链式 0/1 计数器并附加 `SECTION_5==0`
 * （`Progress(SECTION_0<1; SECTION_5==0)`、`Progress(SECTION_1<1; SECTION_0==1)`）——`SECTION_5` 是
 * **简报标志位**：接取时置 1（任务书亮“去见简报 NPC”的那一行），与简报 NPC 对话后清 0，击杀计数行才出现。
 * 旧 handler 直接证明这条语义：`_24112NoLaissezfaireforLepharists` 在 `ACCEPT_QUEST_SIMPLE` 里
 * `setQuestVarById(5, 1)`、在与 Brodir 的 `STEP_TO_1` 里 `setQuestVarById(5, 0)`，击杀 210510 时
 * `setQuestVarById(0, 1)`，最后 `SELECT_REWARD` 置 REWARD（var0=1、var5=0）。
 * <p>
 * 旧定义的两类缺陷：24112 的 reward 投影是 var0=0（击杀已把 var0 推到 1）——领奖态存档既不匹配 `reward`
 * 节点也不匹配任何路线，玩家在 Brodir 处卡死；30600/30610 只有一个 `var0` 步骤号（0/1/2）、`var1` 缺失
 * （客户端行 2 的计数永远是 0/1），并且有两条**无守卫**的 `started --SETPRO1--> reward` 直跳，
 * 领奖 owner 还落在静态 spawn 与实例/AI 代码里都没有出场点的 205842(Ancanus)/205864(Udvi) 上。
 * 本门禁同时锁死：简报标志位的接取/清除、每只（组）怪只推自己那一槽、击杀必须发生在简报之后、
 * 报告与 completion owner 收敛到任务书行 0/行 3 点名的 NPC、饱和领奖投影。
 * <p>
 * P0c-6 起 24112 也由真端表驱动（XML 已退役）：它的"存档修复边"按 P3 既有裁定一并移除
 * （旧 XML 的 AionEmu 历史包袱，真端表没有修复列），因此单槽族与双槽族共用同一组
 * "retail 不迁移旧存档"断言。节点名不进契约：门禁用 (状态, 投影) 语义查找节点，
 * 网格族（a0/a1..）与串行族（briefed/k1..）的命名差异不应影响本文件要锁的语义。
 * <p>
 * Locks batch 26: the briefing flag (SECTION_5) plus the named/boss counter ladders of 24112/30600/30610,
 * the journal-NPC owners (24112 Nokir -&gt; Brodir; 30600 Hejitor/Linocus; 30610 Astella/Aluna), the removal of
 * the unguarded SETPRO1 reward jumps and of the unreachable 205842/205864 owners. Since P0c-6, 24112 is
 * retail-driven too and its legacy save-repair edges are gone (P3 adjudication: no repair column in retail).
 */
class CounterChainBriefingStageContractTest {

	/** 任务 / 接取 NPC / 简报 NPC / 行 3 报告 NPC / Named 目标 / Boss 目标 / 是否双层计数。 */
	private record Contract(int questId, int acceptNpc, int briefNpc, int reportNpc,
			List<Integer> namedKills, Integer bossKill) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(24112, 203631, 832821, 832821, List.of(210510), null),
		new Contract(30600, 800325, 800324, 800325, List.of(219256, 219257), 219264),
		new Contract(30610, 800327, 800326, 800327, List.of(219256, 219257), 219264));

	/** 旧 handler 里已证明无出场点的 owner（静态 spawn 与实例/AI 代码都没有这些 NPC）。 */
	private static final Set<Integer> UNREACHABLE_LEGACY_OWNERS = Set.of(205842, 205864);

	@Test
	void everyClientRowOwnsItsCounterSlotAndTheBriefingFlag() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();
			assertEquals(0, layout.field("var0").offset(),
				() -> "quest " + contract.questId() + " var0 must map SECTION_0");
			assertEquals(30, layout.field("var5").offset(),
				() -> "quest " + contract.questId() + " var5 must map SECTION_5");
			assertEquals(1, layout.field("var5").maxValue(),
				() -> "quest " + contract.questId() + " var5 must stay a 0/1 briefing flag");
			if (contract.bossKill() != null) {
				assertEquals(6, layout.field("var1").offset(),
					() -> "quest " + contract.questId() + " var1 must map SECTION_1 (client row 2)");
				assertEquals(3, layout.fields().size(),
					() -> "quest " + contract.questId() + " declares exactly three fields");
			} else {
				assertEquals(2, layout.fields().size(),
					() -> "quest " + contract.questId() + " declares exactly two fields");
			}

			/* 接取后 = 标志位 1 的 START 节点；听完简报 = 零计数 START 节点（网格/串行族命名不同）。 */
			/* After accept the flag is raised; the briefing clears it. Node naming stays a family detail. */
			assertNode(definition, QuestStatus.START, slots(contract, 0, false, true), "briefing-pending");
			assertNode(definition, QuestStatus.START, slots(contract, 0, false, false), "briefed");
			assertNode(definition, QuestStatus.START, slots(contract, 1, false, false), "first-kill");
			if (contract.bossKill() != null) {
				assertNode(definition, QuestStatus.START, slots(contract, 1, true, false), "boss-kill");
			}
			assertNode(definition, QuestStatus.REWARD,
				slots(contract, 1, contract.bossKill() != null, false), "reward");
			assertNode(definition, QuestStatus.COMPLETE, slots(contract, 0, false, false), "complete");
			assertNode(definition, QuestStatus.NONE, slots(contract, 0, false, false), "unaccepted");
		}
	}

	@Test
	void briefingTalkClearsTheFlagInOneStepAndKillsStayGated() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 规范形简报（quest-native-dispatch P0-2，网格族与串行族同形）：简报 NPC 的 QUEST_SELECT
			 * 一步清标志位直达首段节点（SELECT2 页链不再由服务端驱动，SETPRO1 按钮路由随之消失）；
			 * "击杀不得跳过简报"语义由节点承担——briefing-pending 节点不带击杀边。
			 * Canonical briefing (quest-native-dispatch P0-2, same shape for the grid and serial
			 * families): the briefing NPC's QUEST_SELECT clears the flag and lands on the first stage
			 * node in one step (the SELECT2 page chain is no longer server-driven, so the SETPRO1
			 * button route is gone); "no kill shortcuts the briefing" is carried by the nodes — the
			 * briefing-pending node has no kill edges. */
			List<QuestMutationPlan> briefings = plans(compiled, QuestStatus.START,
				slots(contract, 0, false, true),
				new QuestEvent.TalkToNpc(contract.briefNpc(), QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(1, briefings.size(), () -> "quest " + contract.questId()
				+ " must answer the journal row-0 briefing talk");
			QuestMutationPlan briefing = briefings.getFirst();
			assertEquals(QuestStatus.START, briefing.nextStatus(),
				() -> "quest " + contract.questId() + " briefing stays in START");
			assertEquals(slots(contract, 0, false, false), unpack(compiled, briefing),
				() -> "quest " + contract.questId() + " briefing must clear var5 in one step");
			assertTrue(briefing.afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " briefing must refresh journal visibility");
			assertTrue(briefing.afterCommit().contains(new AfterCommitAction.CloseDialog()),
				() -> "quest " + contract.questId() + " briefing must close the dialog");
			assertTrue(briefing.afterCommit().stream().noneMatch(
					AfterCommitAction.ShowQuestDialog.class::isInstance),
				() -> "quest " + contract.questId() + " briefing must not open any page");
			assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
					new QuestEvent.TalkToNpc(contract.briefNpc(), QuestDialogAction.SETPRO1.id())).isEmpty(),
				() -> "quest " + contract.questId() + " must drop the legacy SETPRO1 briefing button");

			/* 击杀不得跳过简报：标志位仍为 1 时所有击杀都不计数。 */
			/* Kills must not shortcut the briefing. */
			for (int npcId : contract.namedKills()) {
				assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
						new QuestEvent.KillNpc(npcId)).isEmpty(),
					() -> "quest " + contract.questId() + " kill " + npcId
						+ " must not count before the briefing talk");
			}
			if (contract.bossKill() != null) {
				assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
						new QuestEvent.KillNpc(contract.bossKill())).isEmpty(),
					() -> "quest " + contract.questId() + " boss kill must not count before the briefing");
			}
		}
	}

	@Test
	void eachKillAdvancesOnlyItsOwnSlotInOrder() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 行 1：任一 Named 指挥官都只推 SECTION_0。 */
			/* Row 1: either named commander advances only SECTION_0. */
			for (int npcId : contract.namedKills()) {
				List<QuestMutationPlan> plans = plans(compiled, QuestStatus.START, slots(contract, 0, false, false),
					new QuestEvent.KillNpc(npcId));
				assertEquals(1, plans.size(), () -> "quest " + contract.questId() + " named kill " + npcId);
				assertEquals(slots(contract, 1, false, false), unpack(compiled, plans.getFirst()),
					() -> "quest " + contract.questId() + " named kill " + npcId + " must set var0 only");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					plans.getFirst().afterCommit(),
					() -> "quest " + contract.questId() + " named kill must sync PACKET_ONLY");
				/* 回看：Named 打完之后再打一次不产生计划。 */
				/* Backward kills have no plan. */
				assertTrue(plans(compiled, QuestStatus.START, slots(contract, 1, false, false),
						new QuestEvent.KillNpc(npcId)).isEmpty(),
					() -> "quest " + contract.questId() + " named kill " + npcId + " must not count twice");
			}
			if (contract.bossKill() == null) {
				continue;
			}
			/* 行 2：舰长只推 SECTION_1，且必须排在 Named 之后。 */
			/* Row 2: the boss advances only SECTION_1 and only after the named commander. */
			assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, false),
					new QuestEvent.KillNpc(contract.bossKill())).isEmpty(),
				() -> "quest " + contract.questId() + " boss must not count before the named commander");
			List<QuestMutationPlan> boss = plans(compiled, QuestStatus.START, slots(contract, 1, false, false),
				new QuestEvent.KillNpc(contract.bossKill()));
			assertEquals(1, boss.size(), () -> "quest " + contract.questId() + " boss kill");
			assertEquals(slots(contract, 1, true, false), unpack(compiled, boss.getFirst()),
				() -> "quest " + contract.questId() + " boss kill must set var1 only");
			assertTrue(plans(compiled, QuestStatus.REWARD, slots(contract, 1, true, false),
					new QuestEvent.KillNpc(contract.bossKill())).isEmpty(),
				() -> "quest " + contract.questId() + " must not count kills in REWARD");
		}
	}

	@Test
	void journalNpcOwnersConvergeAndUnreachableLegacyOwnersAreGone() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			String unaccepted = nodeLabel(definition, QuestStatus.NONE, slots(contract, 0, false, false));
			String started = nodeLabel(definition, QuestStatus.START, slots(contract, 0, false, true));
			String briefed = nodeLabel(definition, QuestStatus.START, slots(contract, 0, false, false));
			String killed = nodeLabel(definition, QuestStatus.START, slots(contract, 1, false, false));
			String reward = nodeLabel(definition, QuestStatus.REWARD,
				slots(contract, 1, contract.bossKill() != null, false));
			/* 双层族从满段节点（boss 击杀后）报告，单层族从首个击杀节点报告。 */
			/* The two-slot family reports from the boss node, the other from the first kill. */
			String reportSource = contract.bossKill() == null ? killed
				: nodeLabel(definition, QuestStatus.START, slots(contract, 1, true, false));
			assertEquals(Set.of(contract.acceptNpc()), talkNpcIds(definition, unaccepted, started),
				() -> "quest " + contract.questId() + " must accept from the journal NPC");
			assertEquals(Set.of(contract.briefNpc()), talkNpcIds(definition, started, briefed),
				() -> "quest " + contract.questId() + " must take the briefing from the journal row-0 NPC");
			assertEquals(Set.of(contract.reportNpc()), talkNpcIds(definition, reportSource, reward),
				() -> "quest " + contract.questId() + " must report on the journal report-row NPC");
			assertEquals(Set.of(contract.reportNpc()),
				talkNpcIds(definition, reward, nodeLabel(definition, QuestStatus.COMPLETE,
					slots(contract, 0, false, false))),
				() -> "quest " + contract.questId() + " must complete on the journal report-row NPC");
			Set<Integer> allNpcs = definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
				.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
				.collect(Collectors.toSet());
			for (int unreachable : UNREACHABLE_LEGACY_OWNERS) {
				assertFalse(allNpcs.contains(unreachable), () -> "quest " + contract.questId()
					+ " must not keep the unreachable legacy owner " + unreachable);
			}
			/* 无守卫的 started -> reward 直跳必须全部消失（否则可以接取后立刻领奖）。 */
			/* No unguarded shortcut into reward may remain. */
			assertTrue(definition.transitions().stream().noneMatch(route ->
					started.equals(route.sourceNode()) && "reward".equals(route.targetNode())),
				() -> "quest " + contract.questId() + " must not keep an unguarded reward shortcut");
		}
	}

	@Test
	void legacySavesMigrateOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 新模型自身的三个 START 状态不得被迁移边重写。 */
			/* The new model's own START states must not be rewritten. */
			assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not migrate the fresh started state");
			assertTrue(plans(compiled, QuestStatus.START, slots(contract, 0, false, false),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not migrate the briefed state");
			assertTrue(plans(compiled, QuestStatus.START, slots(contract, 1, false, false),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not migrate the k1 state");
			/* 正规领奖态同样不得被自愈边重放。 */
			/* The legitimate reward state must not be replayed either. */
			assertTrue(plans(compiled, QuestStatus.REWARD, slots(contract, 1, contract.bossKill() != null, false),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not resync the saturated reward state");

			if (contract.bossKill() == null) {
				/* 24112 自 P0c-6 起也由真端表驱动（XML 已退役）：旧 XML 的 AionEmu 历史包袱
				 * （"接取后未听简报就先杀怪" 的归一化、旧 reward 投影 var0=0 的自愈）按 P3 既有裁定
				 * 一并移除——真端表没有"存档修复"列。一次性 DB 归一化是可选项，登记在 P0c-6 报告。 */
				/* 24112 is retail-driven since P0c-6: the legacy save-repair edges are gone (P3 ruling);
				 * the optional one-time DB normalization is registered in the P0c-6 report. */
				assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", 1, "var5", 1),
					new QuestEvent.EnterWorld()).isEmpty(),
					() -> "quest " + contract.questId() + " must not normalize the kill-before-briefing save");
				assertTrue(plans(compiled, QuestStatus.REWARD, Map.of("var0", 0),
					new QuestEvent.EnterWorld()).isEmpty(),
					() -> "quest " + contract.questId() + " must not heal the old reward save");
				continue;
			}
			/* 30600/30610 已按真端串行表驱动：旧 step 档（var0=2 的步骤号）不再迁移，也没有自愈边；
			 * P3 裁定见 P3 报告与 p3-serial-hunt-decisions.tsv。 */
			/* 30600/30610 are retail-driven: legacy step saves are not migrated and no repair edge remains. */
			assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", 2), new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not migrate the legacy step-2 save");
			assertTrue(plans(compiled, QuestStatus.REWARD, Map.of("var0", 2), new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId() + " must not heal the legacy reward projection");
		}
	}

	@Test
	void completionsGrantTheFullFixedRewardSet() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			int expectedRewards = contract.bossKill() == null ? 3 : 4;
			assertEquals(expectedRewards, compiled.definition().metadata().rewards().size(),
				() -> "quest " + contract.questId() + " reward entries");
			QuestMutationPlan completion = plans(compiled, QuestStatus.REWARD,
				slots(contract, 1, contract.bossKill() != null, false),
				new QuestEvent.TalkToNpc(contract.reportNpc(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()))
				.stream().findFirst().orElseThrow(() -> new AssertionError("quest " + contract.questId()
					+ " must complete from the saturated reward state"));
			assertEquals(QuestStatus.COMPLETE, completion.nextStatus(),
				() -> "quest " + contract.questId() + " completion status");
			assertTrue(completion.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance),
				() -> "quest " + contract.questId() + " must complete the quest");
			long grantedItems = completion.requiredActions().stream()
				.filter(QuestAction.GrantReward.class::isInstance)
				.map(QuestAction.GrantReward.class::cast)
				.filter(reward -> "ITEM".equals(reward.kind()))
				.count();
			assertEquals(1, grantedItems,
				() -> "quest " + contract.questId() + " must keep the ITEM reward slot");
			assertEquals(expectedRewards, completion.requiredActions().stream()
					.filter(QuestAction.GrantReward.class::isInstance).count(),
				() -> "quest " + contract.questId() + " must grant every fixed reward index");
		}
	}

	/** 目标状态：var0[/var1] 计数 + 简报标志位。 / Expected state: counter slots plus the briefing flag. */
	private static Map<String, Integer> slots(Contract contract, int named, boolean boss, boolean briefing) {
		Map<String, Integer> variables = new LinkedHashMap<>();
		variables.put("var0", named);
		if (contract.bossKill() != null) {
			variables.put("var1", boss ? 1 : 0);
		}
		variables.put("var5", briefing ? 1 : 0);
		return variables;
	}

	/**
	 * 按 (状态, 投影) 语义定位节点标签：网格族叫 a0/a1..、串行族叫 briefed/k1..，命名是家族细节，
	 * 语义才是契约。同一 (状态, 投影) 必须唯一——否则家族形状本身自相矛盾。
	 * Semantic (status, projection) node lookup: naming is a family detail, the state is the contract.
	 */
	private static String nodeLabel(QuestDefinition definition, QuestStatus status, Map<String, Integer> state) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().equals(state))
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), () -> "quest " + definition.id() + " must declare exactly one "
			+ status + " node projecting " + state + ", got " + labels);
		return labels.getFirst();
	}

	/** 该 (状态, 投影) 的节点必须存在且唯一。 / The (status, projection) node must exist exactly once. */
	private static void assertNode(QuestDefinition definition, QuestStatus status, Map<String, Integer> state,
			String role) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().equals(state))
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), () -> "quest " + definition.id() + " must declare exactly one " + role
			+ " node (" + status + " projecting " + state + "), got " + labels);
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

	/**
	 * 生产零售视图：30600/30610 已退役 XML（24112 仍是 XML 家族），统一走生产定义入口。
	 * Production retail view: 30600/30610 have no XML anymore and resolve through the retail table,
	 * while 24112 still comes from the XML directory; both are exposed by the same production entry point.
	 */
	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}
}
