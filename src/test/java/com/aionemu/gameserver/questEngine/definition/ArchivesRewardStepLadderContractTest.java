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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 28：永恒档案馆领奖阶任务 16800/26800 的 legacy 落盘 step 与三段行阶梯（QE-054 + QE-051）。
 * <p>
 * 客户端 quest_q16800.html / quest_q26800.html 的 quest_summary 都是 3 行：行 0 = 使用激活的塔碎片进入
 * （{@code LF_Tower} / {@code DF_Tower}）、行 1 = 和永恒之塔警备组长 Etezar（806232）/ Enfitenta（806233）对话、
 * 行 2 = 向代理人维达（806148）/ 佩莱格兰（806149）报告（领奖行）。legacy
 * {@code _16800Into_The_Archives} / {@code _26800A_Call_For_Champions} 的落盘 step 链是
 * 感应区 {@code changeQuestStep(env, 0, 1, false)} → 警备组长 {@code changeQuestStep(env, 1, 2, false)} →
 * 知识书库 {@code changeQuestStep(env, 2, 3, true)}；旧引擎最后这条 {@code ,true)} 只置 REWARD、
 * 不写 nextStep，因此领奖态落盘 step 仍是 2（= 客户端末行索引）。
 * <p>
 * 旧 XML 分别把领奖态写成 1（16800：var0 只有 1 bit、没有 s1/s2、用 {@code at-distance 206535} + var1
 * 旗标代替 zone 推进，玩家还能在 started 态从 806075/806148/806232 直跳领奖）与 3（26800：{@code s2 -> reward}
 * 又显式写 {@code var0=3}）。本门禁锁定两侧同形的 {@code unaccepted(0)/started(0)/s1(1)/s2(2)/reward(2)/complete(0)}、
 * 三段推进只由 legacy 事件触发、任何进入领奖的路线都不得写过期的行号、旧存档进入世界时自愈、正规态不重放。
 * <p>
 * Locks batch 28: the legacy-persisted reward step (2, which is also the client journal's last row) and the
 * three-stage ladder of the Archives quests 16800/26800, including the stale-save recovery edge.
 */
class ArchivesRewardStepLadderContractTest {

	/** 客户端任务书末行的索引（= 领奖态 packed step）。 */
	private static final int REWARD_ROW = 2;

	/** 任务 / 接取 NPC / 行 1 NPC / 领奖 NPC / 塔感应区 / 知识书库区 / 影片 / 旧投影行。 */
	private record Contract(int questId, int starterNpc, int handoffNpc, int rewardNpc,
			String towerZone, String archivesZone, int movieId, int staleRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(16800, 806075, 806232, 806148,
			"LF_TOWER_SENSORY_AREA_Q16800_210110000", "IDETERNITY_01_Q16800_301540000", 931, 1),
		new Contract(26800, 806079, 806233, 806149,
			"DF_TOWER_SENSORY_AREA_Q26800_220120000", "IDETERNITY_01_Q16800_301540000", 932, 3));

	@Test
	void rewardStepEqualsTheLegacyPersistedStepAndTheLastJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", REWARD_ROW)),
				node(definition, "reward").projection(),
				() -> "quest " + contract.questId() + " reward projection");
			assertEquals(List.of(0, 1, 2), visibleRows(definition),
				() -> "quest " + contract.questId() + " journal rows without a state");
			// 行索引是唯一进度字段：旧 XML 的 var1 影片旗标必须退场。
			assertEquals(List.of("var0"), definition.progressLayout().fields().stream()
					.map(BitField::name).toList(),
				() -> "quest " + contract.questId() + " progress fields");
			BitField row = definition.progressLayout().field("var0");
			assertTrue(row.maxValue() >= REWARD_ROW, () -> "quest " + contract.questId()
				+ " var0 max " + row.maxValue() + " cannot hold the reward row");
		}
	}

	@Test
	void ladderAdvancesOnlyOnTheLegacyEvents() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();

			QuestTransition towerArrival = transition(definition, "started", "s1",
				new QuestEvent.EnterZone(contract.towerZone()));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)),
				towerArrival.conditions(), () -> "quest " + contract.questId() + " tower arrival");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), towerArrival.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				towerArrival.afterCommit());

			QuestTransition handoff = transition(definition, "s1", "s2",
				new QuestEvent.TalkToNpc(contract.handoffNpc(), QuestDialogAction.SET_SUCCEED.id()));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)),
				handoff.conditions(), () -> "quest " + contract.questId() + " sentry handover");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), handoff.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), handoff.afterCommit());

			QuestTransition archivesArrival = transition(definition, "s2", "reward",
				new QuestEvent.EnterZone(contract.archivesZone()));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)),
				archivesArrival.conditions(), () -> "quest " + contract.questId() + " archives arrival");
			assertEquals(List.of(), archivesArrival.actions(),
				() -> "quest " + contract.questId() + " must keep the packed step at 2");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.PlayMovie(contract.movieId())), archivesArrival.afterCommit());

			assertAdvance(compiled, QuestStatus.START, 0, towerArrival, QuestStatus.START, 1);
			assertAdvance(compiled, QuestStatus.START, 1, handoff, QuestStatus.START, 2);
			assertAdvance(compiled, QuestStatus.START, 2, archivesArrival, QuestStatus.REWARD, REWARD_ROW);
		}
	}

	@Test
	void noRouteSkipsJournalRowsIntoReward() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> entries = definition.transitions().stream()
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.sourceNode() != null)
				.filter(route -> !route.targetNode().equals(route.sourceNode()))
				.toList();
			assertEquals(List.of("s2"), entries.stream().map(QuestTransition::sourceNode).toList(),
				() -> "quest " + contract.questId() + " reward entries must come from the handoff stage");
			// 领奖态自身只保留领奖页/交付协议路由，任何进入领奖的路线都不得写过期的行号。
			for (QuestTransition route : definition.transitions().stream()
					.filter(candidate -> "reward".equals(candidate.targetNode()))
					.toList()) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(REWARD_ROW, value, () -> "quest " + contract.questId()
							+ " reward entry writes a stale journal row");
					}
				}
			}
			// 行 1 的对话只能在 s1 -> s2 之间推进，不得直接进入领奖态。
			assertTrue(routes(definition, "s1", contract.handoffNpc()).stream()
					.noneMatch(route -> "reward".equals(route.targetNode())),
				() -> "quest " + contract.questId() + " sentry must not reach reward directly");
			// 接取 NPC 也不能在 START 态把玩家送进领奖态。
			assertTrue(routes(definition, "started", contract.starterNpc()).stream()
					.noneMatch(route -> "reward".equals(route.targetNode())),
				() -> "quest " + contract.questId() + " starter must not reach reward");
		}
	}

	@Test
	void staleRewardStepIsRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", contract.staleRow())), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), recovery.actions(),
				() -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", contract.staleRow())),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void liveRewardStepIsNotReplayed() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			assertTrue(plans(compiled, QuestStatus.REWARD, Map.of("var0", REWARD_ROW),
					new QuestEvent.EnterWorld()).isEmpty(),
				() -> "quest " + contract.questId()
					+ " must not replay the recovery edge for the live reward row");
		}
	}

	@Test
	void rewardOwnerStaysOnTheAgentNamedByTheJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> completions = definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.toList();
			assertFalse(completions.isEmpty(), () -> "quest " + contract.questId() + " completion routes");
			for (QuestTransition route : completions) {
				assertEquals(contract.rewardNpc(), dialogNpc(route),
					() -> "quest " + contract.questId() + " completion owner");
			}
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				transition(definition, "reward", "reward", new QuestEvent.TalkToNpc(
					contract.rewardNpc(), QuestDialogAction.QUEST_SELECT.id())).afterCommit(),
				() -> "quest " + contract.questId() + " reward entry page");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				transition(definition, "s1", "s1", new QuestEvent.TalkToNpc(
					contract.handoffNpc(), QuestDialogAction.QUEST_SELECT.id())).afterCommit(),
				() -> "quest " + contract.questId() + " sentry page");
			assertTrue(routes(definition, "reward", contract.starterNpc()).isEmpty(),
				() -> "quest " + contract.questId() + " starter must not own the reward stage");
			assertTrue(routes(definition, "reward", contract.handoffNpc()).isEmpty(),
				() -> "quest " + contract.questId() + " sentry must not own the reward stage");
		}
	}

	@Test
	void bothSidesShareTheSameJournalLadderShape() throws Exception {
		QuestDefinition elyos = definition(16800).definition();
		QuestDefinition asmodians = definition(26800).definition();
		assertEquals(elyos.nodes(), asmodians.nodes(),
			"the Elyos/Asmodian Archives rewards must expose identical node projections");
		assertEquals(ladderShape(elyos, CONTRACTS.get(0)), ladderShape(asmodians, CONTRACTS.get(1)),
			"the Elyos/Asmodian Archives rewards must expose the same stage ladder");
		assertEquals(stageEdges(elyos), stageEdges(asmodians),
			"the Elyos/Asmodian Archives rewards must expose the same stage graph");
	}

	/** 三段推进的条件/动作形状（zone 名与 NPC/影片 id 之外的合同字段）。 */
	private static List<String> ladderShape(QuestDefinition definition, Contract contract) {
		List<QuestTransition> ladder = List.of(
			transition(definition, "started", "s1", new QuestEvent.EnterZone(contract.towerZone())),
			transition(definition, "s1", "s2", new QuestEvent.TalkToNpc(contract.handoffNpc(),
				QuestDialogAction.SET_SUCCEED.id())),
			transition(definition, "s2", "reward", new QuestEvent.EnterZone(contract.archivesZone())));
		return ladder.stream()
			.map(route -> route.sourceNode() + "->" + route.targetNode()
				+ route.conditions() + route.actions())
			.toList();
	}

	private static List<String> stageEdges(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(route -> route.sourceNode() != null)
			.filter(route -> Set.of("s1", "s2", "reward").contains(route.sourceNode())
				|| Set.of("s1", "s2", "reward").contains(route.targetNode()))
			.map(route -> route.sourceNode() + "->" + route.targetNode())
			.sorted()
			.toList();
	}

	private static void assertAdvance(CompiledQuestDefinition compiled, QuestStatus fromStatus,
			int fromRow, QuestTransition route, QuestStatus toStatus, int toRow) {
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, fromStatus, Map.of("var0", fromRow)), route).orElseThrow(() ->
				new AssertionError("quest " + compiled.id() + " route " + route.sourceNode() + " -> "
					+ route.targetNode() + " must be plannable from " + fromStatus + "/var0=" + fromRow));
		assertEquals(toStatus, plan.nextStatus(), () -> "quest " + compiled.id() + " next status");
		assertEquals(toRow, unpack(compiled, plan).get("var0"), () -> "quest " + compiled.id() + " next row");
	}

	private static List<Integer> visibleRows(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				|| node.projection().status() == QuestStatus.REWARD)
			.map(node -> node.projection().variables().get("var0"))
			.distinct()
			.sorted()
			.toList();
	}

	private static int dialogNpc(QuestTransition route) {
		return route.event() instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : -1;
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), () -> "quest " + definition.id() + " " + source + " -> "
			+ target + " " + event + " routes=" + routes.size());
		return routes.getFirst();
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, QuestEvent event) {
		QuestSnapshot snapshot = snapshot(compiled, status, variables);
		return compiled.definition().transitions().stream()
			.flatMap(route -> QuestMutationPlanner.plan(compiled, snapshot, event, route).stream())
			.toList();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, compiled.definition().id(), status,
			compiled.definition().progressLayout().pack(packedVariables), Map.of());
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition compiled, QuestMutationPlan plan) {
		return compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = ArchivesRewardStepLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
