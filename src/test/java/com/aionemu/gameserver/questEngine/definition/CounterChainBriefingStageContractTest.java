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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * 报告与 completion owner 收敛到任务书行 0/行 3 点名的 NPC、旧存档迁移与饱和领奖投影。
 * <p>
 * Locks batch 26: the briefing flag (SECTION_5) plus the named/boss counter ladders of 24112/30600/30610,
 * the journal-NPC owners (24112 Nokir -&gt; Brodir; 30600 Hejitor/Linocus; 30610 Astella/Aluna),
 * the removal of the unguarded SETPRO1 reward jumps and of the unreachable 205842/205864 owners.
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

			/* 接取后 = started（简报标志位 1）；听完简报 = briefed（标志位 0）。 */
			/* After accept the flag is raised; the briefing clears it. */
			assertEquals(slots(contract, 0, false, true), projection(definition, "started"),
				() -> "quest " + contract.questId() + " started projection");
			assertEquals(slots(contract, 0, false, false), projection(definition, "briefed"),
				() -> "quest " + contract.questId() + " briefed projection");
			if (contract.bossKill() == null) {
				/* 单槽族：击杀态节点是 killed。 */
				/* Single-slot family: the killed count is the terminal START node. */
				assertEquals(slots(contract, 1, false, false), projection(definition, "killed"),
					() -> "quest " + contract.questId() + " killed projection");
			} else {
				assertEquals(slots(contract, 1, false, false), projection(definition, "k1"),
					() -> "quest " + contract.questId() + " k1 projection");
				assertEquals(slots(contract, 1, true, false), projection(definition, "k2"),
					() -> "quest " + contract.questId() + " k2 projection");
			}
			assertEquals(slots(contract, 1, contract.bossKill() != null, false),
				projection(definition, "reward"),
				() -> "quest " + contract.questId() + " reward must project the saturated counters");
			assertEquals(slots(contract, 0, false, false), projection(definition, "complete"),
				() -> "quest " + contract.questId() + " complete projection");
			assertEquals(slots(contract, 0, false, false), projection(definition, "unaccepted"),
				() -> "quest " + contract.questId() + " unaccepted projection");
		}
	}

	@Test
	void briefingPageKeepsTheFlagAndTheEndDialogButtonClearsIt() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			/* 行 0a：QUEST_SELECT 只回应对话并展示简报页，标志位保持不变。 */
			/* Row 0a: QUEST_SELECT only opens the briefing page and keeps the flag raised. */
			List<QuestMutationPlan> briefings = plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
				new QuestEvent.TalkToNpc(contract.briefNpc(), QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(1, briefings.size(), () -> "quest " + contract.questId()
				+ " must answer the journal row-0 briefing talk");
			QuestMutationPlan briefing = briefings.getFirst();
			assertEquals(QuestStatus.START, briefing.nextStatus(),
				() -> "quest " + contract.questId() + " briefing stays in START");
			assertEquals(slots(contract, 0, false, true), unpack(compiled, briefing),
				() -> "quest " + contract.questId() + " must keep var5 while the select2 page is open");
			assertTrue(briefing.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SELECT2.id())),
				() -> "quest " + contract.questId() + " briefing must open the client select2 page");

			/* 行 0b：select2 页唯一可见按钮 HACTION_SETPRO1(10000) 才清标志位并推进到 briefed。 */
			/* Row 0b: the select2 page's only visible button HACTION_SETPRO1(10000) clears the flag. */
			List<QuestMutationPlan> endDialog = plans(compiled, QuestStatus.START, slots(contract, 0, false, true),
				new QuestEvent.TalkToNpc(contract.briefNpc(), QuestDialogAction.SETPRO1.id()));
			assertEquals(1, endDialog.size(), () -> "quest " + contract.questId()
				+ " must route the visible SETPRO1 end-dialog button");
			QuestMutationPlan briefed = endDialog.getFirst();
			assertEquals(QuestStatus.START, briefed.nextStatus(),
				() -> "quest " + contract.questId() + " end-dialog stays in START");
			assertEquals(slots(contract, 0, false, false), unpack(compiled, briefed),
				() -> "quest " + contract.questId() + " end-dialog must clear var5 only");
			assertTrue(briefed.afterCommit().contains(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				() -> "quest " + contract.questId() + " end-dialog must refresh journal visibility");
			assertTrue(briefed.afterCommit().contains(new AfterCommitAction.CloseDialog()),
				() -> "quest " + contract.questId() + " end-dialog must close the dialog");

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
			assertEquals(Set.of(contract.acceptNpc()), talkNpcIds(definition, "unaccepted", "started"),
				() -> "quest " + contract.questId() + " must accept from the journal NPC");
			assertEquals(Set.of(contract.briefNpc()), talkNpcIds(definition, "started", "briefed"),
				() -> "quest " + contract.questId() + " must take the briefing from the journal row-0 NPC");
			assertEquals(Set.of(contract.reportNpc()), talkNpcIds(definition, reportSource(contract), "reward"),
				() -> "quest " + contract.questId() + " must report on the journal report-row NPC");
			assertEquals(Set.of(contract.reportNpc()), talkNpcIds(definition, "reward", "complete"),
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
					"started".equals(route.sourceNode()) && "reward".equals(route.targetNode())),
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
				/* 24112：接取后没听简报就先杀怪的旧存档（var0=1、var5=1）归一化到 killed。 */
				/* 24112: legacy saves that killed before the briefing normalize onto killed. */
				List<QuestMutationPlan> skipped = plans(compiled, QuestStatus.START,
					Map.of("var0", 1, "var5", 1), new QuestEvent.EnterWorld());
				assertEquals(1, skipped.size(),
					() -> "quest " + contract.questId() + " must normalize the kill-before-briefing save");
				assertEquals(slots(contract, 1, false, false), unpack(compiled, skipped.getFirst()),
					() -> "quest " + contract.questId() + " kill-before-briefing save must land on killed");
				/* 旧定义 reward 投影 var0=0 的领奖态存档补齐到饱和值 1。 */
				/* The old REWARD projection var0=0 heals to the saturated 1. */
				List<QuestMutationPlan> heals = plans(compiled, QuestStatus.REWARD, Map.of("var0", 0),
					new QuestEvent.EnterWorld());
				assertEquals(1, heals.size(), () -> "quest " + contract.questId() + " must heal the old reward save");
				assertEquals(slots(contract, 1, false, false), unpack(compiled, heals.getFirst()),
					() -> "quest " + contract.questId() + " old reward save must heal to (1,0)");
				assertEquals(QuestStatus.REWARD, heals.getFirst().nextStatus(),
					() -> "quest " + contract.questId() + " must stay in REWARD while healing");
				continue;
			}
			/* 30600/30610：旧步骤号 var0=2（打完舰长）映射到 k2，旧 REWARD 投影 var0=2 补齐到 (1,1)。 */
			/* 30600/30610: the legacy step value 2 maps onto k2 and its reward save heals to (1,1). */
			List<QuestMutationPlan> stepSaves = plans(compiled, QuestStatus.START, Map.of("var0", 2),
				new QuestEvent.EnterWorld());
			assertEquals(1, stepSaves.size(), () -> "quest " + contract.questId() + " must migrate the step-2 save");
			assertEquals(slots(contract, 1, true, false), unpack(compiled, stepSaves.getFirst()),
				() -> "quest " + contract.questId() + " step-2 save must land on k2");
			List<QuestMutationPlan> heals = plans(compiled, QuestStatus.REWARD, Map.of("var0", 2),
				new QuestEvent.EnterWorld());
			assertEquals(1, heals.size(), () -> "quest " + contract.questId() + " must heal the old reward save");
			assertEquals(slots(contract, 1, true, false), unpack(compiled, heals.getFirst()),
				() -> "quest " + contract.questId() + " old reward save must heal to (1,1)");
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

	/** 双层族从 k2 报告，单层族从 killed 报告。 / The two-slot family reports from k2, the other from killed. */
	private static String reportSource(Contract contract) {
		return contract.bossKill() == null ? "killed" : "k2";
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
		try (InputStream input = CounterChainBriefingStageContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
