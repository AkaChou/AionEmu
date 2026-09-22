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
 * 锁定批次 36：活动“巧克力塔族 + 术古货箱族”共 13 个任务的客户端任务书三行阶梯（QE-051）。
 * <p>
 * 两族客户端都是 {@code quest_summary} 三行、可见槽位 %0/%3/%6（槽位 = 3 x 状态号），但页链不同：
 * <ul>
 *   <li>巧克力塔族（50020 Elyos / 80300+80301 Elyos 世界活动 / 80306+80307 Asmodian 世界活动，客户端
 *       {@code collect_progress=0}）：{@code select1} 的 CHECK_USER_HAS_QUEST_ITEM 交付收集物 -&gt;
 *       {@code check_user_item_ok} 的 SETPRO2 就地关闭 -&gt; 活动塔（701466 / 831402 / 831403）USE_OBJECT
 *       装饰 -&gt; 行 2 对话领奖；塔由活动系统刷出，故保留“直接找术古”的防呆入口。</li>
 *   <li>术古货箱族（50021/50022 Elyos、51021/51022 Asmodian、80302/80303 Elyos 世界活动、80308/80309
 *       Asmodian 世界活动，客户端 {@code collect_progress=4} = 零售 {@code collecting_step=4}）：
 *       {@code select4} 的 SETPRO2 结束击杀行 -&gt; 行 1 对话再确认 -&gt; 行 2 用 {@code select5} 的
 *       CHECK_USER_HAS_QUEST_ITEM 交付货箱物（货箱 701470 / 701774，drop {@code collecting-step} 收敛到收物行 2）。</li>
 * </ul>
 * 迁移把两族都塌陷成“交付直达 reward、缺行 1/2”（50021/51021 还停在越行的 STATES_BEYOND_ROWS）；本批统一重建
 * started(0)/s1(1)/reward(2)，补 var0=0/1 两条无 source ENTER_WORLD 自愈边，并且禁止再出现 started -&gt; reward 直跳。
 * <p>
 * Locks batch 36: the two quest families whose journal owns three rows while the migrated definitions
 * collapsed the hand-in into a direct reward jump and left rows 1/2 without any server state.
 */
class Batch36EventRowLadderContractTest {

	/** 塔族合同 / Chocolate-tower family contract. */
	private record TowerContract(int questId, int npc, int chocolate, int towerObject) {
	}

	/** 货箱族合同 / Shugo cargo-box family contract. */
	private record BoxContract(int questId, int npc, int loot, int boxObject) {
	}

	private static final List<TowerContract> TOWER_FAMILY = List.of(
		new TowerContract(50020, 202549, 182215173, 701466),
		new TowerContract(80300, 799763, 182215288, 831402),
		new TowerContract(80301, 799763, 182215289, 831402),
		new TowerContract(80306, 799763, 182215294, 831403),
		new TowerContract(80307, 799763, 182215295, 831403)
	);

	private static final List<BoxContract> BOX_FAMILY = List.of(
		new BoxContract(50021, 202549, 182215176, 701470),
		new BoxContract(51021, 202549, 182215182, 701470),
		new BoxContract(50022, 202549, 182215177, 701470),
		new BoxContract(51022, 202549, 182215183, 701470),
		new BoxContract(80302, 799763, 182215292, 701774),
		new BoxContract(80303, 799763, 182215293, 701774),
		new BoxContract(80308, 799763, 182215298, 701774),
		new BoxContract(80309, 799763, 182215299, 701774)
	);

	private static final List<Integer> TOWER_QUEST_IDS = TOWER_FAMILY.stream()
		.map(TowerContract::questId).toList();

	private static final List<Integer> BOX_QUEST_IDS = BOX_FAMILY.stream()
		.map(BoxContract::questId).toList();

	private static final List<Integer> ALL_QUEST_IDS = java.util.stream.Stream
		.concat(TOWER_QUEST_IDS.stream(), BOX_QUEST_IDS.stream()).toList();

	private static final int FIRST_ROW = 0;
	private static final int MIDDLE_ROW = 1;
	private static final int REWARD_ROW = 2;

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (List<Integer> family : List.of(TOWER_QUEST_IDS, BOX_QUEST_IDS)) {
			for (int questId : family) {
				QuestDefinition definition = definition(questId).definition();
				assertEquals(FIRST_ROW, node(definition, "started").projection().variables().get("var0"),
					() -> "quest " + questId + " started row");
				assertEquals(MIDDLE_ROW, node(definition, "s1").projection().variables().get("var0"),
					() -> "quest " + questId + " s1 row");
				assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
					() -> "quest " + questId + " reward row");
				assertEquals(QuestStatus.START, node(definition, "s1").projection().status(),
					() -> "quest " + questId + " s1 status");
				assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
					() -> "quest " + questId + " reward status");
				assertTrue(definition.progressLayout().field("var0").maxValue() >= REWARD_ROW,
					() -> "quest " + questId + " must widen var0 so the reward row fits");

