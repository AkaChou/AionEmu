package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证卡斯帕任务把「阶段索引」放在客户端任务说明行索引读取的 SECTION_0（var0），
 * 把「眼泪使用次数」放在 SECTION_1（var1，offset 6），并保证离开副本/死亡/下线的
 * 回退在缺少道具时仍能生效。
 * Verifies the Caspa chain keeps the stage index in SECTION_0 (var0), the slot the 5.8 client
 * journal line index reads, keeps the Taloc's Tears use counter in SECTION_1 (var1, offset 6),
 * and still rolls back on instance exit, death, or logout even when the quest items are gone.
 */
class Quest10032ItemPlayClientCounterProductionFlowTest {
	private static final int TALOCS_HOLLOW_WORLD_ID = 300190000;
	private static final int INGGISON_WORLD_ID = 210050000;
	private static final int KILL_NPC_ID = 215488;
	private static final int TEARS_COUNT = 20;
	private static final int STAGE_TEARS = 5;
	private static final int STAGE_KILL = 6;
	private static final int ROLLBACK_STAGE = 2;
	private static final int LOTHA_REPORT_NPC_ID = 799503;
	private static final int SECTION_MASK = 0x3F;

	@TestFactory
	Stream<DynamicTest> keepsClientJournalStageAndReadableTearsCounter() {
		return Stream.of(
			new QuestContract(10032, 182215618, 182215619, 182215620),
			new QuestContract(20032, 182215593, 182215592, 0))
			.map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
				() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();

		assertClientReadableLayout(definition);
		assertStageProjections(definition);
		assertTearsCounterFlow(compiled, contract);
		assertRollbackWorksWithoutQuestItems(compiled, contract);
		assertKillStageSurvivesLeavingTheInstance(compiled);
		assertHeartStageTurnsInOnLeaving(compiled, contract);
		assertDropGateFollowsStageSlot(definition, contract);
		assertHeartHandoverKeepsExactCount(definition, contract);
	}

	private static void assertClientReadableLayout(QuestDefinition definition) {
		BitField stage = definition.progressLayout().field("var0");
		BitField counter = definition.progressLayout().field("var1");
		assertEquals(0, stage.offset(), "阶段必须占用客户端任务说明行索引读取的 SECTION_0");
		assertEquals(8, stage.maxValue());
		assertEquals(6, counter.offset(), "眼泪次数必须放在 SECTION_1（offset 6），不能占用步进行");
		assertEquals(TEARS_COUNT, counter.maxValue());

		for (int stageIndex = 0; stageIndex <= 8; stageIndex++) {
			int expectedStage = stageIndex;
			int packed = definition.progressLayout().pack(Map.of("var0", expectedStage, "var1", 0));
			assertEquals(expectedStage, packed & SECTION_MASK,
				() -> "客户端任务说明行索引必须等于阶段: " + expectedStage);
		}
		for (int uses = 0; uses <= TEARS_COUNT; uses++) {
			int expectedUses = uses;
			int packed = definition.progressLayout().pack(
				Map.of("var0", STAGE_TEARS, "var1", expectedUses));
			assertEquals(STAGE_TEARS, packed & SECTION_MASK,
				"眼泪阶段的任务说明行索引必须保持 5");
			assertEquals(expectedUses, (packed >> 6) & SECTION_MASK,
				() -> "SECTION_1 必须等于真实使用次数: " + expectedUses);
		}
	}

