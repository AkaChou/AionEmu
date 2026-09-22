package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 19：单步塌陷对（Elyos 存根）的客户端任务书行阶梯。
 * <p>
 * 族级判据：同形镜像对（q / q±10000）客户端 quest_summary 行数相同、末行 NPC 都能在任务内对上，镜像那一侧已有完整的
 * {@code 0..N-1} 状态阶梯，落后那一侧却被迁移塌陷成“单步交接直接置 REWARD”。这类任务不能只改 reward 投影（会指向
 * 服务端不存在的行），必须按客户端对话页 {@code <Act href="HACTION_*">} 的动作序列把中间行补回来。
 * <p>
 * 15000（镜像 25000）：交回背囊 → 和米利亚德对话（SETPRO2 接过工作物品 182215662）→ 使用修理工具 → 向米利亚德报告。
 * 15670（镜像 25670）：与赫梅洛斯（806093）对话 → 调查 4 处痕迹后交出证据 → 调查第五处痕迹（731793）→ 向伊利西亚（806114）报告。
 * <p>
 * Locks batch 19: Elyos shards whose migration collapsed the multi-row client journal into a single hand-over that
 * jumped straight to REWARD; the ladder is restored from the client dialog pages and the aligned mirror.
 */
class CollapsedSingleStepLadderContractTest {

	/** 任务 / 已对齐镜像 / 领奖行。 / Collapsed shard, aligned mirror, reward journal row. */
	private record Contract(int questId, int mirrorId, int rewardRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(15000, 25000, 3),
		new Contract(15670, 25670, 3)
	);

	private static final int STALE_ROW = 0;

	/** 15000：交回背囊的领奖 NPC 与领取工作物品的对话 NPC 都是米利亚德。 */
	/* 15000: Milliard hands back the bag and later provides the repair tool. */
	private static final int MILLIARD = 804874;
	private static final int REPAIR_TOOL = 182215662;
	private static final int BAG = 182215661;

	/** 15670：行 0/行 1 的赫梅洛斯、第五处痕迹对象与行 3 的伊利西亚。 */
	/* 15670: Hemellos (rows 0/1), the fifth trace object (row 2) and Ilisia (row 3). */
	private static final int HEMELLOS = 806093;
	private static final int FIFTH_TRACE = 731793;
	private static final int ILISIA = 806114;
	private static final List<Integer> EVIDENCE = List.of(182216189, 182216190, 182216191, 182216192);
	private static final List<Integer> TRACES = List.of(703434, 703435, 703436, 703437);

