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
 * 锁定批次 33：共享可见槽位族（25094 An Offering of Friendship）的行/状态收口。
 * <p>
 * 族判据：客户端 {@code quest_q25094.html} 的 quest_summary 有 3 行，但可见槽位是 {@code %0 / %3 / %3}——行 1
 * “和 Bakring 对话”与行 2“把贝克灵的礼物交给 Daruku”共用同一个槽位，客户端真实状态只有 2 个（状态 0 = 行 0，
 * 状态 1 = 行 1 + 行 2 同时可见）。槽位与状态的换算由已客户端验收的 10527/20527 锁定：16 行任务的槽位是
 * {@code 0,3,6,...,45} 且 reward=15 正确，即“槽位 = 3 × 状态号”。因此 25094 的 reward 投影必须是 1；
 * 行号口径（client_rows=3）在本任务多算一行，这也是审计把它判成缺尾的原因。
 * <p>
 * 迁移前 handler {@code _25094An_Offering_Of_Friendship} 的 {@code checkQuestItems(env, 0, 1, true, 10000, 10001)}
 * 意图同样是 0 -&gt; 1 后置 REWARD，只是旧 helper 的 reward 分支不写 nextStep（QE-054），旧存档因此落在 0；
 * 本批补 {@code REWARD/var0=0 -> 1} 的 enter-world 自愈边。
 * <p>
 * Locks batch 33: the shared-client-visibility-slot member 25094, where two journal rows share one client
 * condition slot, so the ladder owns two states and the reward projection stays on the shared slot.
 */
class Batch33SharedVisibleSlotContractTest {

	private static final int QUEST = 25094;
	private static final int ACCEPT_NPC = 804929;
	private static final int BONE_OBJECT = 702768;
	private static final int REWARD_NPC = 804740;
	private static final int BONE_ITEM = 182215736;
	private static final int BONE_COUNT = 10;
	/** 共享槽位状态号：客户端 `%3` = 状态 1（行 1 与行 2 同时可见）。 */
	private static final int SHARED_SLOT_STATE = 1;
	private static final int COLLECT_STATE = 0;

