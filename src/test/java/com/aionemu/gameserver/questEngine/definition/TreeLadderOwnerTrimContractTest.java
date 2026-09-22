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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 20：焦树族 23809（天族）/ 13809（魔族）的三树叶阶梯与领奖 owner 收敛。
 * <p>
 * 族级判据：两侧客户端 {@code quest_q23809.html} / {@code quest_q13809.html} 的 {@code quest_summary} 都是 4 行
 * （行 0/1/2 = 依次调查 DeadTree_a/b/c 并采集 {@code quest_<id>a/b/c}，行 3 = 向
 * {@code STR_DIC_N_LDF5_Fortress_Village_Guard01_D|L} 报告）；迁移前的 legacy handler（{@code 7e9f0316c^}）两侧逐行同形，
 * 三棵树只做 {@code useQuestObject(env, 0,1 / 1,2 / 2,3, false, 0)} 的行推进，接取与领奖都只落在守卫身上。
 * 但迁移后 23809 被塌陷成“三棵树各一条 started -> reward 直跳 + 整包交付”（行 1/行 2 没有状态），
 * 13809 阶梯正确却让三棵树同时兼任 {@code NPC_START} / {@code npc-complete}（QE-052 冗余 owner），
 * 且两侧都没有按客户端页动作（SETPRO1/2/3）与行号发放采集物。
 * <p>
 * Locks batch 20: the four-row tree ladder of the mirror pair 23809/13809 together with the owner trim. Both sides
 * must keep one state per client journal row, each tree must open its own client page and advance exactly one row
 * while granting that row's collected item, the offer and the completion must stay on the journal NPC, and stale
 * saves collapsed to REWARD row 0 must heal to the report row.
 */
class TreeLadderOwnerTrimContractTest {