	@Test
	void collapsedShardsProjectTheAlignedRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(contract.rewardRow(),
				node(definition(contract.questId()).definition(), "reward").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(QuestStatus.REWARD,
				node(definition(contract.questId()).definition(), "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(
				node(definition(contract.mirrorId()).definition(), "reward").projection().variables().get("var0"),
				node(definition(contract.questId()).definition(), "reward").projection().variables().get("var0"),
				() -> "mirror pair " + contract.questId() + "/" + contract.mirrorId() + " reward row");
		}
	}

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode node : definition.nodes()) {
				Integer row = node.projection().variables().get("var0");
				QuestStatus status = node.projection().status();
				if (row == null || row > contract.rewardRow()
						|| (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
				QuestStatus expected = row == contract.rewardRow()
					? QuestStatus.REWARD : QuestStatus.START;
				assertEquals(expected, node.projection().status(),
					() -> "quest " + contract.questId() + " row " + row + " node status");
			}
			assertEquals(Set.of(0, 1, 2, contract.rewardRow()), rows,
				() -> "quest " + contract.questId() + " must own a state per client journal row");
		}
	}

	@Test
	void milliardShardWalksTheClientActionLadder() throws Exception {
		QuestDefinition definition = definition(15000).definition();
		assertEquals(1, routes(definition, "started", "s1").size(),
			() -> "15000 hands the bag over into journal row 1");
		assertEquals(new QuestEvent.TalkToNpc(MILLIARD, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0),
			routes(definition, "started", "s1").getFirst().event(),
			() -> "15000 row 0 uses the client CHECK_USER_HAS_QUEST_ITEM page");
		assertTrue(conditions(routes(definition, "started", "s1").getFirst()).stream()
				.anyMatch(condition -> condition instanceof QuestCondition.HasItem hasItem
					&& hasItem.itemId() == BAG),
			() -> "15000 row 0 requires the recovered bag " + BAG);

		QuestTransition takeTool = routes(definition, "s1", "s2").getFirst();
		assertEquals(new QuestEvent.TalkToNpc(MILLIARD, QuestDialogAction.SETPRO2.id(), 0), takeTool.event(),
			() -> "15000 row 1 ends with the client SETPRO2 page");
		assertTrue(takeTool.actions().contains(new QuestAction.GiveItem(REPAIR_TOOL, 1)),
			() -> "15000 row 1 hands over the repair tool " + REPAIR_TOOL);

		/* 客户端页按钮链：check_user_item_ok 的 SELECT2(1352) -> select2 页，select2 的 SELECT2_1(1353) -> select2_1 页。
		   没有这两条路由时 QuestClientContractGateTest 会报 BUTTON_WITHOUT_ROUTE。 */
		/* Client button chain on row 1: SELECT2(1352) -> select2 page, SELECT2_1(1353) -> select2_1 page. */
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			routes(definition, "s1", "s1").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					MILLIARD, QuestDialogAction.SELECT2.id(), 0)))
				.findFirst().orElseThrow().afterCommit(),
			() -> "15000 page 1352 opens the client select2 page");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			routes(definition, "s1", "s1").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					MILLIARD, QuestDialogAction.SELECT2_1.id(), 0)))
				.findFirst().orElseThrow().afterCommit(),
			() -> "15000 page 1353 opens the client select2_1 page");

		assertEquals(1, routes(definition, "s2", "reward").size(),
			() -> "15000 row 2 advances into the reward row exactly once");
		assertEquals(new QuestEvent.UseItem(REPAIR_TOOL),
			routes(definition, "s2", "reward").getFirst().event(),
			() -> "15000 row 2 is the client 'use the repair tool near the generator' step");
		assertTrue(definition.metadata().questWorkItems().contains(new QuestItemRequirement(REPAIR_TOOL, 1)),
			() -> "15000 declares the repair tool as a quest work item");
	}

	@Test
	void hemellosShardWalksTheClientActionLadder() throws Exception {
		QuestDefinition definition = definition(15670).definition();
		assertEquals(new QuestEvent.TalkToNpc(HEMELLOS, QuestDialogAction.SETPRO1.id(), 0),
			routes(definition, "started", "s1").getFirst().event(),
			() -> "15670 row 0 ends on the client SETPRO1 page at Hemellos");

		QuestTransition handover = routes(definition, "s1", "s2").getFirst();
		assertEquals(new QuestEvent.TalkToNpc(HEMELLOS, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0),
			handover.event(), () -> "15670 row 1 hands the evidence to Hemellos");
		List<Integer> removed = handover.actions().stream()
			.filter(action -> action instanceof QuestAction.RemoveItem)
			.map(action -> ((QuestAction.RemoveItem) action).itemId())
			.toList();
		assertEquals(EVIDENCE, removed, () -> "15670 row 1 consumes the four evidence items");

		assertEquals(new QuestEvent.TalkToNpc(FIFTH_TRACE, QuestDialogAction.SET_SUCCEED.id(), 0),
			routes(definition, "s2", "reward").getFirst().event(),
			() -> "15670 row 2 is the client SET_SUCCEED step on the fifth trace object");

		Set<Integer> completionNpcs = new LinkedHashSet<>();
		definition.transitions().stream()
			.filter(route -> "complete".equals(route.targetNode()))
			.forEach(route -> {
				if (route.event() instanceof QuestEvent.TalkToNpc talk) {
					completionNpcs.add(talk.npcId());
				}
			});
		assertEquals(Set.of(ILISIA), completionNpcs,
			() -> "15670 completion owner stays on the reward-row NPC Ilisia");
		assertTrue(routes(definition, "reward", "complete").size() >= 1,
			() -> "15670 keeps a reward -> complete route");
	}

	@Test
	void ilumaEvidenceCollectionMovesToTheHandoverRow() throws Exception {
		QuestDefinition definition = definition(15670).definition();
		for (int templateId : TRACES) {
			assertTrue(routes(definition, "s1", "s1").stream()
					.anyMatch(route -> route.event()
						.equals(new QuestEvent.CanAct(templateId, "ACTION_ITEM_USE"))),
				() -> "15670 trace " + templateId + " is usable on journal row 1");
			assertTrue(routes(definition, "started", "started").stream()
					.noneMatch(route -> route.event()
						.equals(new QuestEvent.CanAct(templateId, "ACTION_ITEM_USE"))),
				() -> "15670 trace " + templateId + " must no longer be usable on journal row 0");
		}
		for (QuestDrop drop : definition.metadata().drops()) {
			if (TRACES.contains(drop.npcId())) {
				assertEquals(1, drop.collectingStep(),
					() -> "15670 trace drop " + drop.npcId() + " belongs to journal row 1");
			}
		}
	}

	@Test
	void staleCollapsedRewardSavesHealToTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", STALE_ROW))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + contract.questId() + " heal route for the collapsed row " + STALE_ROW);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())), heal.actions(),
				() -> "quest " + contract.questId() + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + contract.questId() + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", STALE_ROW)), heal).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " healed status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " healed journal row");
		}
	}

	@Test
	void collapsedHandoverNoLongerJumpsStraightToReward() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep the collapsed started -> reward jump");
			for (QuestTransition route : definition.transitions()) {
				if (!"reward".equals(route.targetNode())) {
					continue;
				}
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " reward route writes a non reward journal row");
					}
				}
			}
		}
	}

	private static List<QuestCondition> conditions(QuestTransition transition) {
		return transition.conditions();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static List<QuestTransition> enterWorldRecoveries(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = CollapsedSingleStepLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
