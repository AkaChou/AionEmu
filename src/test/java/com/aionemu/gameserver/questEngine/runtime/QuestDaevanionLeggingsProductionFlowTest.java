package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证守护者套装护腿任务的 60 次实时击杀计数与交付合同（真端驱动混合链，客户端 SECTION 对齐形）：
 * var0 = 行阶梯（SECTION_0），var1 = 当前段击杀计数（SECTION_1，偏移 6、段完成清零）。
 * Verifies the 60-kill live counter and turn-in contract for both Daevanion leggings quests
 * (the retail-driven mixed chain, client SECTION-aligned shape): var0 is the row ladder
 * (SECTION_0) and var1 the current stage's kill counter (SECTION_1, offset 6, reset on stage
 * completion).
 */
class QuestDaevanionLeggingsProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(15314, Set.of(233945, 233946, 233947, 233948, 233949, 233950, 233951, 233952),
			805328, 182215835, 182215868),
		new QuestContract(25314, Set.of(233902, 233903, 233904, 233905, 233906, 233907, 233908),
			805340, 182215850, 182215880));

	private static final int REQUIRED_KILLS = 60;

	@TestFactory
	Stream<DynamicTest> preservesTheRetailSixtyKillAndTurnInFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = ProductionQuestDefinitions.definition(contract.questId());
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();
		// 客户端 SECTION 对齐形：var0 占 SECTION_0、var1 占 SECTION_1（偏移 6）。
		// Client SECTION-aligned shape: var0 owns SECTION_0 and var1 owns SECTION_1 (offset 6).
		assertEquals(0, layout.field("var0").offset());
		assertEquals(6, layout.field("var1").offset());
		// 链形：started（全零）→ s1 信件行 → s2 采集行（hunt 开段行）→ s3 交付行（计数清零）→ reward。
		// Chain: started (zeros), s1 the letter row, s2 the collect row (the hunt's own row), s3 the
		// handover row (counter reset), reward.
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));

		// talk 步：交付 NPC 的信件推进边 started->s1 只推行阶梯（动作 id 从信件梯登记读）。
		// Talk step: the turn-in npc's letter advance edge started->s1 bumps the row ladder only.
		QuestTransition talkAdvanceEdge = definition.transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode()) && "s1".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == contract.turnInNpcId()
				&& !candidate.actions().isEmpty())
			.findFirst().orElseThrow(() -> new AssertionError("missing turn-in advance edge started->s1"));

		// 采集段：39 检查整组过/扣 s1 -> s2（只锁必需交付物），缺货兜底留在 s1。
		// Collect stage: the group check consumes the required item from s1 into s2; missing goods
		// fall back on s1.
		QuestTransition check = definition.transitions().stream()
			.filter(candidate -> "s1".equals(candidate.sourceNode()) && "s2".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())
			.findFirst().orElseThrow(() -> new AssertionError("missing group check edge s1->s2"));
		assertEquals(List.of(new QuestCondition.HasItem(contract.requiredItemId(), 1)), check.conditions(),
			"the check demands the required item");
		assertEquals(List.of(new QuestAction.RemoveItem(contract.requiredItemId(), 1),
			new QuestAction.SetVariable("var0", 2)), check.actions(),
			"the check consumes the required item and opens the hunt row");

		// hunt 段：s2 起的击杀边 = 实时击杀名单（显示名族闭包，按 M2-c 判例为超集语义）。
		// Hunt stage: the kill edges out of s2 are the live kill list (display-family closure, a
		// superset per the M2-c adjudication).
		Set<Integer> killTargets = new LinkedHashSet<>();
		for (QuestTransition candidate : definition.transitions()) {
			if ("s2".equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.KillNpc kill) {
				killTargets.add(kill.npcId());
			}
		}
		assertTrue(killTargets.containsAll(contract.killNpcIds()),
			() -> "the live kill list must cover the retail family: " + killTargets);

		// 逐杀走链：第 60 杀清零计数并落交付行；饱和后多余击杀不命中任何路线。
		// Walk the chain kill by kill: the 60th kill resets the counter into the handover row;
		// further kills plan nothing.
		QuestSnapshot state = new QuestSnapshot(7, contract.questId(), QuestStatus.START, 0, Map.of());
		state = apply(compiled, state, talkAdvanceEdge.event());
		assertEquals(Map.of("var0", 1, "var1", 0), layout.unpack(state.packedVariables()),
			"the letter advance must land on the hunt's opening row");
		/* 带必需品快照走成功边：HasItem 按快照物品栏求值，整组过/扣进采集行。 */
		/* With the required item in the snapshot's inventory the success edge plans into the hunt
		   row. */
		QuestSnapshot withGoods = new QuestSnapshot(7, contract.questId(), state.status(),
			state.packedVariables(), Map.of(contract.requiredItemId(), 1));
		state = apply(compiled, withGoods, new QuestEvent.TalkToNpc(contract.turnInNpcId(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Map.of("var0", 2, "var1", 0), layout.unpack(state.packedVariables()),
			"the check must reach the hunt row only with the goods");
		int probeNpcId = contract.killNpcIds().iterator().next();
		for (int count = 1; count < REQUIRED_KILLS; count++) {
			final int killIndex = count;
			state = apply(compiled, state, new QuestEvent.KillNpc(probeNpcId));
			assertEquals(Map.of("var0", 2, "var1", killIndex), layout.unpack(state.packedVariables()),
				() -> "kill " + killIndex + " must advance SECTION_1 on the hunt row");
		}
		state = apply(compiled, state, new QuestEvent.KillNpc(probeNpcId));
		assertEquals(Map.of("var0", 3, "var1", 0), layout.unpack(state.packedVariables()),
			"the 60th kill must reset the counter and open the handover row");
		QuestEvent probeKill = new QuestEvent.KillNpc(probeNpcId);
		final QuestSnapshot saturated = state;
		assertTrue(compiled.transitionsFor(probeKill.type()).stream()
			.noneMatch(transition -> QuestMutationPlanner.plan(compiled, saturated, probeKill, transition)
				.isPresent()),
			"the completed hunt must not count further kills");

		// 交付收尾：s3 的推进边（动作 id 从客户端页梯读）落领奖并授予任务凭证。
		// The handover: the s3 advance edge (action id read from the client page ladder) enters
		// reward and grants the voucher.
		QuestTransition handover = definition.transitions().stream()
			.filter(candidate -> "s3".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode()))
			.findFirst().orElseThrow(() -> new AssertionError("missing handover edge s3->reward"));
		assertEquals(List.of(), handover.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4),
			new QuestAction.GiveItem(contract.workItemId(), 1)), handover.actions(),
			"the handover opens the reward row and grants the voucher");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handover.afterCommit());
		state = apply(compiled, state, handover.event());
		assertEquals(QuestStatus.REWARD, state.status());
		assertEquals(Map.of("var0", 4, "var1", 0), layout.unpack(state.packedVariables()),
			"the handover must reach the reward row");

		// 日志修复：REWARD 状态下阶梯丢零时 EnterWorld 补写 reward 行（journalRowRepair）。
		// Journal repair: a lost ladder while in REWARD is rewritten to the reward row on
		// EnterWorld (journalRowRepair).
		QuestTransition repair = definition.transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld
				&& "reward".equals(candidate.targetNode()))
			.findFirst().orElseThrow(() -> new AssertionError("missing journal repair edge ->reward"));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), repair.actions(),
			"the repair must restore the reward row ladder");
	}

	private static QuestSnapshot apply(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestTransition> candidates = definition.transitionsFor(event.type()).stream()
			.filter(transition -> QuestEvent.matches(transition.event(), event))
			.toList();
		for (QuestTransition transition : candidates) {
			var plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isPresent()) {
				var mutation = plan.orElseThrow();
				return new QuestSnapshot(7, definition.id(), mutation.nextStatus(),
					mutation.nextPackedVariables(), Map.of());
			}
		}
		throw new AssertionError("no route for quest " + definition.id() + " event " + event);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/**
	 * 保存双阵营任务由 retail 步骤和 Aion 5.8 客户端证明的差异字段。
	 * Holds faction-specific fields proven by the retail steps and Aion 5.8 client.
	 */
	private record QuestContract(int questId, Set<Integer> killNpcIds, int turnInNpcId,
			int requiredItemId, int workItemId) {
	}
}
