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
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 48：11012（Practical Nursing）因吉森三名患者四行阶梯与领奖 owner 收敛。
 * <p>
 * 判据：客户端 {@code quest_q11012.html} 的 {@code quest_summary} 共 4 行——行 0/1/2 = 依次治疗
 * LF4_patient_1/2/3（799072/799073/799074），行 3 = 和 Naiting（799071）对话；客户端页
 * select1(1011)/select2(1352)/select3(1693) 的按钮分别是 HACTION_SETPRO1/2/3(10000/10001/10002)，
 * 领奖页 select_success(10002) 的按钮是 HACTION_SELECT_QUEST_REWARD(1009)。迁移前 handler
 * {@code _11012PracticalNursing}（7e9f0316c^）在 799072(var0==0)/799073(var0==1)/799074(var0==2)
 * 上各消耗 1 个绷带 182206715 并把 var0 推一格，799071 是接取与领奖 owner；typed 迁移把四名 NPC
 * 都写成 {@code NPC_REPORT -> reward} 直跳，行 1/2/3 没有状态（审计 MISSING_TAIL_ROWS）。
 * <p>
 * Locks batch 48: the four-row Inggison nursing ladder for 11012. Each patient must open its own client
 * page, advance exactly one row and consume one bandage; the offer and the completion stay on Naiting, the
 * reward row keeps the client select_success(10002) entry page, and stale collapsed saves heal to row 3.
 */
class InggisonNursingRowLadderContractTest {

	private static final int QUEST = 11012;
	private static final int NAITING = 799071;
	private static final List<Integer> PATIENTS = List.of(799072, 799073, 799074);
	private static final List<String> ROW_NODES = List.of("started", "stage1", "stage2");
	private static final List<String> NEXT_NODES = List.of("stage1", "stage2", "reward");
	private static final List<QuestDialogPage> ROW_PAGES = List.of(
		QuestDialogPage.SELECT1, QuestDialogPage.SELECT2, QuestDialogPage.SELECT3);
	private static final List<QuestDialogAction> ROW_BUTTONS = List.of(
		QuestDialogAction.SETPRO1, QuestDialogAction.SETPRO2, QuestDialogAction.SETPRO3);
	private static final int BANDAGE = 182206715;
	private static final int BANDAGE_COUNT = 3;
	private static final int REWARD_ROW = 3;