				Set<Integer> rows = new LinkedHashSet<>();
				for (QuestNode candidate : definition.nodes()) {
					Integer row = candidate.projection().variables().get("var0");
					QuestStatus status = candidate.projection().status();
					if (row == null || row > REWARD_ROW
							|| (status != QuestStatus.START && status != QuestStatus.REWARD)) {
						continue;
					}
					rows.add(row);
					assertEquals(row == REWARD_ROW ? QuestStatus.REWARD : QuestStatus.START, status,
						() -> "quest " + questId + " row " + row + " status");
				}
				assertEquals(Set.of(FIRST_ROW, MIDDLE_ROW, REWARD_ROW), rows,
					() -> "quest " + questId + " owns a state per journal row");
				assertTrue(routes(definition, "started", "reward").isEmpty(),
					() -> "quest " + questId + " must not keep a collapsed row 0 -> reward jump");
			}
		}
	}

	@Test
	void towerFamilyHandsInThenDecoratesTheTower() throws Exception {
		for (TowerContract contract : TOWER_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			List<QuestTransition> handIn = routes(definition, "started", "s1");
			assertEquals(1, handIn.size(),
				() -> "quest " + contract.questId() + " advances row 0 -> 1 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.npc(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0), handIn.getFirst().event(),
				() -> "quest " + contract.questId() + " row 0 ends on the client hand-in button");
			assertTrue(handIn.getFirst().conditions().contains(
					new QuestCondition.HasItem(contract.chocolate(), 3)),
				() -> "quest " + contract.questId() + " gates the hand-in on the collected chocolate");
			assertTrue(handIn.getFirst().actions().contains(
					new QuestAction.RemoveItem(contract.chocolate(), 3)),
				() -> "quest " + contract.questId() + " consumes the collected chocolate");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
				handIn.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " shows the client check_ok page");

			assertEquals(List.of(new AfterCommitAction.CloseDialog()),
				routes(definition, "s1", "s1").stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
						QuestDialogAction.SETPRO2.id(), 0)))
					.findFirst().orElseThrow(() -> new AssertionError(
						"quest " + contract.questId() + " keeps the check_ok confirm button"))
					.afterCommit(),
				() -> "quest " + contract.questId() + " keeps the check_ok confirm as a no-op close");

			List<QuestTransition> decorate = routes(definition, "s1", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.towerObject(),
					QuestDialogAction.USE_OBJECT.id(), 0)))
				.toList();
			assertEquals(1, decorate.size(),
				() -> "quest " + contract.questId() + " wires the event tower exactly once");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), decorate.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " refreshes the journal after decorating");

			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				routes(definition, "s1", "reward").stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
						QuestDialogAction.QUEST_SELECT.id(), 0)))
					.findFirst().orElseThrow(() -> new AssertionError(
						"quest " + contract.questId() + " keeps a soft-lock guard when the tower is missing"))
					.afterCommit(),
				() -> "quest " + contract.questId() + " opens the reward entry page from the guard");
		}
	}

	@Test
	void boxFamilyTalksThenLootsTheCargoBox() throws Exception {
		for (BoxContract contract : BOX_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			List<QuestTransition> endKillRow = routes(definition, "started", "s1");
			assertEquals(1, endKillRow.size(),
				() -> "quest " + contract.questId() + " advances row 0 -> 1 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.npc(),
				QuestDialogAction.SETPRO2.id(), 0), endKillRow.getFirst().event(),
				() -> "quest " + contract.questId() + " ends row 0 on the client select4 button");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), endKillRow.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " refreshes the journal leaving row 0");
			assertTrue(routes(definition, "started", "started").stream()
					.anyMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
						QuestDialogAction.QUEST_SELECT.id(), 0))
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT4.id())))),
				() -> "quest " + contract.questId() + " shows the client select4 page on row 0");

			List<QuestTransition> enterCollectRow = routes(definition, "s1", "reward");
			assertEquals(1, enterCollectRow.size(),
				() -> "quest " + contract.questId() + " advances row 1 -> 2 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.npc(),
				QuestDialogAction.SETPRO2.id(), 0), enterCollectRow.getFirst().event(),
				() -> "quest " + contract.questId() + " ends the talk row on the select4 button");

			List<QuestTransition> handIn = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0)))
				.toList();
			assertEquals(2, handIn.size(),
				() -> "quest " + contract.questId() + " splits the check into success/failure routes");
			QuestTransition success = handIn.stream()
				.filter(route -> route.conditions().contains(new QuestCondition.HasItem(contract.loot(), 3)))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + contract.questId() + " has no successful hand-in route"));
			assertEquals(0, success.priority(), () -> "quest " + contract.questId() + " success priority");
			assertTrue(success.actions().contains(new QuestAction.RemoveItem(contract.loot(), 3)),
				() -> "quest " + contract.questId() + " consumes the cargo-box loot");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())), success.afterCommit(),
				() -> "quest " + contract.questId() + " opens the client select_success page after the hand-in");
			assertTrue(routes(definition, "reward", "reward").stream()
					.anyMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
						QuestDialogAction.SELECT_QUEST_REWARD.id(), 0))),
				() -> "quest " + contract.questId()
					+ " keeps the 1009 reward-window claim on the select_success page");
			assertTrue(routes(definition, "reward", "reward").stream()
					.anyMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.npc(),
						QuestDialogAction.QUEST_SELECT.id(), 0))
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT5.id())))),
				() -> "quest " + contract.questId() + " shows the client select5 page on the loot row");
			assertTrue(routes(definition, "s1", "s1").stream()
					.anyMatch(route -> route.event().equals(new QuestEvent.CanAct(contract.boxObject(),
						"ACTION_ITEM_USE"))),
				() -> "quest " + contract.questId() + " keeps the cargo box usable on the loot row");
			assertTrue(definition.metadata().drops().stream()
					.anyMatch(drop -> drop.npcId() == contract.boxObject()
						&& drop.itemId() == contract.loot()
						&& drop.collectingStep() == MIDDLE_ROW),
				() -> "quest " + contract.questId()
					+ " gates the cargo-box drop on the START loot row (collecting-step=1)");
		}
	}

	@Test
	void rewardRoutesSeatOnTheJournalNpc() throws Exception {
		for (int questId : ALL_QUEST_IDS) {
			QuestDefinition definition = definition(questId).definition();
			int npc = journalNpc(questId);

			Set<Integer> completionNpcs = new LinkedHashSet<>();
			definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.forEach(route -> {
					if (route.event() instanceof QuestEvent.TalkToNpc talk) {
						completionNpcs.add(talk.npcId());
					}
				});
			assertEquals(Set.of(npc), completionNpcs,
				() -> "quest " + questId + " completes on the journal reward-row NPC");
			assertTrue(routes(definition, "reward", "complete").size() >= 1,
				() -> "quest " + questId + " keeps a reward -> complete route");
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> "complete".equals(route.targetNode())
						&& route.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() != npc),
				() -> "quest " + questId + " must not let a non-journal NPC claim the quest");
		}
	}

	@Test
	void staleCollapsedRewardSavesHealToTheRewardRow() throws Exception {
		for (int questId : ALL_QUEST_IDS) {
			CompiledQuestDefinition compiled = definition(questId);
			List<QuestTransition> recoveries = enterWorldRecoveries(compiled.definition());
			for (int staleRow : List.of(FIRST_ROW, MIDDLE_ROW)) {
				List<QuestTransition> matches = recoveries.stream()
					.filter(route -> route.conditions().equals(List.of(
						new QuestCondition.StatusIs(QuestStatus.REWARD),
						new QuestCondition.QuestVariableIs("var0", staleRow))))
					.toList();
				assertEquals(1, matches.size(),
					() -> "quest " + questId + " heal route for stale row " + staleRow);
				QuestTransition heal = matches.getFirst();
				assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
					() -> "quest " + questId + " heal writes the reward row");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
					() -> "quest " + questId + " heal refreshes the journal");
				assertNull(heal.priority(), () -> "quest " + questId + " heal priority");

				Map<String, Integer> stale = new LinkedHashMap<>(
					compiled.definition().progressLayout().unpack(0));
				stale.put("var0", staleRow);
				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(compiled, stale), heal)
					.orElseThrow(() -> new AssertionError(
						"quest " + questId + " heal plan for stale row " + staleRow));
				assertEquals(QuestStatus.REWARD, plan.nextStatus(),
					() -> "quest " + questId + " healed status for row " + staleRow);
				assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
					() -> "quest " + questId + " healed row for " + staleRow);
			}
		}
	}

	private static int journalNpc(int questId) {
		for (TowerContract contract : TOWER_FAMILY) {
			if (contract.questId() == questId) {
				return contract.npc();
			}
		}
		for (BoxContract contract : BOX_FAMILY) {
			if (contract.questId() == questId) {
				return contract.npc();
			}
		}
		throw new AssertionError("unknown quest " + questId);
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
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(variables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch36EventRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
