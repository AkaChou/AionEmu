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
 * 锁定批次 49：21027（[Group] Fearless Kantele）格尔克马洛斯三行阶梯与领奖 owner 收敛。
 * <p>
 * 判据：客户端 {@code quest_q21027.html} 的 {@code quest_summary} 共 3 行——行 0 = 找到被关押的 Kantele
 * （799255），行 1 = 从 Owllau 精锐身上找钥匙 {@code STR_DIC_I_QUEST_21027a}（182207824）交给 Kantele，
 * 行 2 = 和 Asathor（799254）对话报平安；客户端页 select1(1011)/select1_1(1012)/select2(1352) 的按钮分别是
 * HACTION_SELECT1_1(1012)/SETPRO1(10000)/CHECK_USER_HAS_QUEST_ITEM(39)，成功/失败页
 * check_user_item_ok(10000)/fail(10001)，领奖页 select_success(10002) 的按钮是 HACTION_SELECT_QUEST_REWARD(1009)。
 * 迁移前 handler {@code _21027FearlessKantele}（7e9f0316c^）在 Kantele 上做 0 -> 1 推进与
 * {@code checkQuestItems(1, 2, true, 10000, 10001)} 交付（落盘 var0=2 + REWARD），Asathor 是接取与领奖 owner；
 * typed 迁移把交错状态塌陷成 Asathor 上的直跳并让 reward 投影停在 0（审计 MISSING_TAIL_ROWS）。
 * <p>
 * Locks batch 49: the three-row Kantele ladder for 21027 together with the Asathor owner trim. Kantele must walk
 * 0 -> 1 and gate the key hand-over on the client CHECK_USER_HAS_QUEST_ITEM(39) button; the offer, the reward row
 * entry page and the completion stay on Asathor, and stale collapsed reward saves heal to row 2.
 */
class GelkmarosKanteleRowLadderContractTest {

	private static final int QUEST = 21027;
	private static final int ASATHOR = 799254;
	private static final int KANTELE = 799255;
	private static final int KEY = 182207824;
	private static final int REWARD_ROW = 2;

	@Test
	void threeClientJournalRowsOwnAState() throws Exception {
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
		assertEquals(Set.of(0, 1, REWARD_ROW), rows,
			() -> "quest " + QUEST + " must own a state per client journal row");
		assertEquals(REWARD_ROW, node(definition, "reward").projection().variables().get("var0"),
			() -> "quest " + QUEST + " reward journal row");
		assertEquals(Map.of("var0", REWARD_ROW),
			definition.progressLayout().unpack(
				definition.progressLayout().pack(Map.of("var0", REWARD_ROW))),
			() -> "quest " + QUEST + " reward row fits its declared var0 bit field");
	}

