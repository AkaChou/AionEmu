package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 10529/20529 破坏圣物 2 的 boss 击杀在客户端 /1 步骤内一次推进，并清零 s6 计数残留。
 * Verifies that the Protection Artifact 2 boss kill of quests 10529/20529 advances in the client /1 step
 * and clears the s6 counter residue.
 * <p>旧定义在 s8 沿用旧 handler 的“var1 != 0 时只累加”分支：var1 上限为 7，而 s7→s8 不清零 var1
 * （s6 计数残留 6/7），因此第一次击杀即得到 7、第二次击杀计算到 8，被 {@code ProgressLayout.pack}
 * 判为越界并整笔失败，玩家永久卡在“消灭潜入部队军团长 (0/1)”这一步。现在 s8 击杀只依据 var0 推进，
 * 并在同一条 mutation 中把 var1 清零；旧存档残留 0..15（新布局可见 4-bit 残值）均可一次击杀推进。</p>
 * <p>The old definition kept the legacy "accumulate while var1 != 0" branch at s8 while var1 is capped at 7
 * and the s7-to-s8 transition left the s6 counter (6/7) in place, so the first boss kill wrote 7 and the second
 * computed 8, which {@code ProgressLayout.pack} rejected, stranding the player on the boss step. The s8 kill
 * now advances solely from var0 and clears var1 in the same mutation, so every visible legacy 4-bit residue
 * 0..15 advances in one kill.</p>
 * <p>2026-10-09 增补：s6→s7、s7→s8、s8→s9 只清 var1 不清 var2，s6 的 var2=3 一路残留到 s10/REWARD
 * （packed 12297+），客户端整型步数与本地步骤表脱节，报告代理人（806075/806079）头顶不显示任务标记
 * （QE-044 同根因：对白/标记按整型步数精确匹配，与按 var0 位域投影的任务书行高亮互不影响，所以对话
 * 能走通而标记缺失）。现在离开计数阶段的所有步进边（s6→s7、s7→s8、s8→s9、s9→s10、SET_SUCCEED）
 * 全清 var1/var2，并补无 source 的 enter-world 自愈边修复修复前落盘的旧档；自愈边必须排在副本失败
 * 回退边之后——世界外 s4..s9 场景由回退边优先接管。</p>
 * <p>Added 2026-10-09: s6→s7, s7→s8 and s8→s9 cleared only var1, so the s6 var2=3 residue rode all the
 * way into s10/REWARD (packed 12297+), desyncing the client's integral step from its local step table and
 * hiding the reporting agent's overhead quest marker (806075/806079; the QE-044 root cause — dialogs and
 * markers match the integral step exactly while journal row highlighting reads the var0 bit field, which is
 * why dialogs worked while the marker was missing). Every advancing edge that leaves a counting stage
 * (s6→s7, s7→s8, s8→s9, s9→s10, SET_SUCCEED) now clears var1/var2, plus source-less enter-world self-heal
 * edges repair legacy saves persisted before the fix; the heal edges must stay after the instance-failure
 * fallback edges, which take priority for out-of-instance s4..s9.</p>
 */