	/** 任务 / 同形镜像 / 领奖 NPC / 三行采集物 / 客户端行 3 的行内 NPC 字典键。 */
	private record Contract(int questId, int mirrorId, int reportNpc, List<Integer> items, String reportRowNpcKey) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(23809, 13809, 802429, List.of(182215493, 182215494, 182215495),
			"LDF5_Fortress_Village_Guard01_D"),
		new Contract(13809, 23809, 802427, List.of(182215485, 182215486, 182215487),
			"LDF5_Fortress_Village_Guard01_L"));

	/** 三棵世界物件（天/魔共用）与它们各自的行 / 页 / 按钮。 / The three shared DeadTree objects with their row, page and button. */
	private static final List<Integer> TREES = List.of(730969, 730970, 730971);
	private static final List<String> ROW_NODES = List.of("started", "stage1", "stage2");
	private static final List<String> NEXT_NODES = List.of("stage1", "stage2", "reward");
	private static final List<QuestDialogAction> ROW_BUTTONS = List.of(
		QuestDialogAction.SETPRO1, QuestDialogAction.SETPRO2, QuestDialogAction.SETPRO3);
	private static final List<QuestDialogPage> ROW_PAGES = List.of(
		QuestDialogPage.SELECT2, QuestDialogPage.SELECT3, QuestDialogPage.SELECT4);

	private static final int COLLAPSED_ROW = 0;
	private static final int REWARD_ROW = 3;

	@Test
	void everyClientJournalRowOwnsAStateAndMatchesTheMirror() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode node : definition.nodes()) {
				Integer row = node.projection().variables().get("var0");
				QuestStatus status = node.projection().status();
				if (row == null || row > REWARD_ROW
						|| (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
				assertEquals(row == REWARD_ROW ? QuestStatus.REWARD : QuestStatus.START, status,
					() -> "quest " + contract.questId() + " row " + row + " node status");
			}
			assertEquals(Set.of(0, 1, 2, REWARD_ROW), rows,
				() -> "quest " + contract.questId() + " must own a state per client journal row");
			assertEquals(REWARD_ROW,
				node(definition, "reward").projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(
				node(definition(contract.mirrorId()).definition(), "reward").projection().variables().get("var0"),
				node(definition, "reward").projection().variables().get("var0"),
				() -> "mirror pair " + contract.questId() + "/" + contract.mirrorId() + " reward row");
			/* 领奖行必须落在声明的 var0 位域内 / the reward row must survive its declared var0 bit field */
			assertEquals(Map.of("var0", REWARD_ROW),
				definition.progressLayout().unpack(
					definition.progressLayout().pack(Map.of("var0", REWARD_ROW))),
				() -> "quest " + contract.questId() + " reward row fits its bit field");
		}
	}

	@Test
	void eachTreeOpensItsOwnClientPageAndAdvancesExactlyOneRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			for (int index = 0; index < TREES.size(); index++) {
				int rowIndex = index;
				int tree = TREES.get(index);
				int item = contract.items().get(index);
				String row = ROW_NODES.get(index);
				String next = NEXT_NODES.get(index);
				QuestDialogPage page = ROW_PAGES.get(index);
				int pageId = page.id();
				int button = ROW_BUTTONS.get(index).id();

				List<QuestTransition> pageRoutes = routes(definition, row, row).stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(tree, QuestDialogAction.USE_OBJECT.id())))
					.toList();
				assertEquals(1, pageRoutes.size(),
					() -> "quest " + contract.questId() + " tree " + tree + " opens exactly one page on row " + row);
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), pageRoutes.getFirst().afterCommit(),
					() -> "quest " + contract.questId() + " tree " + tree + " must open the client page "
						+ page);

				List<QuestTransition> ladderSteps = routes(definition, row, next);
				assertEquals(1, ladderSteps.size(),
					() -> "quest " + contract.questId() + " row " + row + " keeps a single ladder step");
				List<QuestTransition> advances = ladderSteps.stream()
					.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(tree, button)))
					.toList();
				assertEquals(1, advances.size(),
					() -> "quest " + contract.questId() + " tree " + tree + " advances exactly one row via the client button "
						+ ROW_BUTTONS.get(rowIndex));
				QuestTransition advance = advances.getFirst();
				assertTrue(advance.actions().contains(new QuestAction.GiveItem(item, 1)),
					() -> "quest " + contract.questId() + " row " + rowIndex + " grants its collected item " + item);
				assertTrue(definition.metadata().questWorkItems().contains(new QuestItemRequirement(item, 1)),
					() -> "quest " + contract.questId() + " declares item " + item
						+ " as a quest work item (quest_data.xml authority)");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(index < TREES.size() - 1
						? QuestStateSyncMode.PACKET_ONLY : QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
					advance.afterCommit().stream().filter(AfterCommitAction.SyncQuestState.class::isInstance).toList(),
					() -> "quest " + contract.questId() + " row " + rowIndex
						+ " syncs like legacy sendUpdatePacket (START rows are packet-only, REWARD also refreshes)");
			}
		}
	}

	@Test
	void treeInteractionWalksTheLadderInOrder() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			for (int index = 0; index < TREES.size(); index++) {
				int rowIndex = index;
				int tree = TREES.get(index);
				int item = contract.items().get(index);
				QuestEvent event = new QuestEvent.TalkToNpc(tree, ROW_BUTTONS.get(index).id());
				List<QuestMutationPlan> plans = plans(compiled, QuestStatus.START, Map.of("var0", rowIndex), event);
				assertEquals(1, plans.size(), () -> "quest " + contract.questId() + " tree " + tree
					+ " must answer on journal row " + rowIndex + " with exactly one route");
				QuestMutationPlan plan = plans.getFirst();
				QuestStatus expectedStatus = rowIndex == TREES.size() - 1 ? QuestStatus.REWARD : QuestStatus.START;
				assertEquals(expectedStatus, plan.nextStatus(),
					() -> "quest " + contract.questId() + " tree " + tree + " target status");
				assertEquals(rowIndex + 1, unpack(compiled, plan).get("var0"),
					() -> "quest " + contract.questId() + " tree " + tree + " must land on row " + (rowIndex + 1));
				assertTrue(plan.requiredActions().stream().anyMatch(action ->
						action.equals(new QuestAction.GiveItem(item, 1))),
					() -> "quest " + contract.questId() + " tree " + tree + " grants its own row item " + item);

				/* 顺序门禁：同一棵树只在它自己的行响应，跳行或回看不产生任何计划。 */
				/* Order gate: a tree only answers on its own row; skipping or rewinding a row yields no plan. */
				for (int otherRow = 0; otherRow <= TREES.size(); otherRow++) {
					if (otherRow == rowIndex) {
						continue;
					}
					int probeRow = otherRow;
					assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", probeRow), event).isEmpty(),
						() -> "quest " + contract.questId() + " tree " + tree + " must not answer on row " + probeRow);
				}
			}
		}
	}

	@Test
	void offerAndCompletionStayOnTheJournalNpc() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			Set<Integer> offerNpcs = new LinkedHashSet<>();
			definition.transitions().stream()
				.filter(route -> "unaccepted".equals(route.sourceNode()))
				.filter(route -> "started".equals(route.targetNode()))
				.forEach(route -> {
					if (route.event() instanceof QuestEvent.TalkToNpc talk) {
						offerNpcs.add(talk.npcId());
					}
				});
			assertEquals(Set.of(contract.reportNpc()), offerNpcs,
				() -> "quest " + contract.questId() + " offer must stay on the journal NPC");

			Set<Integer> completionNpcs = new LinkedHashSet<>();
			definition.transitions().stream()
				.filter(route -> "complete".equals(route.targetNode()))
				.forEach(route -> {
					if (route.event() instanceof QuestEvent.TalkToNpc talk) {
						completionNpcs.add(talk.npcId());
					}
				});
			assertEquals(Set.of(contract.reportNpc()), completionNpcs,
				() -> "quest " + contract.questId() + " completion owner must be the journal NPC "
					+ contract.reportNpc());

			Set<Integer> talkNpcs = talkNpcIds(definition);
			Set<Integer> expected = new LinkedHashSet<>(TREES);
			expected.add(contract.reportNpc());
			assertEquals(expected, talkNpcs,
				() -> "quest " + contract.questId() + " talk routes cover only the three trees and the journal NPC");
			for (int tree : TREES) {
				assertFalse(completionNpcs.contains(tree),
					() -> "quest " + contract.questId() + " tree " + tree + " must not complete the quest");
			}
		}
	}

	@Test
	void reportRowKeepsTheClientSelect5PageAndClearsTheWorkItems() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestDefinition definition = compiled.definition();
			List<QuestTransition> reportRoutes = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(
					new QuestEvent.TalkToNpc(contract.reportNpc(), QuestDialogAction.QUEST_SELECT.id())))
				.toList();
			assertEquals(1, reportRoutes.size(),
				() -> "quest " + contract.questId() + " report row opens the client select5 page once");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
				reportRoutes.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " report row must show the client select5 page");

			/* 领奖窗口与 1009 只由 npc-complete 的 preview 承接：再显式声明 SELECT_QUEST_REWARD 自环会
			   触发 AMBIGUOUS_TRANSITION（批次 19 教训），因此该动作在 reward 行必须只有 preview 一条路由。 */
			/* The reward window and action 1009 stay on the npc-complete preview only; a second explicit
			   SELECT_QUEST_REWARD self-loop is ambiguous (batch 19 lesson). */
			List<QuestTransition> rewardWindowRoutes = routes(definition, "reward", "reward").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(contract.reportNpc(),
					QuestDialogAction.SELECT_QUEST_REWARD.id())))
				.toList();
			assertEquals(1, rewardWindowRoutes.size(),
				() -> "quest " + contract.questId() + " keeps exactly the npc-complete preview SELECT_QUEST_REWARD route");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardWindowRoutes.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " preview opens the quest reward window");

			Map<Integer, Integer> inventory = new LinkedHashMap<>();
			contract.items().forEach(item -> inventory.put(item, 1));
			QuestEvent completion = new QuestEvent.TalkToNpc(contract.reportNpc(),
				QuestDialogAction.SELECTED_QUEST_REWARD1.id());
			List<QuestMutationPlan> plans = plans(compiled, QuestStatus.REWARD, Map.of("var0", REWARD_ROW),
				inventory, completion);
			assertEquals(1, plans.size(), () -> "quest " + contract.questId()
				+ " reward window selection must complete the quest through exactly one route");
			QuestMutationPlan plan = plans.getFirst();
			assertEquals(QuestStatus.COMPLETE, plan.nextStatus(),
				() -> "quest " + contract.questId() + " reward selection status");
			for (int item : contract.items()) {
				assertTrue(plan.requiredActions().contains(
						new QuestAction.RemoveItem(item, QuestAction.RemoveItem.ALL)),
					() -> "quest " + contract.questId() + " completion must clear work item " + item);
			}
		}
	}

	@Test
	void collapsedTreesNoLongerJumpStraightToReward() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep a started -> reward jump");
			assertTrue(routes(definition, "stage1", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep a stage1 -> reward jump");
			for (QuestTransition route : definition.transitions()) {
				if (!"reward".equals(route.targetNode())) {
					continue;
				}
				String source = route.sourceNode();
				assertTrue(source == null || "stage2".equals(source) || "reward".equals(source),
					() -> "quest " + contract.questId()
						+ " reward is entered from row 2 (or the reward row itself) only, found " + source);
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value) && "var0".equals(field)) {
						assertEquals(REWARD_ROW, value, () -> "quest " + contract.questId()
							+ " reward route writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void staleCollapsedSavesHealToTheReportRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", COLLAPSED_ROW))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + contract.questId() + " heal route for the collapsed row " + COLLAPSED_ROW);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
				() -> "quest " + contract.questId() + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + contract.questId() + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");
			assertEquals(1, enterWorldRecoveries(compiled.definition()).size(),
				() -> "quest " + contract.questId() + " keeps exactly one recovery route");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", COLLAPSED_ROW), Map.of()), heal)
				.orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " healed status");
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " healed journal row");
		}
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, QuestEvent event) {
		return plans(compiled, status, variables, Map.of(), event);
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory, QuestEvent event) {
		QuestSnapshot snapshot = snapshot(compiled, status, variables, inventory);
		return compiled.definition().transitions().stream()
			.flatMap(route -> QuestMutationPlanner.plan(compiled, snapshot, event, route).stream())
			.toList();
	}

	private static Set<Integer> talkNpcIds(QuestDefinition definition) {
		Set<Integer> npcIds = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
				npcIds.add(talk.npcId());
			}
		}
		return npcIds;
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

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = TreeLadderOwnerTrimContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
