package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 38：活动阵营选择族“Lover or Loner? / Bitter or Sweet?”的三行状态与完成奖励索引。
 * <p>
 * 80298/80299（Elyos）与 80304/80305（Asmodian）的客户端任务书都是三行、槽位 %0/%3/%6：
 * 行 0「回答有恋人还是孤单一人」、行 1「去见坠入爱河的术古」、行 2「去见单身部队成员」。
 * 页链 select_none -&gt; select1_1 -&gt; select2_1/select2_2 -&gt; select3_1/select3_2 的结束按钮分别是
 * SETPRO1/SETPRO2；它们不是中间行，而是把完成奖励索引冻结成 1（情侣）或 2（单身）。后续
 * 80300/80302/80306/80308 以 reward-mode=1 接取，80301/80303/80307/80309 以 reward-mode=2 接取。
 * 迁移把 SETPRO2 直接送到 reward/var0=0、SETPRO1 只送到 started/var0=0，导致行 1/2 没有状态。
 * </p>
 * <p>
 * Locks batch 38: the event-side branch-choice family. The two rows after the answer are branch
 * projections, and the completion reward index is the durable branch flag consumed by the
 * follow-up quests' {@code reward-mode} start conditions.
 * </p>
 */
class Batch38BranchChoiceRewardIndexContractTest {

	private static final int NPC_ID = 799763;

	private record BranchQuest(int questId, int coupleFollowUp, int soloFollowUp) {
	}

	private static final List<BranchQuest> FAMILY = List.of(
		new BranchQuest(80298, 80300, 80301),
		new BranchQuest(80299, 80302, 80303),
		new BranchQuest(80304, 80306, 80307),
		new BranchQuest(80305, 80308, 80309)
	);

	@Test
	void everyJournalRowOwnsAStateAndBranchProjection() throws Exception {
		for (BranchQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertNode(definition, contract.questId(), "started", QuestStatus.START, 0);
			assertNode(definition, contract.questId(), "reward1", QuestStatus.REWARD, 1);
			assertNode(definition, contract.questId(), "reward", QuestStatus.REWARD, 2);
			assertTrue(definition.progressLayout().field("var0").maxValue() >= 2,
				() -> "quest " + contract.questId() + " must widen var0 so both branch rows fit");

			Set<Integer> rows = new LinkedHashSet<>();
			for (QuestNode candidate : definition.nodes()) {
				Integer row = candidate.projection().variables().get("var0");
				QuestStatus status = candidate.projection().status();
				if (row == null || row > 2 || (status != QuestStatus.START && status != QuestStatus.REWARD)) {
					continue;
				}
				rows.add(row);
				assertEquals(row == 0 ? QuestStatus.START : QuestStatus.REWARD, status,
					() -> "quest " + contract.questId() + " row " + row + " status");
			}
			assertEquals(Set.of(0, 1, 2), rows,
				() -> "quest " + contract.questId() + " owns a state per journal row");
		}
	}