class Quest10529BossKillCounterContractTest {
	private static final int BOSS_STAGE = 8;
	private static final int NEXT_STAGE = 9;
	private static final int COUNTER_MAX = 7;
	private static final int INSTANCE_WORLD_ID = 301690000;
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(10529, 703317, 244113, 806294, 806075, 244111, 244112),
		new QuestContract(20529, 703325, 244129, 806299, 806079, 244127, 244128));

	@TestFactory
	Stream<DynamicTest> bossKillCounterStaysInRangeAndKeepsAdvancing() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		BitField counter = layout.field("var1");
		assertEquals(0, layout.field("var0").offset(), "阶段必须占用客户端任务说明行索引读取的 SECTION_0");
		assertEquals(6, counter.offset(), "击杀计数必须留在 SECTION_1（offset 6）");
		assertEquals(COUNTER_MAX, counter.maxValue(), "客户端 quest_script_monster.csv 声明 SECTION_1<7");

		QuestTransition enterBossStep = transition(definition, "s7", "s8",
			new QuestEvent.TalkToNpc(contract.objectNpcId(), QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", BOSS_STAGE),
			new QuestAction.SetVariable("var1", 0),
			new QuestAction.SetVariable("var2", 0)), enterBossStep.actions(),
			() -> "quest " + contract.questId() + " 进入 boss 步必须清零 s6 全部击杀计数残留（var1/var2）");

		// s6 → s7 的两条狩猎尾杀边同样必须全清计数，否则残留从 s7 就开始污染整型步数。
		// Both s6-to-s7 final-hunt kill edges must clear every counter too, or the residue
		// pollutes the integral step from s7 onward.
		for (int huntNpcId : new int[] {contract.huntNpcA(), contract.huntNpcB()}) {
			QuestTransition huntTailKill = transition(definition, "s6", "s7", new QuestEvent.KillNpc(huntNpcId));
			assertTrue(huntTailKill.actions().contains(new QuestAction.SetVariable("var0", 7)),
				() -> "quest " + contract.questId() + " s6→s7 必须推进到阶段 7");
			assertTrue(huntTailKill.actions().contains(new QuestAction.SetVariable("var1", 0))
					&& huntTailKill.actions().contains(new QuestAction.SetVariable("var2", 0)),
				() -> "quest " + contract.questId() + " s6→s7（kill " + huntNpcId
					+ "）必须清零 var1/var2 计数残留");
		}

		QuestEvent bossKill = new QuestEvent.KillNpc(contract.bossNpcId());
		QuestTransition advance = transition(definition, "s8", "s9", bossKill);
		assertTrue(advance.conditions().contains(new QuestCondition.QuestVariableIs("var0", BOSS_STAGE)));
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", NEXT_STAGE),
			new QuestAction.SetVariable("var1", 0),
			new QuestAction.SetVariable("var2", 0)), advance.actions(),
			() -> "quest " + contract.questId() + " 击杀 boss 必须一次推进并清零全部计数残留");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			advance.afterCommit());
		assertEquals(0, definition.transitions().stream()
			.filter(candidate -> "s8".equals(candidate.sourceNode()) && "s8".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(bossKill))
			.count(), () -> "quest " + contract.questId() + " 不应再有 boss 击杀累加自环");

		// 报告行与领奖行的 packed 步数必须纯净（10/11），否则代理人头顶标记缺失。
		// The report and reward rows' packed steps must be clean (10/11), or the agent's overhead marker vanishes.
		QuestTransition reportHandIn = transition(definition, "s9", "s10",
			new QuestEvent.TalkToNpc(contract.sageNpcId(), QuestDialogAction.SETPRO10.id()));
		assertTrue(reportHandIn.actions().contains(new QuestAction.SetVariable("var1", 0))
				&& reportHandIn.actions().contains(new QuestAction.SetVariable("var2", 0)),
			() -> "quest " + contract.questId() + " 报告交接（SETPRO10）必须清零 var1/var2");
		QuestTransition rewardHandIn = transition(definition, "s10", "reward",
			new QuestEvent.TalkToNpc(contract.agentNpcId(), QuestDialogAction.SET_SUCCEED.id()));
		assertTrue(rewardHandIn.actions().contains(new QuestAction.SetVariable("var1", 0))
				&& rewardHandIn.actions().contains(new QuestAction.SetVariable("var2", 0)),
			() -> "quest " + contract.questId() + " 领奖交接（SET_SUCCEED）必须清零 var1/var2");

		// 旧档自愈：无 source 的 enter-world 边只清计数、不动阶段；覆盖 s7..reward 五个阶段。
		// Legacy-save self-heal: source-less enter-world edges clear counters only; s7..reward covered.
		assertSelfHealEdge(definition, contract.questId(), "s7", QuestStatus.START, 7);
		assertSelfHealEdge(definition, contract.questId(), "s8", QuestStatus.START, 8);
		assertSelfHealEdge(definition, contract.questId(), "s9", QuestStatus.START, 9);
		assertSelfHealEdge(definition, contract.questId(), "s10", QuestStatus.START, 10);
		assertSelfHealEdge(definition, contract.questId(), "reward", QuestStatus.REWARD, 11);

		QuestSnapshot fresh = dispatch(compiled, snapshot(contract.questId(),
			layout.pack(Map.of("var0", BOSS_STAGE, "var1", 0))), bossKill);
		assertEquals(Map.of("var0", NEXT_STAGE, "var1", 0, "var2", 0), layout.unpack(fresh.packedVariables()),
			() -> "quest " + contract.questId() + " 无残留计数时应一次击杀推进");

		int stageShift = layout.field("var0").offset();
		int counterShift = counter.offset();
		for (int legacy = 0; legacy <= 15; legacy++) {
			int value = legacy;
			// 旧 handler 的 6-bit 计数在新布局按 4-bit 解包，0..15 覆盖全部可见残值。
			// The legacy 6-bit counter is read through the new 4-bit field, so 0..15 covers every visible residue.
			QuestSnapshot state = snapshot(contract.questId(),
				(BOSS_STAGE << stageShift) | (value << counterShift));
			QuestSnapshot next = dispatch(compiled, state, bossKill);
			Map<String, Integer> produced = layout.unpack(next.packedVariables());
			assertEquals(Map.of("var0", NEXT_STAGE, "var1", 0, "var2", 0), produced,
				() -> "quest " + contract.questId() + " 的旧存档 var1=" + value
					+ " 必须一次击杀推进并清零");
		}

		// 实机报障档（packed 12297 = var0=9, var1=0, var2=3）：副本内重登自愈为纯 9；
		// boss 击杀带 var2=3 残留时同样必须产出纯 9。
		// The field-reported save (packed 12297 = var0=9, var1=0, var2=3): relogging inside the
		// instance heals to a clean 9; a boss kill carrying the var2=3 residue must also produce a clean 9.
		int residueShift = layout.field("var2").offset();
		QuestSnapshot dirtyInstance = snapshotIn(contract.questId(), QuestStatus.START,
			(NEXT_STAGE << stageShift) | (3 << residueShift), INSTANCE_WORLD_ID);
		Map<String, Integer> healed = layout.unpack(
			dispatch(compiled, dirtyInstance, new QuestEvent.EnterWorld()).packedVariables());
		assertEquals(Map.of("var0", NEXT_STAGE, "var1", 0, "var2", 0), healed,
			() -> "quest " + contract.questId() + " 副本内旧档（var2=3 残留）重登必须自愈为纯 "
				+ NEXT_STAGE + " 阶段");

		QuestSnapshot dirtyBossStage = snapshot(contract.questId(),
			(BOSS_STAGE << stageShift) | (3 << residueShift));
		Map<String, Integer> advancedClean = layout.unpack(
			dispatch(compiled, dirtyBossStage, bossKill).packedVariables());
		assertEquals(Map.of("var0", NEXT_STAGE, "var1", 0, "var2", 0), advancedClean,
			() -> "quest " + contract.questId() + " boss 击杀必须吞掉 var2=3 残留并产出纯 "
				+ NEXT_STAGE + " 阶段");
	}

	/** 断言某阶段存在无 source 的 enter-world 计数自愈边（target=reward 是回滚边：REWARD/11 -> 10）。 Asserts the stage's source-less enter-world heal edge (the reward target is a rollback edge: REWARD/11 -> 10). */
	private static void assertSelfHealEdge(QuestDefinition definition, int questId, String target,
			QuestStatus status, int stage) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld)
			.filter(candidate -> candidate.conditions().contains(new QuestCondition.StatusIs(status))
				&& candidate.conditions().contains(new QuestCondition.QuestVariableIs("var0", stage)))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + questId + " 必须存在 target=" + target + " 的 enter-world 自愈边");
		QuestTransition heal = matches.getFirst();
		assertTrue(heal.conditions().contains(new QuestCondition.StatusIs(status))
				&& heal.conditions().contains(new QuestCondition.QuestVariableIs("var0", stage)),
			() -> "quest " + questId + " target=" + target + " 自愈边必须限定 " + status + "/var0=" + stage
				+ "，不得误伤其它阶段");
		assertTrue(heal.actions().contains(new QuestAction.SetVariable("var1", 0))
				&& heal.actions().contains(new QuestAction.SetVariable("var2", 0)),
			() -> "quest " + questId + " target=" + target + " 自愈边必须清零 var1/var2");
		if ("reward".equals(target)) {
			// QE-051 反向带毒边反转：REWARD/11 档回滚到 legacy 落盘值 10（行 11 = 行 0 复述行例外）。
			// The QE-051 reverse-poison edge inverted: REWARD/11 rolls back to the legacy 10
			// (journal row 11 repeats row 0 and is never lit).
			assertTrue(heal.actions().contains(new QuestAction.SetVariable("var0", 10)),
				() -> "quest " + questId + " target=reward 自愈边必须把 REWARD/11 回滚到 legacy 值 10");
		}
		else {
			assertTrue(heal.actions().stream()
					.noneMatch(action -> action instanceof QuestAction.SetVariable set && set.field().equals("var0")),
				() -> "quest " + questId + " target=" + target + " 自愈边只清计数，不得改写 var0");
		}
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	/** 复刻 dispatcher 的优先级选择：条件不匹配即无操作。 Mirrors the dispatcher priority choice; a mismatch is a no-op. */
	private static QuestSnapshot dispatch(CompiledQuestDefinition definition, QuestSnapshot snapshot,
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
				return snapshot(definition.id(), mutation.nextStatus(), mutation.nextPackedVariables());
			}
		}
		return snapshot;
	}

	private static QuestSnapshot snapshot(int questId, int packedVariables) {
		return snapshot(questId, QuestStatus.START, packedVariables);
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, int packedVariables) {
		return new QuestSnapshot(7, questId, status, packedVariables, Map.of());
	}

	/** 副本内快照：worldId 生效，副本失败回退边不匹配。 In-instance snapshot; the fallback edges do not match. */
	private static QuestSnapshot snapshotIn(int questId, QuestStatus status, int packedVariables, int worldId) {
		return new QuestSnapshot(7, questId, status, packedVariables, Map.of(), Map.of(),
			true, true, 0, 0, worldId, 0, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest/definitions/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				Quest10529BossKillCounterContractTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}

	/** 单任务的镜像契约。 One quest's mirrored contract. */
	private record QuestContract(int questId, int objectNpcId, int bossNpcId, int sageNpcId, int agentNpcId,
			int huntNpcA, int huntNpcB) {
	}
}
