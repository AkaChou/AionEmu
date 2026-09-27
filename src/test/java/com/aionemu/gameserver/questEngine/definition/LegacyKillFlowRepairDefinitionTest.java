package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LegacyKillFlowRepairDefinitionTest {
	@Test
	void killMilestonesRemainStartedUntilTheLegacyHandoff() {
		for (int questId : List.of(2620, 4210, 18302, 18303, 18510, 23702, 23703, 23705)) {
			CompiledQuestDefinition definition = load(questId);
			assertFalse(definition.definition().transitions().stream()
				.anyMatch(transition -> isKill(transition.event()) && transition.targetNode().equals("reward")),
				() -> questId + " must not become reward-ready directly from its ordinary kill milestone");
		}

		QuestTransition phagrasulReport = talk(load(2620), "s1", 204787, 1009, "reward");
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var1", 5)));
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var2", 5)));

		assertEquals("reward", talk(load(18302), "started", 730375, 10255, "reward").targetNode());
		assertEquals("reward", talk(load(18303), "started", 700980, -1, "reward").targetNode());
		/* 23702 自 P0c-6 起走真端网格合成器：报告行是计数满段节点（真端表 count1=4 → 单槽网格名 a4），
		 * 不是旧 XML 里那个兼作"接取态"的 started 节点。P0-2 规范形后交付边是满段节点上的
		 * QUEST_SELECT（1009 中转删除）。
		 * 23702 is grid-composed since P0c-6: the report row is the saturated single-slot grid node a4
		 * (retail count1 = 4), not the legacy start state. Canonical since P0-2: the delivery edge is
		 * the full node's QUEST_SELECT (the 1009 hop is gone). */
		CompiledQuestDefinition baltasar = load(23702);
		String reportRow = rewardReady(baltasar.definition(), 802354, QuestDialogAction.QUEST_SELECT.id());
		assertEquals("a4", reportRow, "quest 23702 must report from the saturated counter node");
		assertEquals("reward", talk(baltasar, reportRow, 802354, QuestDialogAction.QUEST_SELECT.id(), "reward")
			.targetNode());
		/* P0c-8c（2026-09-24）：23703/23705 已由真端网格驱动——报告路由挂在**计数满段节点**上，不再是旧 XML 的
		 * `started`；旧 XML 的 `var0 == 3` 行门控由满段节点的打包值承担，故按"家族命名无关"的 rewardReady
		 * 定位报告行，只继续锁定"报告路由存在且不被第二个计数器（var1）门控"这一条历史修复。
		 * P0-2 规范形后报告动作从 1009 换成满段 QUEST_SELECT。
		 * Since P0c-8c both quests are grid-composed: the report route hangs off the saturated counter node
		 * (not the legacy start state); the old row gate lives in the packed node value now. Canonical
		 * since P0-2: the report action switched from 1009 to the full node's QUEST_SELECT. */
		CompiledQuestDefinition quest23703 = load(23703);
		String reportRow23703 = rewardReady(quest23703.definition(), 802353, QuestDialogAction.QUEST_SELECT.id());
		assertEquals("reward", talk(quest23703, reportRow23703, 802353, QuestDialogAction.QUEST_SELECT.id(),
			"reward").targetNode());

		CompiledQuestDefinition quest23705 = load(23705);
		String reportRow23705 = rewardReady(quest23705.definition(), 802345, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition oldRoadGate = talk(quest23705, reportRow23705, 802345, QuestDialogAction.QUEST_SELECT.id(),
			"reward");
		assertEquals(List.of(), oldRoadGate.conditions(), "网格满段节点自身即报告门控，路由不应再带条件");
		assertFalse(oldRoadGate.conditions().stream().anyMatch(condition ->
			condition instanceof QuestCondition.QuestVariableIs variable && variable.field().equals("var1")));
	}

	@Test
	void independentLegacyKillBitsRemainIndependent() {
		CompiledQuestDefinition missingHaorunerk = load(4210);
		assertEquals(Set.of("var1", "var2"), fieldNames(missingHaorunerk, "var1", "var2"));
		assertSetsBit(kill(missingHaorunerk, "s1", 215056), "var1");
		assertSetsBit(kill(missingHaorunerk, "s1", 215080), "var2");

		CompiledQuestDefinition fate = load(4502);
		assertEquals(Set.of("var1", "var2", "var3"), fieldNames(fate, "var1", "var2", "var3"));
		assertSetsBit(kill(fate, "s2", 214895), "var1");
		assertSetsBit(kill(fate, "s2", 214896), "var2");
		assertSetsBit(kill(fate, "s2", 214897), "var3");
		QuestTransition itemReport = talk(fate, "s2", 204837, 39, "reward");
		assertTrue(itemReport.conditions().contains(new QuestCondition.HasItem(182204534, 1)));
		assertTrue(itemReport.actions().contains(new QuestAction.RemoveItem(182204534, 1)));

		CompiledQuestDefinition destroyingWeapons = load(2633);
		assertEquals("s2", talk(destroyingWeapons, "s1", 700296, -1, "s2").targetNode());
		assertEquals("reward", kill(destroyingWeapons, "s2", 213933).targetNode());
	}

	@Test
	void missionKillChainsPreserveTheirDialogZoneAndItemStages() {
		CompiledQuestDefinition totem = load(24015);
		assertEquals("s2", totem.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "s1")
				&& transition.event().equals(new QuestEvent.EnterZone("BLACK_CLAW_OUTPOST_220030000")))
			.findFirst().orElseThrow().targetNode());
		assertEquals(List.of("s3", "s4", "reward"), totem.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(700099)))
			.map(QuestTransition::targetNode).toList());
		assertTrue(totem.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete"))
			.allMatch(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId() == 203557));

		CompiledQuestDefinition frozenCity = load(24052);
		List<QuestTransition> itemUses = frozenCity.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.UseItem).toList();
		assertEquals(9, itemUses.size());
		assertTrue(itemUses.stream().allMatch(transition ->
			transition.conditions().contains(new QuestCondition.ZoneIs("DF3_ITEMUSEAREA_Q2056"))
				&& transition.actions().contains(new QuestAction.BlockDefaultItemUse())));
		assertEquals(3, itemUses.stream().filter(transition -> transition.targetNode().equals("s4")
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.SpawnNpc.class::isInstance)
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.StartQuestTimer.class::isInstance)).count());
		assertEquals("reward", kill(frozenCity, "s4", 233864).targetNode());

		CompiledQuestDefinition crisis = load(24054);
		assertEquals("s5", kill(crisis, "s2", 702041).targetNode());
		assertEquals("s6", kill(crisis, "s5", 233865).targetNode());
		assertEquals("reward", talk(crisis, "s6", 204701, 10255, "reward").targetNode());

		CompiledQuestDefinition umkata = load(24114);
		QuestTransition spirit = umkata.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
				&& npcIds.equals(Set.of(210722, 210588))).findFirst().orElseThrow();
		assertEquals("started", spirit.targetNode());
		QuestTransition boss = kill(umkata, "started", 210752);
		assertEquals("reward", boss.targetNode());
		assertEquals(Set.of(182215474, 182215475, 182215476), boss.actions().stream()
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(QuestAction.RemoveItem.class::cast).map(QuestAction.RemoveItem::itemId)
			.collect(Collectors.toSet()));
	}

	@Test
	void daevanionKillStagesUseClientCountsAndResetTheSharedPackedCounter() {
		CompiledQuestDefinition pants = load(15304);
		if (pants.definition().progressLayout().fields().size() > 2) {
			// 真端驱动三族混合链（DD_TALK_COLLECT_HUNT_CHAIN）：var0 talk 标志、var1 采集标志、
			// var2 = 60 杀计数链、var3 回报标志；SET_SUCCEED 满态后 1009 进领奖。
			// The retail-driven three-kind mixed chain (DD_TALK_COLLECT_HUNT_CHAIN).
			assertDaevanionSequentialChain(pants);
		} else {
			assertDaevanionLegacySharedCounter(pants);
		}

		CompiledQuestDefinition weapon = load(15306);
		assertEquals(Set.of("var0", "var1"), weapon.definition().progressLayout().fields().stream()
			.map(BitField::name).collect(Collectors.toSet()));
		// 15306 已由真端驱动（客户端进度行名单回退）：五个 hunt 段共享 var1 计数（60/60/30/1/5）、
		// 段完成清零并推进行阶梯（行 3..7 = 客户端 S0==3..7），领奖行 = 客户端末行 10。
		// 15306 is now retail-driven (via the client progress-row name fallback): five hunt stages
		// share the var1 counter (60/60/30/1/5), each completion resets it and advances the ladder
		// (rows 3..7 = client S0==3..7), and the reward row is the client last row 10.
		int[] stageCounts = {60, 60, 30, 1, 5};
		QuestSnapshot state = snapshot(weapon, 3, 0, Map.of());
		for (int stage = 0; stage < stageCounts.length; stage++) {
			final int row = 3 + stage;
			final int count = stageCounts[stage];
			int huntNpc = weapon.definition().transitions().stream()
				.filter(transition -> ("s" + row).equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.KillNpc)
				.map(transition -> ((QuestEvent.KillNpc) transition.event()).npcId())
				.findFirst().orElseThrow(() -> new AssertionError("missing kill edge from s" + row));
			state = applyKills(weapon, state, huntNpc, count - 1);
			assertProjection(weapon, state, QuestStatus.START, row, count - 1);
			state = applyKills(weapon, state, huntNpc, 1);
			assertProjection(weapon, state, QuestStatus.START, row + 1, 0);
		}
		QuestTransition midAdvance = weapon.definition().transitions().stream()
			.filter(transition -> "s8".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc && !transition.actions().isEmpty())
			.findFirst().orElseThrow(() -> new AssertionError("missing s8 talk advance"));
		state = apply(weapon, state, midAdvance.event());
		assertProjection(weapon, state, QuestStatus.START, 9, 0);
		QuestTransition report = weapon.definition().transitions().stream()
			.filter(transition -> "s9".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc)
			.findFirst().orElseThrow(() -> new AssertionError("missing s9 report edge"));
		state = apply(weapon, state, report.event());
		assertProjection(weapon, state, QuestStatus.REWARD, 10, 0);
	}

	/** 15304 链形合同：SETPRO1 → 39 检查 → 60 杀计数链 → SET_SUCCEED → 1009 领奖。 */
	/** The 15304 chain contract: SETPRO1, the 39 check, the 60-kill counter chain, SET_SUCCEED,
	 * then the 1009 report. */
	private static void assertDaevanionSequentialChain(CompiledQuestDefinition pants) {
		ProgressLayout layout = pants.definition().progressLayout();
		QuestSnapshot state = new QuestSnapshot(7, pants.id(), QuestStatus.START, 0, Map.of(182215835, 1));
		state = apply(pants, state, new QuestEvent.TalkToNpc(805328, 10000));
		assertEquals(1, layout.unpack(state.packedVariables()).get("var0"), "SETPRO1 sets the talk flag");
		state = apply(pants, state, new QuestEvent.TalkToNpc(805328, 39));
		assertEquals(1, layout.unpack(state.packedVariables()).get("var1"), "the group check sets the collect flag");
		// 击杀目标从定义发现（真端表 8 个 advance 模板的显示名族闭包）。
		// The kill targets come from the definition (the display-name closure of the eight retail
		// templates).
		int huntNpc = pants.definition().transitions().stream()
			.filter(transition -> "s3".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.KillNpc)
			.map(transition -> ((QuestEvent.KillNpc) transition.event()).npcId())
			.findFirst().orElseThrow(() -> new AssertionError("missing kill edge from the hunt row"));
		for (int count = 1; count <= 60; count++) {
			final int killIndex = count;
			state = apply(pants, state, new QuestEvent.KillNpc(huntNpc));
			assertEquals(killIndex, layout.unpack(state.packedVariables()).get("var2"),
				() -> "kill " + killIndex + " must advance the counter chain");
		}
		state = apply(pants, state, new QuestEvent.TalkToNpc(805328, 10255));
		assertEquals(1, layout.unpack(state.packedVariables()).get("var3"), "SET_SUCCEED sets the report flag");
		QuestEvent report = pants.definition().transitions().stream()
			.filter(transition -> "s64".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == 1009)
			.map(transition -> transition.event())
			.findFirst().orElseThrow(() -> new AssertionError("missing full-state report edge s64->reward"));
		state = apply(pants, state, report);
		assertEquals(QuestStatus.REWARD, state.status(), "the 1009 report enters reward");
	}

	/** 15304 旧形合同（单 var0 阶梯 + 共享 var1 计数、完成即清零）。 */
	/** The 15304 legacy contract: a single var0 ladder with the shared var1 counter reset on
	 * completion. */
	private static void assertDaevanionLegacySharedCounter(CompiledQuestDefinition pants) {
		// 击杀目标从定义发现（真端表显示名族闭包；旧 XML 单个目标不必在族内）。
		// The kill target comes from the definition (the retail display-name family closure; the
		// legacy XML's single target need not be inside it).
		int huntNpc = pants.definition().transitions().stream()
			.filter(transition -> "s2".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.KillNpc)
			.map(transition -> ((QuestEvent.KillNpc) transition.event()).npcId())
			.findFirst().orElseThrow(() -> new AssertionError("missing kill edge from the hunt row"));
		QuestSnapshot pantsState = snapshot(pants, 2, 0, Map.of(182215835, 1));
		pantsState = applyKills(pants, pantsState, huntNpc, 59);
		assertProjection(pants, pantsState, QuestStatus.START, 2, 59);
		pantsState = apply(pants, pantsState, new QuestEvent.KillNpc(huntNpc));
		assertProjection(pants, pantsState, QuestStatus.START, 3, 0);
		pantsState = apply(pants, pantsState, new QuestEvent.TalkToNpc(805328, 10255));
		assertProjection(pants, pantsState, QuestStatus.REWARD, 4, 0);
	}

	private static Set<String> fieldNames(CompiledQuestDefinition definition, String... names) {
		return java.util.Arrays.stream(names)
			.filter(name -> definition.definition().progressLayout().field(name) != null)
			.collect(Collectors.toSet());
	}

	private static void assertSetsBit(QuestTransition transition, String field) {
		assertTrue(transition.actions().contains(new QuestAction.SetVariable(field, 1)));
	}

	private static boolean isKill(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	/**
	 * "可报告态"节点：把指定 NPC/对话的交付路线带进 reward 的那个来源节点（家族命名无关）。
	 * The report-ready node: the source of the report route into reward, whatever the family names it.
	 */
	private static String rewardReady(QuestDefinition definition, int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.targetNode()))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)))
			.map(QuestTransition::sourceNode)
			.filter(Objects::nonNull)
			.distinct()
			.findFirst()
			.orElseThrow(() -> new AssertionError("quest " + definition.id() + " has no report route into reward"));
	}

	private static QuestTransition kill(CompiledQuestDefinition definition, String source, int npcId) {
		return transition(definition, source, transition -> QuestEvent.matches(transition.event(), new QuestEvent.KillNpc(npcId)));
	}

	private static QuestTransition talk(CompiledQuestDefinition definition, String source, int npcId,
			int dialogId, String target) {
		return transition(definition, source, transition -> transition.targetNode().equals(target)
			&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)));
	}

	private static QuestTransition transition(CompiledQuestDefinition definition, String source,
			Predicate<QuestTransition> predicate) {
		return definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), source) && predicate.test(transition))
			.findFirst().orElseThrow();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, int var0, int var1,
			Map<Integer, Integer> inventory) {
		int packed = definition.definition().progressLayout().pack(Map.of("var0", var0, "var1", var1));
		return new QuestSnapshot(7, definition.id(), QuestStatus.START, packed, inventory);
	}

	private static QuestSnapshot applyKills(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			int npcId, int count) {
		QuestSnapshot current = snapshot;
		for (int i = 0; i < count; i++) {
			current = apply(definition, current, new QuestEvent.KillNpc(npcId));
		}
		return current;
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestTransition> candidates = definition.transitionsFor(event.type()).stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.sorted(Comparator.comparingInt(transition -> transition.priority() == null
				? Integer.MAX_VALUE : transition.priority()))
			.toList();
		for (QuestTransition transition : candidates) {
			var plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				QuestMutationPlan mutation = plan.orElseThrow();
				return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), mutation.nextStatus(),
					mutation.nextPackedVariables(), snapshot.inventory());
			}
		}
		return fail("no eligible transition for quest " + definition.id() + " event " + event
			+ " at " + definition.definition().progressLayout().unpack(snapshot.packedVariables()));
	}

	private static void assertProjection(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestStatus status, int var0, int var1) {
		Map<String, Integer> variables = definition.definition().progressLayout().unpack(snapshot.packedVariables());
		assertEquals(status, snapshot.status());
		assertEquals(var0, variables.get("var0"));
		assertEquals(var1, variables.get("var1"));
	}

	private static CompiledQuestDefinition load(int questId)  {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
