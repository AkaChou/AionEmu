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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 22：24046（The Shadow Calls）副本段的任务书行号阶梯，并用天族镜像 14046 作横向对照。
 * <p>
 * 族级判据：两侧客户端 {@code quest_summary} 都是 8 行（行 0..7，末行向领奖 NPC 报告）。
 * 24046 的行 4「进入 DC1_door_Q2076 寻找沉默审判官」与行 5「找到 IDDC1_Arena_3F_Exit 逃出秘密监狱」
 * 在迁移前的 legacy handler（{@code _24046The_Shadow_Calls}）里被压成同一格：与加尔姆对话时
 * {@code changeQuestStep(3, 5)} 直接进副本，出口物件 700369 再做 5→6，于是客户端行 4 永远不亮；
 * reward 投影也停在 6，领奖行（行 7）与行 6 共用投影（QE-051 行错位）。
 * <p>
 * 本门禁锁：每个客户端行都有独立 START/REWARD 状态（0..7 连续）、副本段每步只推一格
 * （3→4 与加尔姆对话进副本、4→5 踏入副本世界、5→6 用出口逃出、6→REWARD(7) 向 Muninn 报告）、
 * 乱序/回看不产生计划、{@code REWARD + var0==6} 的旧存档在进入世界时自愈到 7、
 * 领奖与完成 owner 唯一（Muninn 203550）、出口物件只在行 5（副本内）可用并传送回主城，
 * 且 24046 不得混入镜像 14046 的道具/影片推进链。
 * <p>
 * Locks batch 22: the eight-row journal ladder of 24046, whose rows 4 and 5 were collapsed into one step by
 * the legacy {@code changeQuestStep(3, 5)} jump while the reward projection stayed at row 6. The Elyos twin
 * 14046 already owns all eight rows; this gate pins the Asmodian ladder, the instance/escape conditionality,
 * the legacy reward-row self-heal and the unique Muninn(203550) owner, and keeps the two variants apart.
 */
class ShadowCourtRowLadderContractTest {

	/** 魔族侧任务 / 天族镜像 / 副本世界 / 出口传送目标 / 关键 NPC 与物件。 */
	private static final int ASMODIAN = 24046;
	private static final int ELYOS_TWIN = 14046;
	private static final int SHADOW_COURT = 320120000;
	private static final int PANDAEMONIUM = 120010000;
	private static final int PHYPER = 798300;
	private static final int GARM = 204089;
	private static final int KHRUDGELMIR = 204253;
	private static final int MUNINN = 203550;
	private static final int EXIT_OBJECT = 700369;
	/** 领奖行号与旧投影 / reward row index and the legacy projection it replaces. */
	private static final int REWARD_ROW = 7;
	private static final int LEGACY_REWARD_ROW = 6;