	@Test
	void sharedSlotJournalKeepsTwoClientStates() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(COLLECT_STATE, node(definition, "started").projection().variables().get("var0"),
			() -> "row 0 lives on the collect state");
		assertEquals(SHARED_SLOT_STATE, node(definition, "s1").projection().variables().get("var0"),
			() -> "rows 1 and 2 share the %3 slot");
		assertEquals(SHARED_SLOT_STATE, node(definition, "reward").projection().variables().get("var0"),
			() -> "the reward row is the shared slot, not the next row index");
		assertEquals(QuestStatus.START, node(definition, "s1").projection().status());
		assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status());
	}

	@Test
	void everyVisibleSlotOwnsAState() throws Exception {
		QuestDefinition definition = definition().definition();
		Set<Integer> rows = new LinkedHashSet<>();
		for (QuestNode node : definition.nodes()) {
			Integer row = node.projection().variables().get("var0");
			QuestStatus status = node.projection().status();
			if (row == null || row > SHARED_SLOT_STATE
					|| (status != QuestStatus.START && status != QuestStatus.REWARD)) {
				continue;
			}
			rows.add(row);
		}
		assertEquals(Set.of(COLLECT_STATE, SHARED_SLOT_STATE), rows,
			() -> "both client visibility slots must own a state");
	}

	@Test
	void collectStepAdvancesIntoTheSharedSlot() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> handOver = routes(definition, "started", "s1");
		assertEquals(1, handOver.size(), () -> "the collect state advances exactly once");
		QuestTransition route = handOver.getFirst();
		assertEquals(new QuestEvent.TalkToNpc(
			ACCEPT_NPC, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id(), 0), route.event(),
			() -> "row 1 ends on the client quest-item check button");
		assertTrue(route.conditions().contains(new QuestCondition.HasItem(BONE_ITEM, BONE_COUNT)),
			() -> "the check requires the collected bones");
		assertTrue(route.actions().contains(new QuestAction.SetVariable("var0", SHARED_SLOT_STATE)),
			() -> "handing the bones over persists the shared slot");
		assertTrue(route.actions().contains(new QuestAction.RemoveItem(BONE_ITEM, BONE_COUNT)),
			() -> "the bones are consumed on hand-over");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			route.afterCommit(),
			() -> "the client shows the success page for the collected bones");
	}

	@Test
	void briefingFinishesOnSetSucceed() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> finish = routes(definition, "s1", "reward").stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
				ACCEPT_NPC, QuestDialogAction.SET_SUCCEED.id(), 0)))
			.toList();
		assertEquals(1, finish.size(), () -> "the select2 page ends on SET_SUCCEED exactly once");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), finish.getFirst().afterCommit(),
			() -> "SET_SUCCEED refreshes the journal and closes the dialog");
		assertEquals(1, routes(definition, "s1", "s1").stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
				ACCEPT_NPC, QuestDialogAction.QUEST_SELECT.id(), 0)))
			.count(),
			() -> "the client select2 page stays reachable from the shared slot");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			routes(definition, "s1", "s1").stream()
				.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
					ACCEPT_NPC, QuestDialogAction.FINISH_DIALOG.id(), 0)))
				.findFirst().orElseThrow(() -> new AssertionError(
					"the client check_ok page closes on FINISH_DIALOG"))
				.afterCommit(),
			() -> "the check_ok page keeps its FINISH_DIALOG route on the shared slot");
	}

	@Test
	void rewardNpcAlsoOpensTheSharedSlot() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> entry = routes(definition, "s1", "reward").stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
				REWARD_NPC, QuestDialogAction.QUEST_SELECT.id(), 0)))
			.toList();
		assertEquals(1, entry.size(),
			() -> "talking to the reward NPC must not soft-lock the shared slot");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			entry.getFirst().afterCommit(),
			() -> "Daruku opens the client reward entry page");
	}

	@Test
	void rewardWindowSeatsOnTheRewardNpc() throws Exception {
		QuestDefinition definition = definition().definition();
		Set<Integer> completionNpcs = new LinkedHashSet<>();
		definition.transitions().stream()
			.filter(route -> "complete".equals(route.targetNode()))
			.forEach(route -> {
				if (route.event() instanceof QuestEvent.TalkToNpc talk) {
					completionNpcs.add(talk.npcId());
				}
			});
		assertEquals(Set.of(REWARD_NPC), completionNpcs,
			() -> "completion stays on the reward-row NPC");

		QuestTransition claim = routes(definition, "reward", "reward").stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(
				REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD.id(), 0)))
			.findFirst().orElseThrow(() -> new AssertionError("missing claim route"));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), claim.afterCommit(),
			() -> "the claim button opens the reward window");
		assertTrue(routes(definition, "reward", "reward").stream()
			.anyMatch(route -> route.event().equals(new QuestEvent.TalkToNpc(
				REWARD_NPC, QuestDialogAction.QUEST_SELECT.id(), 0))),
			() -> "the reward entry page stays reachable in REWARD");
	}

	@Test
	void staleCollapsedRewardSavesHealToTheSharedSlot() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> matches = compiled.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", COLLECT_STATE))))
			.toList();
		assertEquals(1, matches.size(), () -> "the collapsed reward save needs one heal route");
		QuestTransition heal = matches.getFirst();
		assertEquals(List.of(new QuestAction.SetVariable("var0", SHARED_SLOT_STATE)), heal.actions(),
			() -> "the heal writes the shared slot");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
			() -> "the heal refreshes the journal");
		assertNull(heal.priority(), () -> "the heal carries no priority");

		Map<String, Integer> stale = new LinkedHashMap<>(compiled.definition().progressLayout().unpack(0));
		stale.put("var0", COLLECT_STATE);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(compiled, stale), heal)
			.orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus(), () -> "healed status");
		assertEquals(SHARED_SLOT_STATE, unpack(compiled, plan).get("var0"),
			() -> "healed journal row");
	}

	@Test
	void collapsedJumpNoLongerSkipsTheSharedSlot() throws Exception {
		QuestDefinition definition = definition().definition();
		assertTrue(routes(definition, "started", "reward").isEmpty(),
			() -> "the collapsed started -> reward jump must be gone");
		assertTrue(definition.transitions().stream()
			.noneMatch(route -> route.actions().contains(new QuestAction.SetVariable("var0", 2))),
			() -> "no route may write the surplus row index");
		assertTrue(routes(definition, "started", "started").stream()
			.anyMatch(route -> route.event().equals(new QuestEvent.CanAct(
				BONE_OBJECT, "ACTION_ITEM_USE"))),
			() -> "the bone object stays actionable on the collect row");
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

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(variables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition() throws IOException {
		try (InputStream input = Batch33SharedVisibleSlotContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