	@Test
	void pageChainSelectsTheBranchAndOpensTheMatchingRewardWindow() throws Exception {
		for (BranchQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			assertPageRoute(definition, "unaccepted", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT2_2, QuestDialogPage.SELECT2_2);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
			assertPageRoute(definition, "unaccepted", QuestDialogAction.SELECT3_2, QuestDialogPage.SELECT3_2);
			assertPageRoute(definition, "started", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT2_2, QuestDialogPage.SELECT2_2);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
			assertPageRoute(definition, "started", QuestDialogAction.SELECT3_2, QuestDialogPage.SELECT3_2);

			assertBranchRoute(definition, "unaccepted", QuestDialogAction.SETPRO1,
				"reward1", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
			assertBranchRoute(definition, "started", QuestDialogAction.SETPRO1,
				"reward1", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
			assertBranchRoute(definition, "unaccepted", QuestDialogAction.SETPRO2,
				"reward", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
			assertBranchRoute(definition, "started", QuestDialogAction.SETPRO2,
				"reward", QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);

			assertPreview(definition, "reward1", QuestDialogAction.USE_OBJECT,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
			assertPreview(definition, "reward1", QuestDialogAction.SELECT_QUEST_REWARD,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
			assertPreview(definition, "reward", QuestDialogAction.USE_OBJECT,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
			assertPreview(definition, "reward", QuestDialogAction.SELECT_QUEST_REWARD,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
		}
	}

	@Test
	void completionFreezesBranchRewardIndexForFollowUps() throws Exception {
		for (BranchQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertCompletionIndex(definition, contract.questId(), "reward1", 1);
			assertCompletionIndex(definition, contract.questId(), "reward", 2);
			assertFollowUp(contract.coupleFollowUp(), contract.questId(), 1);
			assertFollowUp(contract.soloFollowUp(), contract.questId(), 2);
		}
	}

	@Test
	void staleSoloRewardSaveHealsToRowTwo() throws Exception {
		for (BranchQuest contract : FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> stale = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null)
				.filter(route -> "reward".equals(route.targetNode()))
				.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0))))
				.toList();
			assertEquals(1, stale.size(),
				() -> "quest " + contract.questId() + " must heal the old solo reward save");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), stale.getFirst().actions(),
				() -> "quest " + contract.questId() + " heals to the solo row");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), stale.getFirst().afterCommit(),
				() -> "quest " + contract.questId() + " refreshes the journal after healing");
		}
	}

	private static void assertNode(QuestDefinition definition, int questId, String label,
		QuestStatus status, int row) {
		QuestNode node = node(definition, label);
		assertEquals(status, node.projection().status(),
			() -> "quest " + questId + " node " + label + " status");
		assertEquals(row, node.projection().variables().get("var0"),
			() -> "quest " + questId + " node " + label + " row");
	}

	private static void assertPageRoute(QuestDefinition definition, String source,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> routes = dialogRoutes(definition, source, action);
		assertEquals(1, routes.size(),
			() -> "quest route " + source + " + " + action + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			routes.getFirst().afterCommit(),
			() -> "quest route " + source + " + " + action + " page");
	}

	private static void assertBranchRoute(QuestDefinition definition, String source,
		QuestDialogAction action, String target, QuestDialogPage page) {
		List<QuestTransition> routes = routes(definition, source, target).stream()
			.filter(route -> dialogId(route, action))
			.toList();
		assertEquals(1, routes.size(),
			() -> "quest route " + source + " + " + action + " -> " + target + " must be unique");
		assertTrue(routes.getFirst().afterCommit().contains(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			() -> "quest route " + source + " + " + action + " refreshes visibility");
		assertTrue(routes.getFirst().afterCommit().contains(new AfterCommitAction.ShowQuestDialog(page.id())),
			() -> "quest route " + source + " + " + action + " opens the matching reward window");
	}

	private static void assertPreview(QuestDefinition definition, String source,
		QuestDialogAction action, QuestDialogPage page) {
		List<QuestTransition> routes = dialogRoutes(definition, source, action);
		assertEquals(1, routes.size(),
			() -> "quest preview " + source + " + " + action + " must be unique");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), routes.getFirst().afterCommit(),
			() -> "quest preview " + source + " + " + action + " window");
	}

	private static void assertCompletionIndex(QuestDefinition definition, int questId,
		String source, int rewardIndex) {
		List<QuestTransition> completions = routes(definition, source, "complete");
		assertFalse(completions.isEmpty(),
			() -> "quest " + questId + " source " + source + " must have a completion route");
		for (QuestTransition completion : completions) {
			assertTrue(talk(completion),
				() -> "quest " + questId + " completion must stay on the client NPC");
			assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(rewardIndex)),
				() -> "quest " + questId + " source " + source + " must freeze reward index "
					+ rewardIndex + " but had " + completion.actions());
		}
	}

	private static void assertFollowUp(int followUpQuestId, int parentQuestId, int rewardMode)
		throws IOException {
		assertTrue(definition(followUpQuestId).definition().metadata().startConditions().contains(
			new QuestStartCondition("finished", parentQuestId, rewardMode)),
			() -> "quest " + followUpQuestId + " must consume parent " + parentQuestId
				+ " reward-mode " + rewardMode);
	}

	private static List<QuestTransition> dialogRoutes(QuestDefinition definition, String source,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> dialogId(route, action))
			.toList();
	}

	private static boolean dialogId(QuestTransition route, QuestDialogAction action) {
		return route.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == NPC_ID && talk.dialogId() == action.id();
	}

	private static boolean talk(QuestTransition route) {
		return route.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == NPC_ID;
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

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch38BranchChoiceRewardIndexContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