	@Test
	void everyClientRowOwnsOneLadderState() throws Exception {
		Set<String> expectedNodes = Set.of("unaccepted", "started", "s1", "s2", "s3", "s4", "s5", "s6",
			"reward", "complete");
		for (int questId : List.of(ASMODIAN, ELYOS_TWIN)) {
			QuestDefinition definition = definition(questId).definition();
			assertEquals(expectedNodes,
				definition.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				() -> "quest " + questId + " node shape");
			BitField field = definition.progressLayout().field("var0");
			assertNotNull(field, () -> "quest " + questId + " must declare var0 as the journal row index");
			assertEquals(0, field.offset(), () -> "quest " + questId + " var0 must stay in SECTION_0");
			assertEquals(REWARD_ROW, field.maxValue(),
				() -> "quest " + questId + " var0 must be able to hold the reward row " + REWARD_ROW);
			for (int row = 0; row <= 6; row++) {
				final int currentRow = row;
				QuestNode node = node(definition, rowNode(currentRow));
				assertEquals(currentRow, node.projection().variables().get("var0"),
					() -> "quest " + questId + " node " + rowNode(currentRow) + " row projection");
				assertEquals(QuestStatus.START, node.projection().status(),
					() -> "quest " + questId + " node " + rowNode(currentRow) + " status");
			}
			assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
				() -> "quest " + questId + " reward must project the client reward row");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + questId + " reward status");
			/* 每一行都必须在 START/REWARD 节点里出现过，否则客户端那一行永远不亮。 */
			/* Every row must appear on a START/REWARD node or the client row can never light up. */
			assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7), visibleRows(definition),
				() -> "quest " + questId + " rows without a state");
		}
	}

	@Test
	void instanceSegmentAdvancesExactlyOneRowPerEvent() throws Exception {
		QuestDefinition definition = definition(ASMODIAN).definition();
		/* 旧 handler 的 3 -> 5 跳格必须删除（行 4 因此才能亮）。 */
		/* The legacy 3 -> 5 row jump must be gone so row 4 can light up. */
		assertTrue(definition.transitions().stream().noneMatch(route ->
				"s3".equals(route.sourceNode()) && "s5".equals(route.targetNode())),
			"the legacy s3 -> s5 row jump must be removed");

		QuestTransition enter = route(definition, "s3", "s4");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), enter.actions(),
			"row 3 -> row 4 must advance exactly one row");
		assertTrue(teleportWorlds(enter).contains(SHADOW_COURT),
			"row 3 -> row 4 must keep the Shadow Court instance teleport");
		QuestEvent.TalkToNpc enterDialog = assertInstanceOf(QuestEvent.TalkToNpc.class, enter.event());
		assertEquals(GARM, enterDialog.npcId(), "row 4 is opened by the arena manager Garm");

		QuestTransition inside = route(definition, "s4", "s5");
		assertInstanceOf(QuestEvent.EnterWorld.class, inside.event(),
			"row 4 -> row 5 is driven by entering the instance world");
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4),
			new QuestCondition.WorldIs(SHADOW_COURT, true)), inside.conditions(),
			"row 4 -> row 5 must require the player to be inside the Shadow Court instance");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), inside.actions(),
			"row 4 -> row 5 must advance exactly one row");

		QuestTransition fallback = route(definition, "s4", "s3");
		assertInstanceOf(QuestEvent.EnterWorld.class, fallback.event(),
			"a row-4 save outside the instance must roll back");
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4),
			new QuestCondition.WorldIs(SHADOW_COURT, false)), fallback.conditions(),
			"row 4 rollback only applies outside the instance");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), fallback.actions(),
			"row 4 rollback must re-arm the Garm dialog");

		QuestTransition escape = route(definition, "s5", "s6");
		QuestEvent.TalkToNpc escapeObject = assertInstanceOf(QuestEvent.TalkToNpc.class, escape.event());
		assertEquals(EXIT_OBJECT, escapeObject.npcId(), "row 6 is reached through the 3F exit object");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), escape.actions(),
			"row 5 -> row 6 must advance exactly one row");
		assertTrue(teleportWorlds(escape).contains(PANDAEMONIUM),
			"leaving the secret prison must teleport the player back out");

		QuestTransition report = route(definition, "s6", "reward");
		QuestEvent.TalkToNpc reportDialog = assertInstanceOf(QuestEvent.TalkToNpc.class, report.event());
		assertEquals(KHRUDGELMIR, reportDialog.npcId(), "row 7 is opened by Khrudgelmir");
		assertTrue(report.actions().stream().noneMatch(action -> action instanceof QuestAction.SetVariable set
			&& "var0".equals(set.field())), "the reward row comes from the target projection, not an action");
	}

	@Test
	void rowsAdvanceSeriallyAndOutOfOrderEventsProduceNoPlan() throws Exception {
		CompiledQuestDefinition compiled = definition(ASMODIAN);
		QuestDefinition definition = compiled.definition();
		List<QuestTransition> ladder = List.of(
			route(definition, "started", "s1"), route(definition, "s1", "s2"), route(definition, "s2", "s3"),
			route(definition, "s3", "s4"), route(definition, "s4", "s5"), route(definition, "s5", "s6"),
			route(definition, "s6", "reward"));
		for (int row = 0; row < ladder.size(); row++) {
			final int currentRow = row;
			final QuestTransition step = ladder.get(currentRow);
			/* 进入副本的那一步只有身处副本世界时才成立。 */
			/* The instance step only holds while the player is inside the Shadow Court. */
			int world = "s4".equals(step.sourceNode()) ? SHADOW_COURT : PANDAEMONIUM;
			Map<Integer, Integer> carried = carriedItems(step);
			QuestMutationPlan plan = plan(compiled, QuestStatus.START, currentRow, world, step, carried);
			assertNotNull(plan, () -> "row " + currentRow + " must advance through " + step.sourceNode()
				+ " -> " + step.targetNode());
			assertEquals(currentRow + 1, row(compiled, plan),
				() -> "row " + currentRow + " must advance exactly one row");
			assertEquals(currentRow + 1 == REWARD_ROW ? QuestStatus.REWARD : QuestStatus.START,
				plan.nextStatus(), () -> "row " + currentRow + " status");
			/* 同一步在别的行号上（回看/乱序）不得产生任何计划。 */
			/* Replaying that step from another row must not produce a plan. */
			assertNull(plan(compiled, QuestStatus.START, (currentRow + 1) % 7, world, step, carried),
				() -> "step for row " + currentRow + " must not fire from another row");
		}
		QuestTransition fallback = route(definition, "s4", "s3");
		QuestMutationPlan rollback = plan(compiled, QuestStatus.START, 4, PANDAEMONIUM, fallback, Map.of());
		assertNotNull(rollback, "a row-4 save outside the instance must roll back to row 3");
		assertEquals(3, row(compiled, rollback), "row 4 rollback target");
		assertNull(plan(compiled, QuestStatus.START, 4, SHADOW_COURT, fallback, Map.of()),
			"the rollback must not fire inside the instance");
	}

	@Test
	void legacyRewardRowSelfHealsOnEnterWorld() throws Exception {
		CompiledQuestDefinition compiled = definition(ASMODIAN);
		List<QuestTransition> recovery = compiled.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(1, recovery.size(), "exactly one enter-world reward recovery route");
		QuestTransition heal = recovery.getFirst();
		assertEquals(List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", LEGACY_REWARD_ROW)), heal.conditions(),
			"the recovery route must target the legacy reward projection");
		assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
			"the recovery route must write the reward row");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
			"the recovery route must refresh the journal");

		QuestMutationPlan repaired = plan(compiled, QuestStatus.REWARD, LEGACY_REWARD_ROW, PANDAEMONIUM, heal,
			Map.of());
		assertNotNull(repaired, "a REWARD save on the legacy row must be repaired");
		assertEquals(REWARD_ROW, row(compiled, repaired), "repaired reward row");
		assertEquals(QuestStatus.REWARD, repaired.nextStatus(), "repair keeps the reward status");
		assertNull(plan(compiled, QuestStatus.REWARD, REWARD_ROW, PANDAEMONIUM, heal, Map.of()),
			"an already repaired reward save must not be rewritten");

		/* 旧投影下的领奖入口也必须在同一个 owner 上（QE-052）。 */
		/* The reward entry must stay on the same owner (QE-052). */
		assertEquals(Set.of(MUNINN), npcIds(compiled.definition(), "reward", "complete"),
			"completion owner must be the client reward-row NPC");
		assertTrue(compiled.definition().transitions().stream()
				.filter(candidate -> "reward".equals(candidate.sourceNode())
					&& "reward".equals(candidate.targetNode()))
				.allMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == MUNINN),
			"every reward re-entry route must stay on Muninn");
	}

	@Test
	void exitObjectIsUsableOnlyInsideTheInstance() throws Exception {
		QuestDefinition definition = definition(ASMODIAN).definition();
		List<QuestTransition> canAct = definition.transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.CanAct can
				&& can.templateId() == EXIT_OBJECT)
			.toList();
		assertEquals(1, canAct.size(), "the 3F exit object must have exactly one activation route");
		assertEquals("s5", canAct.getFirst().sourceNode(),
			"the 3F exit may only be used on row 5 (inside the instance)");
		/* 副本内的既有无退出路（死亡/切到副本外）必须保留，避免玩家被锁在副本状态。 */
		/* The legacy in-instance escapes (death, leaving the instance world) must be preserved. */
		QuestTransition onDie = route(definition, "s5", "s3");
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 5)), onDie.conditions(),
			"the die rollback keeps its row-5 guard");
		List<QuestTransition> leaveRollbacks = definition.transitions().stream()
			.filter(candidate -> "s5".equals(candidate.sourceNode()) && "s3".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(1, leaveRollbacks.size(), "one enter-world rollback for row 5");
		assertTrue(leaveRollbacks.getFirst().conditions().contains(
			new QuestCondition.WorldIs(SHADOW_COURT, false)),
			"the row-5 rollback only fires outside the instance");
	}

	@Test
	void asmodianSegmentIsNotPollutedByTheElyosMovieRoute() throws Exception {
		QuestDefinition asmodian = definition(ASMODIAN).definition();
		QuestDefinition elyos = definition(ELYOS_TWIN).definition();
		/* 天族侧用「使用记忆碎片 + 播放影片」推进行 4，魔族侧用副本世界推进；两侧不得互相套用。 */
		/* The Elyos variant advances row 4 with an item use plus a movie; the Asmodian variant advances it by
		   entering the instance. Neither shape may leak into the other variant. */
		assertTrue(elyos.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.UseItem use && use.itemId() == 182215354),
			"the Elyos twin keeps its item-driven row 4");
		assertTrue(asmodian.transitions().stream().noneMatch(candidate ->
				candidate.event() instanceof QuestEvent.UseItem), "the Asmodian variant has no item-driven row");
		assertTrue(asmodian.transitions().stream()
			.flatMap(candidate -> candidate.afterCommit().stream())
			.noneMatch(action -> action instanceof AfterCommitAction.PlayMovie),
			"the Asmodian variant plays no movie");
		assertTrue(elyos.transitions().stream()
			.flatMap(candidate -> candidate.afterCommit().stream())
			.noneMatch(action -> action instanceof AfterCommitAction.TeleportPlayer teleport
				&& teleport.worldId() == SHADOW_COURT),
			"the Elyos twin must not teleport into the Asmodian instance");
		/* 两侧共用的行号口径：领奖行 = 客户端末行 7，且行 0 的接取 NPC 各自独立。 */
		/* Shared row contract: reward row equals the client's last row (7), while the offer NPC stays per side. */
		assertEquals(REWARD_ROW, visibleRows(asmodian).stream().max(Integer::compareTo).orElseThrow());
		assertEquals(REWARD_ROW, visibleRows(elyos).stream().max(Integer::compareTo).orElseThrow());
		assertEquals(PHYPER, ((QuestEvent.TalkToNpc) route(asmodian, "started", "s1").event()).npcId(),
			"the Asmodian offer row is opened by Phyper");
	}

	private static String rowNode(int row) {
		return row == 0 ? "started" : "s" + row;
	}

	private static Set<Integer> visibleRows(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				|| node.projection().status() == QuestStatus.REWARD)
			.map(node -> node.projection().variables().get("var0"))
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private static Set<Integer> npcIds(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc)
			.map(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId())
			.collect(Collectors.toSet());
	}

	private static Set<Integer> teleportWorlds(QuestTransition transition) {
		return transition.afterCommit().stream()
			.filter(action -> action instanceof AfterCommitAction.TeleportPlayer)
			.map(action -> ((AfterCommitAction.TeleportPlayer) action).worldId())
			.collect(Collectors.toSet());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing route " + source + " -> " + target));
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition compiled, QuestStatus status, int currentRow,
			int worldId, QuestTransition transition, Map<Integer, Integer> inventory) {
		Map<String, Integer> variables = new LinkedHashMap<>(compiled.definition().progressLayout().unpack(0));
		variables.put("var0", currentRow);
		QuestSnapshot snapshot = new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(variables), inventory, Map.of(),
			true, true, 0, 0, worldId, 0, 0f, 0f, 0f, (byte) 0);
		return QuestMutationPlanner.plan(compiled, snapshot, transition.event(), transition).orElse(null);
	}

	/**
	 * 取出转换要求的交付物，供快照携带：remove-item 的可行性由物品事实决定，
	 * 不携带就无法规划该步骤（与运行时一致）。
	 * Carries the turn-in items a transition consumes: remove-item feasibility depends on the captured
	 * inventory, so a step that removes an item cannot be planned without holding it (same as runtime).
	 */
	private static Map<Integer, Integer> carriedItems(QuestTransition transition) {
		Map<Integer, Integer> inventory = new LinkedHashMap<>();
		for (QuestAction action : transition.actions()) {
			if (action instanceof QuestAction.RemoveItem removal && !removal.removeAll()) {
				inventory.merge(removal.itemId(), removal.count(), Integer::sum);
			}
		}
		return inventory;
	}

	private static int row(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables()).get("var0");
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = ShadowCourtRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
