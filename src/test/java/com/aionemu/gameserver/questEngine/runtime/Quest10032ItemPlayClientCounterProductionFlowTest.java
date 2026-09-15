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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证卡斯帕任务把「眼泪使用次数」放在客户端任务说明读取的低位槽（var0），阶段放在 var1，
 * 并且离开副本/死亡/下线的回退在缺少道具时仍能生效。
 *
 * Verifies the Caspa chain keeps the Taloc's Tears use counter in the low slot the 5.8 client
 * journal reads (var0), keeps the stage index in var1, and still rolls back on instance exit,
 * death, or logout even when the quest items are no longer in the cube.
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
	private static final int CLIENT_COUNTER_MASK = 0x3F;

	@TestFactory
	Stream<DynamicTest> keepsClientReadableTearsCounterAndReliableRollback() {
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
		assertDropGateFollowsClientCounter(definition, contract);
		assertHeartHandoverKeepsExactCount(definition, contract);
	}

	private static void assertClientReadableLayout(QuestDefinition definition) {
		BitField counter = definition.progressLayout().field("var0");
		BitField stage = definition.progressLayout().field("var1");
		assertEquals(0, counter.offset(), "眼泪次数必须占用客户端读取的最低位槽");
		assertEquals(TEARS_COUNT, counter.maxValue());
		assertEquals(6, stage.offset(), "阶段必须离开客户端计数槽，与 6 位步进约定一致");
		assertEquals(8, stage.maxValue());
		for (int uses = 0; uses <= TEARS_COUNT; uses++) {
			int expectedUses = uses;
			int packed = definition.progressLayout().pack(
				Map.of("var0", expectedUses, "var1", STAGE_TEARS));
			assertEquals(expectedUses, packed & CLIENT_COUNTER_MASK,
				() -> "客户端读取的低位槽必须等于真实使用次数: " + expectedUses);
		}
	}

	private static void assertStageProjections(QuestDefinition definition) {
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var1", 0));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var1", 3));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var1", 4));
		assertNode(definition, "s5", QuestStatus.START, Map.of("var1", STAGE_TEARS));
		assertNode(definition, "s6", QuestStatus.START, Map.of("var1", STAGE_KILL));
		assertNode(definition, "s7", QuestStatus.START, Map.of("var1", 7));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var1", 8));
	}

	private static void assertTearsCounterFlow(CompiledQuestDefinition compiled, QuestContract contract) {
		QuestEvent useTears = new QuestEvent.UseItem(contract.tearsItemId());
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", 0, "var1", STAGE_TEARS), Map.of(contract.tearsItemId(), 1),
			TALOCS_HOLLOW_WORLD_ID, compiled.definition());

		for (int uses = 1; uses < TEARS_COUNT; uses++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, useTears);
			assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), plan.requiredActions());
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", uses, "var1", STAGE_TEARS),
				compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
			assertEquals(uses, snapshot.packedVariables() & CLIENT_COUNTER_MASK);
		}

		QuestMutationPlan finalUse = dispatch(compiled, snapshot, useTears);
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", TEARS_COUNT),
			new QuestAction.SetVariable("var1", STAGE_KILL)), finalUse.requiredActions());
		snapshot = nextSnapshot(snapshot, finalUse);
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", TEARS_COUNT, "var1", STAGE_KILL),
			compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		assertEquals(TEARS_COUNT, snapshot.packedVariables() & CLIENT_COUNTER_MASK);
	}

	private static void assertRollbackWorksWithoutQuestItems(CompiledQuestDefinition compiled,
			QuestContract contract) {
		QuestSnapshot snapshot = snapshot(QuestStatus.START,
			Map.of("var0", 3, "var1", STAGE_TEARS), Map.of(), INGGISON_WORLD_ID, compiled.definition());

		QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.EnterWorld());
		List<QuestAction> actions = plan.requiredActions();
		assertEquals(4, actions.size());
		assertEquals(Set.of(
			new QuestAction.RemoveItem(contract.fruitItemId(), QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(contract.tearsItemId(), QuestAction.RemoveItem.ALL)),
			Set.copyOf(actions.subList(0, 2)), "回退必须回收两件任务道具且不再因缺物品被判不可行");
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 0),
			new QuestAction.SetVariable("var1", ROLLBACK_STAGE)), actions.subList(2, 4));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			plan.afterCommit());

		QuestSnapshot rolledBack = nextSnapshot(snapshot, plan);
		assertEquals(QuestStatus.START, rolledBack.status());
		assertEquals(Map.of("var0", 0, "var1", ROLLBACK_STAGE),
			compiled.definition().progressLayout().unpack(rolledBack.packedVariables()));
	}

	private static void assertDropGateFollowsClientCounter(QuestDefinition definition,
			QuestContract contract) {
		if (contract.heartItemId() == 0) {
			return;
		}
		assertTrue(definition.metadata().drops().stream().anyMatch(drop ->
			drop.npcId() == KILL_NPC_ID && drop.itemId() == contract.heartItemId()
				&& drop.collectingStep() == TEARS_COUNT),
			"掉落门禁必须匹配击杀阶段的槽位 0 值（满 20 次）");
		int killStagePacked = definition.progressLayout().pack(
			Map.of("var0", TEARS_COUNT, "var1", STAGE_KILL));
		assertEquals(TEARS_COUNT, killStagePacked & CLIENT_COUNTER_MASK);
		assertEquals(19, definition.progressLayout().pack(
			Map.of("var0", 19, "var1", STAGE_TEARS)) & CLIENT_COUNTER_MASK,
			"眼泪阶段未满 20 次时不得掉落心脏");
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