	private static void assertStageProjections(QuestDefinition definition) {
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", STAGE_TEARS));
		assertNode(definition, "s6", QuestStatus.START, Map.of("var0", STAGE_KILL));
		assertNode(definition, "s7", QuestStatus.START, Map.of("var0", 7));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 8));
	}

	private static void assertTearsCounterFlow(CompiledQuestDefinition compiled, QuestContract contract) {
		QuestEvent useTears = new QuestEvent.UseItem(contract.tearsItemId());
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", STAGE_TEARS, "var1", 0), Map.of(contract.tearsItemId(), 1),
			TALOCS_HOLLOW_WORLD_ID, compiled.definition());

		for (int uses = 1; uses < TEARS_COUNT; uses++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, useTears);
			assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), plan.requiredActions());
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", STAGE_TEARS, "var1", uses),
				compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
			assertEquals(STAGE_TEARS, snapshot.packedVariables() & SECTION_MASK,
				"使用过程中任务说明行索引必须保持眼泪阶段");
			assertEquals(uses, (snapshot.packedVariables() >> 6) & SECTION_MASK,
				"SECTION_1 必须逐步增长");
		}

		QuestMutationPlan finalUse = dispatch(compiled, snapshot, useTears);
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", TEARS_COUNT),
			new QuestAction.SetVariable("var0", STAGE_KILL)), finalUse.requiredActions());
		snapshot = nextSnapshot(snapshot, finalUse);
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", STAGE_KILL, "var1", TEARS_COUNT),
			compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		assertEquals(STAGE_KILL, snapshot.packedVariables() & SECTION_MASK,
			"第 20 次使用必须把任务说明切到击杀阶段");
		assertEquals(TEARS_COUNT, (snapshot.packedVariables() >> 6) & SECTION_MASK);
	}

	private static void assertRollbackWorksWithoutQuestItems(CompiledQuestDefinition compiled,
			QuestContract contract) {
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", STAGE_TEARS, "var1", 3), Map.of(), INGGISON_WORLD_ID, compiled.definition());

		QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.EnterWorld());
		List<QuestAction> actions = plan.requiredActions();
		assertEquals(4, actions.size());
		assertEquals(Set.of(
			new QuestAction.RemoveItem(contract.fruitItemId(), QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(contract.tearsItemId(), QuestAction.RemoveItem.ALL)),
			Set.copyOf(actions.subList(0, 2)), "回退必须回收两件任务道具且不再因缺物品被判不可行");
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 0),
			new QuestAction.SetVariable("var0", ROLLBACK_STAGE)), actions.subList(2, 4));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			plan.afterCommit());

		QuestSnapshot rolledBack = nextSnapshot(snapshot, plan);
		assertEquals(QuestStatus.START, rolledBack.status());
		assertEquals(Map.of("var0", ROLLBACK_STAGE, "var1", 0),
			compiled.definition().progressLayout().unpack(rolledBack.packedVariables()));
	}

	private static void assertKillStageSurvivesLeavingTheInstance(CompiledQuestDefinition compiled) {
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", STAGE_KILL, "var1", TEARS_COUNT), Map.of(), INGGISON_WORLD_ID, compiled.definition());
		for (QuestEvent event : List.of(new QuestEvent.EnterWorld(), new QuestEvent.Die(), new QuestEvent.LogOut())) {
			assertFalse(hasMatchingPlan(compiled, snapshot, event),
				"完成 20 次眼泪后离开副本/死亡/下线不得回退: " + event);
		}
	}

	private static void assertHeartStageTurnsInOnLeaving(CompiledQuestDefinition compiled,
			QuestContract contract) {
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", 7, "var1", TEARS_COUNT), Map.of(), INGGISON_WORLD_ID, compiled.definition());
		QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.EnterWorld());
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(Map.of("var0", 8, "var1", TEARS_COUNT),
			compiled.definition().progressLayout().unpack(plan.nextPackedVariables()));
		List<QuestAction> actions = plan.requiredActions();
		assertEquals(Set.of(
			new QuestAction.RemoveItem(contract.fruitItemId(), QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(contract.tearsItemId(), QuestAction.RemoveItem.ALL)),
			Set.copyOf(actions.subList(0, 2)), "离副本转 reward 时必须回收两件任务道具");
		assertTrue(actions.subList(2, actions.size()).contains(new QuestAction.SetVariable("var0", 8)),
			"离副本转 reward 时必须提交阶段 8");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), plan.afterCommit());
	}

	private static boolean hasMatchingPlan(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		return compiled.definition().transitions().stream()
			.anyMatch(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent());
	}

	private static void assertDropGateFollowsStageSlot(QuestDefinition definition,
			QuestContract contract) {
		if (contract.heartItemId() == 0) {
			return;
		}
		assertTrue(definition.metadata().drops().stream().anyMatch(drop ->
			drop.npcId() == KILL_NPC_ID && drop.itemId() == contract.heartItemId()
				&& drop.collectingStep() == STAGE_KILL),
			"掉落门禁必须匹配击杀阶段的 SECTION_0 = 6");
		int killStagePacked = definition.progressLayout().pack(
			Map.of("var0", STAGE_KILL, "var1", TEARS_COUNT));
		assertEquals(STAGE_KILL, killStagePacked & SECTION_MASK);
		int tearsStagePacked = definition.progressLayout().pack(
			Map.of("var0", STAGE_TEARS, "var1", 19));
		assertEquals(STAGE_TEARS, tearsStagePacked & SECTION_MASK,
			"眼泪阶段未满 20 次时不得满足击杀阶段门禁");
	}

	private static void assertHeartHandoverKeepsExactCount(QuestDefinition definition,
			QuestContract contract) {
		if (contract.heartItemId() == 0) {
			return;
		}
		QuestEvent submit = new QuestEvent.TalkToNpc(LOTHA_REPORT_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("s7"))
			.filter(candidate -> candidate.targetNode().equals("s7"))
			.filter(candidate -> candidate.event().equals(submit))
			.toList();
		assertEquals(2, routes.size(), "心脏上交路由应同时包含持有与缺失两种分支");
		List<QuestAction.RemoveItem> removals = routes.stream()
			.flatMap(route -> route.actions().stream())
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(QuestAction.RemoveItem.class::cast)
			.toList();
		assertEquals(List.of(new QuestAction.RemoveItem(contract.heartItemId(), 1)), removals,
			"上交心脏是精确数量消耗，不得被清理语义的 count=ALL 覆盖");
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory(), Map.of(), true, true, 0, 0,
			snapshot.worldId(), snapshot.instanceId(), 0f, 0f, 0f, (byte) 0);
	}

	private static QuestSnapshot snapshot(QuestStatus status, Map<String, Integer> variables,
			Map<Integer, Integer> inventory, int worldId, QuestDefinition definition) {
		return new QuestSnapshot(7, definition.id(), status, definition.progressLayout().pack(variables),
			inventory, Map.of(), true, true, 0, 0, worldId, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest10032ItemPlayClientCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestContract(int questId, int fruitItemId, int tearsItemId, int heartItemId) {
	}
}
