package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定零售收集任务中的接取、上交、交互物和领奖角色，以及收集物的单次事务扣除。
 * Locks retail collection quest offer, turn-in, interaction-item, and completion roles together with the
 * single transactional removal of collected items.
 */
class QuestRetailCollectionRoleAlignmentTest {
	@ParameterizedTest(name = "quest {0}")
	@MethodSource("collectionCases")
	void preservesRetailNpcRolesAndSingleTurnInRemoval(CollectionCase questCase) throws Exception {
		QuestDefinition definition = load(questCase.questId());
		assertEquals(new NodeProjection(QuestStatus.NONE, Map.of("var0", 0)),
			node(definition, "unaccepted").projection());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 0)),
			node(definition, "started").projection());
		// QE-051：reward 投影 = 客户端任务书领奖行（见 questCase.rewardRow，取自 quest_q<id> 可见槽位）。
		// QE-051: the reward projection is the client journal reward row (see questCase.rewardRow).
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", questCase.rewardRow())),
			node(definition, "reward").projection());
		assertEquals(new NodeProjection(QuestStatus.COMPLETE, Map.of("var0", 0)),
			node(definition, "complete").projection());

		List<QuestTransition> accepts = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> "started".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Set.of(QuestDialogAction.QUEST_ACCEPT_1.id(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
					.contains(talk.dialogId()))
			.toList();
		assertEquals(2, accepts.size());
		assertTrue(accepts.stream().allMatch(transition -> talk(transition).npcId() == questCase.startNpcId()));
		assertTrue(accepts.stream().allMatch(transition ->
			transition.conditions().equals(List.of(new QuestCondition.StartEligible()))));

		assertRoute(definition, "started", "started", questCase.startNpcId(),
			QuestDialogAction.QUEST_SELECT, null, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())));

		List<QuestCondition> itemConditions = questCase.items().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> turnInActions = new ArrayList<>();
		if (!"reward".equals(questCase.turnInTarget())) {
			// 交付边落在中间行（未到领奖态）时会显式写下目标行号，与其它行阶梯任务同形。
			// A hand-in that lands on an intermediate row writes the target row explicitly.
			turnInActions.add(new QuestAction.SetVariable("var0", 1));
		}
		questCase.items().forEach(item -> turnInActions.add(new QuestAction.RemoveItem(item.itemId(), item.count())));
		assertRoute(definition, "started", questCase.turnInTarget(), questCase.startNpcId(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0, itemConditions, turnInActions,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(questCase.successPage())));
		assertRoute(definition, "started", "started", questCase.startNpcId(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 1, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())));

		assertRewardReportRoutes(definition, questCase.rewardNpcId());
		assertCompletionRoutes(definition, questCase.rewardNpcId(), questCase.finishDialogCompletes());
		assertInteractionObjectContract(definition, questCase);
	}

	@Test
	void quest25022PreservesTheTwoStepMaterialAndSoupFlow() throws Exception {
		QuestDefinition definition = load(25022);
		assertEquals(List.of(new QuestItemRequirement(182215709, 1)), definition.metadata().questWorkItems());
		assertUniqueAcceptNpc(definition, 804908);

		// 真端驱动混合采集链（DD_TALK_COLLECT_CHAIN）：采集段落点节点 = s1（旧形 prepared）。
		// The retail-driven mixed collect chain (DD_TALK_COLLECT_CHAIN) names the collect landing
		// state s1 (the legacy shape called it prepared).
		QuestTransition turnIn = route(definition, "started", "s1", 804908,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0);
		assertEquals(List.of(
			new QuestAction.RemoveItem(182215707, 5),
			new QuestAction.RemoveItem(182215708, 5)), turnIn.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			turnIn.afterCommit());
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 1)),
			node(definition, "s1").projection());

		assertRoute(definition, "s1", "s1", 804908, QuestDialogAction.FINISH_DIALOG, null,
			List.of(), List.of(), List.of(new AfterCommitAction.CloseDialog()));
		assertRoute(definition, "s1", "s1", 804908, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
		assertRoute(definition, "s1", "reward", 804908, QuestDialogAction.SET_SUCCEED, null,
			List.of(), List.of(new QuestAction.GiveItem(182215709, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertEquals(new NodeProjection(QuestStatus.REWARD, Map.of("var0", 2)),
			node(definition, "reward").projection());

		assertRewardReportRoutes(definition, 804724);
		assertCompletionRoutes(definition, 804724, false);
		assertTrue(completionRoutes(definition).stream().allMatch(transition ->
			transition.actions().stream().noneMatch(QuestAction.GiveItem.class::isInstance)));
	}

	@Test
	void quest25073ConsumesThePoisonOnlyWhenTheInteractionSucceeds() throws Exception {
		QuestDefinition definition = load(25073);
		assertEquals(List.of(new QuestItemRequirement(182215725, 1)), definition.metadata().questWorkItems());
		assertUniqueAcceptNpc(definition, 804918);

		QuestTransition canAct = definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.CanAct(731556, "ACTION_ITEM_USE")))
			.findFirst().orElseThrow();
		assertEquals("started", canAct.sourceNode());
		assertEquals("started", canAct.targetNode());

		assertRoute(definition, "started", "started", 731556, QuestDialogAction.USE_OBJECT, null,
			List.of(new QuestCondition.HasItem(182215725, 1)), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())));
		assertRoute(definition, "started", "reward", 731556, QuestDialogAction.SET_SUCCEED, null,
			List.of(new QuestCondition.HasItem(182215725, 1)),
			List.of(new QuestAction.RemoveItem(182215725, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertRoute(definition, "started", "started", 731556, QuestDialogAction.FINISH_DIALOG, null,
			List.of(), List.of(), List.of(new AfterCommitAction.CloseDialog()));

		assertRewardReportRoutes(definition, 804732);
		assertCompletionRoutes(definition, 804732, false);
	}

	private static Stream<CollectionCase> collectionCases() {
		return Stream.of(
			collection(25013, 804907, 804907, items(item(182215704, 4)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 702750, false, false, 0),
			// 下列任务的客户端任务书都有独立领奖行（quest_q<id> 的可见槽位 %0/%3…），reward 投影落在领奖行。
			// The quests below own a separate client journal reward row, so their reward projection sits on it.
			collection(25062, 804917, 804918, items(item(182215722, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 0, false, false, 1),
			collection(25080, 804922, 804922, items(item(182215727, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 702751, false, false, 0),
			collection(25081, 804922, 804922, items(item(182215732, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 702752, false, false, 0),
			// 25094 的客户端行 1/2 共槽（%3/%3），交付边落在中间行 s1；其余任务的交付边直奔领奖态 reward。
			// 25094 shares slot %3 between its rows 1/2, so the hand-in lands on the intermediate row s1.
			collection(25094, 804929, 804740, items(item(182215736, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 702768, false, false, 1, "s1"),
			collection(25526, 806109, 806109, items(item(182215970, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 703082, false, false, 1),
			collection(25532, 806111, 806111, items(item(182215971, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 703084, false, false, 1),
			collection(25535, 806112, 806112, items(item(182215972, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 703085, false, false, 1),
			collection(25538, 806255, 806255, items(item(182216066, 10)),
				QuestDialogPage.CHECK_USER_ITEM_OK.id(), 703295, false, false, 1),
			// 交付成功后直接进入奖励窗：该任务客户端确认页 10000 的按钮是本地关闭，不能承载服务端续接。
			// Straight into the reward window: this quest's client confirmation page 10000 button is a local
			// close and cannot carry a server continuation.
			collection(25690, 806697, 806697, items(item(186000474, 1)),
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id(), 0, false, true, 1));
	}

	private static CollectionCase collection(int questId, int startNpcId, int rewardNpcId,
			List<CollectedItem> items, int successPage, int interactionNpcId,
			boolean closeInteraction, boolean finishDialogCompletes, int rewardRow) {
		return collection(questId, startNpcId, rewardNpcId, items, successPage, interactionNpcId,
			closeInteraction, finishDialogCompletes, rewardRow, "reward");
	}

	private static CollectionCase collection(int questId, int startNpcId, int rewardNpcId,
			List<CollectedItem> items, int successPage, int interactionNpcId,
			boolean closeInteraction, boolean finishDialogCompletes, int rewardRow,
			String turnInTarget) {
		return new CollectionCase(questId, startNpcId, rewardNpcId, items, successPage,
			interactionNpcId, closeInteraction, finishDialogCompletes, rewardRow, turnInTarget);
	}

	private static CollectedItem item(int itemId, int count) {
		return new CollectedItem(itemId, count);
	}

	private static List<CollectedItem> items(CollectedItem... items) {
		return List.of(items);
	}

	private static void assertUniqueAcceptNpc(QuestDefinition definition, int npcId) {
		List<QuestTransition> accepts = definition.transitions().stream()
			.filter(transition -> "unaccepted".equals(transition.sourceNode()))
			.filter(transition -> "started".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Set.of(QuestDialogAction.QUEST_ACCEPT_1.id(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
					.contains(talk.dialogId()))
			.toList();
		assertEquals(2, accepts.size());
		assertTrue(accepts.stream().allMatch(transition -> talk(transition).npcId() == npcId));
	}

	private static void assertInteractionObjectContract(QuestDefinition definition, CollectionCase questCase) {
		List<QuestTransition> canActRoutes = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.CanAct)
			.toList();
		if (questCase.interactionNpcId() == 0) {
			assertTrue(canActRoutes.isEmpty());
			return;
		}
		assertEquals(List.of(new QuestEvent.CanAct(questCase.interactionNpcId(), "ACTION_ITEM_USE")),
			canActRoutes.stream().map(QuestTransition::event).toList());
		QuestTransition useObject = route(definition, "started", "started", questCase.interactionNpcId(),
			QuestDialogAction.USE_OBJECT, null);
		List<AfterCommitAction> expected = questCase.closeInteraction()
			? List.of(new AfterCommitAction.CloseDialog()) : List.of();
		assertEquals(List.of(), useObject.conditions());
		assertEquals(List.of(), useObject.actions());
		assertEquals(expected, useObject.afterCommit());
	}

	private static void assertRewardReportRoutes(QuestDefinition definition, int npcId) {
		assertRoute(definition, "reward", "reward", npcId, QuestDialogAction.QUEST_SELECT, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
		assertRoute(definition, "reward", "reward", npcId, QuestDialogAction.SELECT_QUEST_REWARD, null,
			List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
	}

	private static void assertCompletionRoutes(QuestDefinition definition, int npcId,
			boolean finishDialogCompletes) {
		List<QuestTransition> completions = completionRoutes(definition);
		Set<Integer> expectedDialogIds = IntStream.rangeClosed(
			QuestDialogAction.SELECTED_QUEST_REWARD1.id(), QuestDialogAction.SELECTED_QUEST_NOREWARD.id())
			.boxed().collect(java.util.stream.Collectors.toCollection(HashSet::new));
		if (finishDialogCompletes) {
			expectedDialogIds.add(QuestDialogAction.FINISH_DIALOG.id());
		}
		assertEquals(expectedDialogIds, completions.stream().map(QuestRetailCollectionRoleAlignmentTest::talk)
			.map(QuestEvent.TalkToNpc::dialogId).collect(java.util.stream.Collectors.toSet()));
		assertTrue(completions.stream().allMatch(transition -> talk(transition).npcId() == npcId));

		List<QuestAction.GrantReward> fixedRewards = definition.metadata().rewards().stream()
			.filter(reward -> !"SELECTABLE_ITEM".equals(reward.kind()))
			.map(QuestRetailCollectionRoleAlignmentTest::grantReward)
			.toList();
		for (QuestTransition completion : completions) {
			assertEquals(List.of(), completion.conditions());
			assertNull(completion.priority());
			assertFalse(completion.actions().stream().anyMatch(QuestAction.RemoveItem.class::isInstance));
			assertEquals(fixedRewards, completion.actions().stream()
				.filter(QuestAction.GrantReward.class::isInstance)
				.map(QuestAction.GrantReward.class::cast)
				.filter(reward -> fixedRewards.contains(reward))
				.toList());
			assertInstanceOf(QuestAction.CompleteQuest.class, completion.actions().getLast());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}

		List<QuestReward> selectableRewards = definition.metadata().rewards().stream()
			.filter(reward -> "SELECTABLE_ITEM".equals(reward.kind()))
			.toList();
		for (int index = 0; index < selectableRewards.size(); index++) {
			QuestReward selectable = selectableRewards.get(index);
			int dialogId = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + index;
			QuestTransition choice = completions.stream()
				.filter(transition -> Objects.equals(talk(transition).dialogId(), dialogId))
				.findFirst().orElseThrow();
			assertTrue(choice.actions().contains(new QuestAction.GrantReward("ITEM", selectable.id(),
				selectable.amount(), QuestRewardAmountMode.EXACT)));
		}
	}

	private static QuestAction.GrantReward grantReward(QuestReward reward) {
		QuestRewardAmountMode amountMode = Set.of("GOLD", "EXP").contains(reward.kind())
			? QuestRewardAmountMode.QUEST_BASE : QuestRewardAmountMode.EXACT;
		return new QuestAction.GrantReward(reward.kind(), reward.id(), reward.amount(), amountMode);
	}

	private static List<QuestTransition> completionRoutes(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow();
	}

	private static void assertRoute(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action, Integer priority, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = route(definition, source, target, npcId, action, priority);
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action, Integer priority) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Objects.equals(talk.dialogId(), action.id()))
			.filter(transition -> Objects.equals(transition.priority(), priority))
			.findFirst().orElseThrow();
	}

	private static QuestEvent.TalkToNpc talk(QuestTransition transition) {
		return assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event());
	}

	private static QuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId).definition();
	}

	/**
	 * 描述一次收集物上交要求。
	 * Describes one collected-item turn-in requirement.
	 */
	private record CollectedItem(int itemId, int count) {
	}

	/**
	 * 描述共享零售收集任务合同中的任务专用常量。
	 * Describes quest-specific constants in the shared retail collection contract.
	 */
	private record CollectionCase(int questId, int startNpcId, int rewardNpcId, List<CollectedItem> items,
		int successPage, int interactionNpcId,
		boolean closeInteraction, boolean finishDialogCompletes, int rewardRow, String turnInTarget) {
		@Override
		public String toString() {
			return Integer.toString(questId);
		}
	}
}