	@Test
	void kanteleWalksTheClientPageChain() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			singleRoute(definition, "started", "started", KANTELE, QuestDialogAction.QUEST_SELECT.id())
				.afterCommit(),
			() -> "quest " + QUEST + " row 0 must open the client select1(1011) page");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			singleRoute(definition, "started", "started", KANTELE, QuestDialogAction.SELECT1_1.id())
				.afterCommit(),
			() -> "quest " + QUEST + " row 0 must open the client select1_1(1012) instruction page");
		QuestTransition advance = singleRoute(definition, "started", "stage1", KANTELE,
			QuestDialogAction.SETPRO1.id());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), advance.afterCommit(),
			() -> "quest " + QUEST + " row 0 advances to row 1 like legacy defaultCloseDialog(0, 1)");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			singleRoute(definition, "stage1", "stage1", KANTELE, QuestDialogAction.QUEST_SELECT.id())
				.afterCommit(),
			() -> "quest " + QUEST + " row 1 must open the client select2(1352) page");
	}

	@Test
	void keyHandOverIsGatedByTheClientCheckButton() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestTransition handOver = singleRoute(definition, "stage1", "reward", KANTELE,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(List.of(new QuestCondition.HasItem(KEY, 1)), handOver.conditions(),
			() -> "quest " + QUEST + " key hand-over must require the Owllau key " + KEY);
		assertTrue(handOver.actions().contains(new QuestAction.RemoveItem(KEY, 1)),
			() -> "quest " + QUEST + " key hand-over must consume the key");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			handOver.afterCommit(),
			() -> "quest " + QUEST + " successful hand-over opens check_user_item_ok(10000)");
		assertEquals(0, handOver.priority(), () -> "quest " + QUEST + " hand-over priority");

		QuestTransition fail = singleRoute(definition, "stage1", "stage1", KANTELE,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			fail.afterCommit(),
			() -> "quest " + QUEST + " missing-key branch opens check_user_item_fail(10001)");
		assertEquals(1, fail.priority(), () -> "quest " + QUEST + " fail branch priority");
	}

	@Test
	void resultPagesKeepTheirCloseRoutes() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			singleRoute(definition, "stage1", "stage1", KANTELE, QuestDialogAction.FINISH_DIALOG.id())
				.afterCommit(),
			() -> "quest " + QUEST + " check_user_item_fail(10001) needs its FINISH_DIALOG close route");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			singleRoute(definition, "reward", "reward", KANTELE, QuestDialogAction.FINISH_DIALOG.id())
				.afterCommit(),
			() -> "quest " + QUEST + " check_user_item_ok(10000) needs its FINISH_DIALOG close route");
	}

	@Test
	void rewardRowStaysOnAsathor() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			singleRoute(definition, "reward", "reward", ASATHOR, QuestDialogAction.QUEST_SELECT.id())
				.afterCommit(),
			() -> "quest " + QUEST + " reward row must open the client select_success(10002) page");

		List<QuestTransition> rewardWindowRoutes = routes(definition, "reward", "reward").stream()
			.filter(route -> route.event().equals(
				new QuestEvent.TalkToNpc(ASATHOR, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.toList();
		assertEquals(1, rewardWindowRoutes.size(),
			() -> "quest " + QUEST + " keeps exactly the npc-complete preview SELECT_QUEST_REWARD route");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			rewardWindowRoutes.getFirst().afterCommit(),
			() -> "quest " + QUEST + " preview opens the quest reward window");
	}

	@Test
	void offerAndCompletionStayOnAsathor() throws Exception {
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
		assertEquals(Set.of(ASATHOR), offerNpcs,
			() -> "quest " + QUEST + " offer must stay on the journal NPC");

		Set<Integer> completionNpcs = new LinkedHashSet<>();
		definition.transitions().stream()
			.filter(route -> "complete".equals(route.targetNode()))
			.forEach(route -> {
				if (route.event() instanceof QuestEvent.TalkToNpc talk) {
					completionNpcs.add(talk.npcId());
				}
			});
		assertEquals(Set.of(ASATHOR), completionNpcs,
			() -> "quest " + QUEST + " completion owner must be the journal NPC " + ASATHOR);
		assertEquals(Set.of(ASATHOR, KANTELE), talkNpcIds(definition),
			() -> "quest " + QUEST + " talk routes cover only Asathor and Kantele");
	}

	@Test
	void handOverWalksTheLadderInOrder() throws Exception {
		CompiledQuestDefinition compiled = definition();
		Map<Integer, Integer> withKey = Map.of(KEY, 1);

		List<QuestMutationPlan> advance = plans(compiled, QuestStatus.START, Map.of("var0", 0),
			Map.of(), new QuestEvent.TalkToNpc(KANTELE, QuestDialogAction.SETPRO1.id()));
		assertEquals(1, advance.size(), () -> "quest " + QUEST + " row 0 advances exactly once");
		assertEquals(QuestStatus.START, advance.getFirst().nextStatus(),
			() -> "quest " + QUEST + " row 0 target status");
		assertEquals(1, unpack(compiled, advance.getFirst()).get("var0"),
			() -> "quest " + QUEST + " row 0 must land on row 1");

		List<QuestMutationPlan> handOver = plans(compiled, QuestStatus.START, Map.of("var0", 1),
			withKey, new QuestEvent.TalkToNpc(KANTELE, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		List<QuestMutationPlan> success = handOver.stream()
			.filter(plan -> plan.nextStatus() == QuestStatus.REWARD)
			.toList();
		assertEquals(1, success.size(),
			() -> "quest " + QUEST + " key hand-over must offer exactly one reward plan");
		assertEquals(REWARD_ROW, unpack(compiled, success.getFirst()).get("var0"),
			() -> "quest " + QUEST + " key hand-over must land on the report row");
		assertTrue(success.getFirst().requiredActions().contains(new QuestAction.RemoveItem(KEY, 1)),
			() -> "quest " + QUEST + " key hand-over consumes the key");

		List<QuestMutationPlan> fail = plans(compiled, QuestStatus.START, Map.of("var0", 1),
			Map.of(), new QuestEvent.TalkToNpc(KANTELE, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(1, fail.size(), () -> "quest " + QUEST + " missing key must answer with the fail branch");
		assertEquals(QuestStatus.START, fail.getFirst().nextStatus(),
			() -> "quest " + QUEST + " missing key keeps START");
		assertEquals(1, unpack(compiled, fail.getFirst()).get("var0"),
			() -> "quest " + QUEST + " missing key stays on row 1");

		/* 顺序门禁：Kantele 的推进按钮只在行 0 生效，钥匙检查只在行 1 生效。 */
		/* Order gate: SETPRO1 only answers on row 0 and the key check only on row 1. */
		assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", 1), withKey,
			new QuestEvent.TalkToNpc(KANTELE, QuestDialogAction.SETPRO1.id())).isEmpty(),
			() -> "quest " + QUEST + " SETPRO1 must not answer on row 1");
		assertTrue(plans(compiled, QuestStatus.START, Map.of("var0", 0), withKey,
			new QuestEvent.TalkToNpc(KANTELE, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())).isEmpty(),
			() -> "quest " + QUEST + " key check must not answer on row 0");
	}

	@Test
	void staleCollapsedSavesHealToTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> heals = enterWorldRecoveries(compiled.definition());
		assertEquals(Set.of(0, 1), healRows(heals),
			() -> "quest " + QUEST + " heals the legacy collapsed reward rows");
		for (int oldRow : List.of(0, 1)) {
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
	void collapsedHandOverNoLongerJumpsStraightToReward() throws Exception {
		QuestDefinition definition = definition().definition();
		assertTrue(routes(definition, "started", "reward").isEmpty(),
			() -> "quest " + QUEST + " must not keep a started -> reward jump");
		for (QuestTransition route : definition.transitions()) {
			if (!"reward".equals(route.targetNode())) {
				continue;
			}
			String source = route.sourceNode();
			assertTrue(source == null || "stage1".equals(source) || "reward".equals(source),
				() -> "quest " + QUEST
					+ " reward is entered from row 1 (or the reward row itself) only, found " + source);
			for (QuestAction action : route.actions()) {
				if (action instanceof QuestAction.SetVariable(String field, int value)
						&& "var0".equals(field)) {
					assertEquals(REWARD_ROW, value,
						() -> "quest " + QUEST + " reward route writes a non reward journal row");
				}
			}
		}
	}

	private static QuestTransition singleRoute(QuestDefinition definition, String source, String target,
			int npcId, int actionId) {
		List<QuestTransition> matches = routes(definition, source, target).stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(npcId, actionId)))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + QUEST + " route " + source + " -> " + target
			+ " on npc " + npcId + " action " + actionId);
		return matches.getFirst();
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
			.filter(route -> Objects.equals(source, route.sourceNode()) && target.equals(route.targetNode()))
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
		try (InputStream input = GelkmarosKanteleRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