	@Test
	void fourClientJournalRowsOwnAState() throws Exception {
		QuestDefinition definition = definition().definition();
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
				() -> "quest " + QUEST + " row " + row + " node status");
		}
		assertEquals(Set.of(0, 1, 2, REWARD_ROW), rows,
			() -> "quest " + QUEST + " must own a state per client journal row");
		assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
			() -> "quest " + QUEST + " reward journal row");
		assertEquals(Map.of("var0", REWARD_ROW),
			definition.progressLayout().unpack(
				definition.progressLayout().pack(Map.of("var0", REWARD_ROW))),
			() -> "quest " + QUEST + " reward row fits its declared var0 bit field");
	}

	@Test
	void eachPatientOpensItsOwnClientPageAndAdvancesExactlyOneRow() throws Exception {
		QuestDefinition definition = definition().definition();
		for (int index = 0; index < PATIENTS.size(); index++) {
			int rowIndex = index;
			int patient = PATIENTS.get(index);
			String row = ROW_NODES.get(index);
			String next = NEXT_NODES.get(index);
			QuestDialogPage page = ROW_PAGES.get(index);

			List<QuestTransition> pageRoutes = routes(definition, row, row).stream()
				.filter(route -> route.event().equals(
					new QuestEvent.TalkToNpc(patient, QuestDialogAction.QUEST_SELECT.id())))
				.toList();
			assertEquals(1, pageRoutes.size(),
				() -> "quest " + QUEST + " patient " + patient + " opens exactly one page on row " + row);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
				pageRoutes.getFirst().afterCommit(),
				() -> "quest " + QUEST + " patient " + patient + " must open the client page " + page);

			List<QuestTransition> advances = routes(definition, row, next).stream()
				.filter(route -> route.event().equals(
					new QuestEvent.TalkToNpc(patient, ROW_BUTTONS.get(rowIndex).id())))
				.toList();
			assertEquals(1, advances.size(),
				() -> "quest " + QUEST + " patient " + patient + " advances exactly one row via "
					+ ROW_BUTTONS.get(rowIndex));
			QuestTransition advance = advances.getFirst();
			assertTrue(advance.actions().contains(new QuestAction.RemoveItem(BANDAGE, 1)),
				() -> "quest " + QUEST + " row " + rowIndex + " consumes one bandage " + BANDAGE);
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(rowIndex < PATIENTS.size() - 1
					? QuestStateSyncMode.PACKET_ONLY : QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				advance.afterCommit().stream().filter(AfterCommitAction.SyncQuestState.class::isInstance).toList(),
				() -> "quest " + QUEST + " row " + rowIndex
					+ " syncs like legacy updateQuestStatus (START rows are packet-only, REWARD also refreshes)");
		}
	}

	@Test
	void nursingInteractionWalksTheLadderInOrder() throws Exception {
		CompiledQuestDefinition compiled = definition();
		for (int index = 0; index < PATIENTS.size(); index++) {
			int rowIndex = index;
			int patient = PATIENTS.get(index);
			QuestEvent event = new QuestEvent.TalkToNpc(patient, ROW_BUTTONS.get(index).id());
			List<QuestMutationPlan> plans = plans(compiled, QuestStatus.START, Map.of("var0", rowIndex),
				Map.of(BANDAGE, 1), event);
			assertEquals(1, plans.size(), () -> "quest " + QUEST + " patient " + patient
				+ " must answer on journal row " + rowIndex + " with exactly one route");
			QuestMutationPlan plan = plans.getFirst();
			QuestStatus expectedStatus = rowIndex == PATIENTS.size() - 1
				? QuestStatus.REWARD : QuestStatus.START;
			assertEquals(expectedStatus, plan.nextStatus(),
				() -> "quest " + QUEST + " patient " + patient + " target status");
			assertEquals(rowIndex + 1, unpack(compiled, plan).get("var0"),
				() -> "quest " + QUEST + " patient " + patient + " must land on row " + (rowIndex + 1));
			assertTrue(plan.requiredActions().contains(new QuestAction.RemoveItem(BANDAGE, 1)),
				() -> "quest " + QUEST + " patient " + patient + " consumes its bandage");

			/* 顺序门禁：患者只在它自己的行响应，跳行或回看不产生任何计划。 */
			/* Order gate: a patient only answers on its own row; skipping or rewinding yields no plan. */
			for (int otherRow = 0; otherRow < PATIENTS.size(); otherRow++) {
				if (otherRow == rowIndex) {
					continue;
				}
				int probeRow = otherRow;
				assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", probeRow),
						Map.of(BANDAGE, 1), event).isEmpty(),
					() -> "quest " + QUEST + " patient " + patient + " must not answer on row " + probeRow);
			}
		}
	}

	@Test
	void acceptGrantsTheThreeBandagesDeclaredAsWorkItems() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new QuestItemRequirement(BANDAGE, BANDAGE_COUNT)),
			definition.metadata().questWorkItems(),
			() -> "quest " + QUEST + " declares the three bandages as quest work items");
		for (QuestDialogAction acceptAction : List.of(
			QuestDialogAction.QUEST_ACCEPT_1, QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			QuestTransition accept = definition.transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "unaccepted")
					&& "started".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == NAITING
					&& Integer.valueOf(acceptAction.id()).equals(talk.dialogId()))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + QUEST + " 缺少 NPC " + NAITING + " 的接取动作 " + acceptAction));
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions(),
				() -> "quest " + QUEST + " accept conditions");
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(BANDAGE, BANDAGE_COUNT)),
				() -> "quest " + QUEST + " 接取时必须发放 " + BANDAGE_COUNT + " 个绷带 " + BANDAGE
					+ "（legacy sendQuestStartDialog(env, " + BANDAGE + ", " + BANDAGE_COUNT + ")）");
		}
	}

	@Test
	void offerAndCompletionStayOnNaiting() throws Exception {
		QuestDefinition definition = definition().definition();
		Set<Integer> offerNpcs = new LinkedHashSet<>();
		definition.transitions().stream()
			.filter(route -> "unaccepted".equals(route.sourceNode()))
			.filter(route -> "started".equals(route.targetNode()))
			.forEach(route -> {
				if (route.event() instanceof QuestEvent.TalkToNpc talk) {
					offerNpcs.add(talk.npcId());
				}
			});
		assertEquals(Set.of(NAITING), offerNpcs,
			() -> "quest " + QUEST + " offer must stay on the journal NPC");

		Set<Integer> completionNpcs = new LinkedHashSet<>();
		definition.transitions().stream()
			.filter(route -> "complete".equals(route.targetNode()))
			.forEach(route -> {
				if (route.event() instanceof QuestEvent.TalkToNpc talk) {
					completionNpcs.add(talk.npcId());
				}
			});
		assertEquals(Set.of(NAITING), completionNpcs,
			() -> "quest " + QUEST + " completion owner must be the journal NPC " + NAITING);

		Set<Integer> talkNpcs = talkNpcIds(definition);
		Set<Integer> expected = new LinkedHashSet<>(PATIENTS);
		expected.add(NAITING);
		assertEquals(expected, talkNpcs,
			() -> "quest " + QUEST + " talk routes cover only the three patients and Naiting");
		for (int patient : PATIENTS) {
			assertTrue(!completionNpcs.contains(patient),
				() -> "quest " + QUEST + " patient " + patient + " must not complete the quest");
		}
	}

	@Test
	void rewardRowKeepsTheClientSuccessPageAndASingleRewardWindowRoute() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> entryRoutes = routes(definition, "reward", "reward").stream()
			.filter(route -> route.event().equals(
				new QuestEvent.TalkToNpc(NAITING, QuestDialogAction.USE_OBJECT.id())))
			.toList();
		assertEquals(1, entryRoutes.size(),
			() -> "quest " + QUEST + " reward row opens the client select_success page once");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			entryRoutes.getFirst().afterCommit(),
			() -> "quest " + QUEST + " reward row must show the client select_success(10002) page");

		/* 领奖窗口与 1009 只由 npc-complete 的 preview 承接：再显式声明 SELECT_QUEST_REWARD 自环会
		   触发 AMBIGUOUS_TRANSITION（批次 19 教训）。 */
		/* The reward window and action 1009 stay on the npc-complete preview only; a second explicit
		   SELECT_QUEST_REWARD self-loop is ambiguous (batch 19 lesson). */
		List<QuestTransition> rewardWindowRoutes = routes(definition, "reward", "reward").stream()
			.filter(route -> route.event().equals(
				new QuestEvent.TalkToNpc(NAITING, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.toList();
		assertEquals(1, rewardWindowRoutes.size(),
			() -> "quest " + QUEST + " keeps exactly the npc-complete preview SELECT_QUEST_REWARD route");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			rewardWindowRoutes.getFirst().afterCommit(),
			() -> "quest " + QUEST + " preview opens the quest reward window");
	}

	@Test
	void staleCollapsedSavesHealToTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> heals = enterWorldRecoveries(compiled.definition());
		assertEquals(Set.of(0, 2), healRows(heals),
			() -> "quest " + QUEST + " heals the two legacy collapsed reward rows");
		for (int oldRow : List.of(0, 2)) {
			List<QuestTransition> matches = heals.stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", oldRow))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + QUEST + " heal route for the collapsed row " + oldRow);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)), heal.actions(),
				() -> "quest " + QUEST + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + QUEST + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + QUEST + " heal priority");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", oldRow), Map.of()), heal)
				.orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + QUEST + " healed status");
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + QUEST + " healed journal row");
		}
	}

	@Test
	void collapsedPatientsNoLongerJumpStraightToReward() throws Exception {
		QuestDefinition definition = definition().definition();
		assertTrue(routes(definition, "started", "reward").isEmpty(),
			() -> "quest " + QUEST + " must not keep a started -> reward jump");
		assertTrue(routes(definition, "stage1", "reward").isEmpty(),
			() -> "quest " + QUEST + " must not keep a stage1 -> reward jump");
		for (QuestTransition route : definition.transitions()) {
			if (!"reward".equals(route.targetNode())) {
				continue;
			}
			String source = route.sourceNode();
			assertTrue(source == null || "stage2".equals(source) || "reward".equals(source),
				() -> "quest " + QUEST
					+ " reward is entered from row 2 (or the reward row itself) only, found " + source);
			for (QuestAction action : route.actions()) {
				if (action instanceof QuestAction.SetVariable(String field, int value)
						&& "var0".equals(field)) {
					assertEquals(REWARD_ROW, value,
						() -> "quest " + QUEST + " reward route writes a non reward journal row");
				}
			}
		}
	}

	private static Set<Integer> healRows(List<QuestTransition> heals) {
		Set<Integer> rows = new LinkedHashSet<>();
		for (QuestTransition heal : heals) {
			for (QuestCondition condition : heal.conditions()) {
				if (condition instanceof QuestCondition.QuestVariableIs(String field, int value)
						&& "var0".equals(field)) {
					rows.add(value);
				}
			}
		}
		return rows;
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

	private static CompiledQuestDefinition definition() throws IOException {
		try (InputStream input = InggisonNursingRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
