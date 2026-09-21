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
 *
 * <p>旧定义在 s8 沿用旧 handler 的“var1 != 0 时只累加”分支：var1 上限为 7，而 s7→s8 不清零 var1
 * （s6 计数残留 6/7），因此第一次击杀即得到 7、第二次击杀计算到 8，被 {@code ProgressLayout.pack}
 * 判为越界并整笔失败，玩家永久卡在“消灭潜入部队军团长 (0/1)”这一步。现在 s8 击杀只依据 var0 推进，
 * 并在同一条 mutation 中把 var1 清零；旧存档残留 0..15（新布局可见 4-bit 残值）均可一次击杀推进。</p>
 * <p>The old definition kept the legacy "accumulate while var1 != 0" branch at s8 while var1 is capped at 7
 * and the s7-to-s8 transition left the s6 counter (6/7) in place, so the first boss kill wrote 7 and the second
 * computed 8, which {@code ProgressLayout.pack} rejected, stranding the player on the boss step. The s8 kill
 * now advances solely from var0 and clears var1 in the same mutation, so every visible legacy 4-bit residue
 * 0..15 advances in one kill.</p>
 */
class Quest10529BossKillCounterContractTest {
	private static final int BOSS_STAGE = 8;
	private static final int NEXT_STAGE = 9;
	private static final int COUNTER_MAX = 7;
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(10529, 703317, 244113),
		new QuestContract(20529, 703325, 244129));

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
		assertTrue(enterBossStep.actions().contains(new QuestAction.SetVariable("var1", 0)),
			() -> "quest " + contract.questId() + " 进入 boss 步必须清零 s6 击杀计数残留");

		QuestEvent bossKill = new QuestEvent.KillNpc(contract.bossNpcId());
		QuestTransition advance = transition(definition, "s8", "s9", bossKill);
		assertTrue(advance.conditions().contains(new QuestCondition.QuestVariableIs("var0", BOSS_STAGE)));
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", NEXT_STAGE),
			new QuestAction.SetVariable("var1", 0)), advance.actions(),
			() -> "quest " + contract.questId() + " 击杀 boss 必须一次推进并清零计数残留");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			advance.afterCommit());
		assertEquals(0, definition.transitions().stream()
			.filter(candidate -> "s8".equals(candidate.sourceNode()) && "s8".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(bossKill))
			.count(), () -> "quest " + contract.questId() + " 不应再有 boss 击杀累加自环");

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

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				Quest10529BossKillCounterContractTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}

	/** 单任务的镜像契约。 One quest's mirrored contract. */
	private record QuestContract(int questId, int objectNpcId, int bossNpcId) {
	}
}
