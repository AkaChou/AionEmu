package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest3961To3964RetailAlignmentTest {
	private static final int FLORA = 798384;
	private static final int ERDOS = 203740;
	private static final List<Spec> SPECS = List.of(
		new Spec(3961, 182206108, List.of(new RequiredItem(182400001, 40000))),
		new Spec(3962, 182206109, List.of(new RequiredItem(186000088, 1),
			new RequiredItem(182400001, 50000))),
		new Spec(3963, 182206110, List.of(new RequiredItem(186000089, 1),
			new RequiredItem(182400001, 70000))),
		new Spec(3964, 182206111, List.of(new RequiredItem(186000090, 1),
			new RequiredItem(182400001, 90000)))
	);

	@Test
	void followsTheClientCharmDialogsAndLegacyTwoStepItemContracts() throws Exception {
		for (Spec spec : SPECS) {
			QuestDefinition definition = compile(spec.questId());

			assertPage(definition, "unaccepted", FLORA, QuestDialogAction.QUEST_SELECT,
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
			QuestTransition accept = route(definition, "unaccepted", FLORA, QuestDialogAction.QUEST_ACCEPT_1);
			assertEquals("started", accept.targetNode(), "quest " + spec.questId() + " accept target");
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(spec.workItemId(), 1)),
				"quest " + spec.questId() + " work item grant");
			assertTrue(routes(definition, "unaccepted", ERDOS).isEmpty(),
				"quest " + spec.questId() + " intermediate NPC must not start the quest");

			assertPage(definition, "started", ERDOS, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPage(definition, "started", ERDOS, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			QuestTransition handoff = route(definition, "started", ERDOS, QuestDialogAction.SETPRO1);
			assertEquals("s1", handoff.targetNode(), "quest " + spec.questId() + " handoff target");
			assertEquals(List.of(new QuestAction.RemoveItem(spec.workItemId(), 1)), handoff.actions(),
				"quest " + spec.questId() + " work item removal");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), handoff.afterCommit(),
				"quest " + spec.questId() + " handoff response");

			// S2：交付 = QUEST_SELECT(s1→reward) 带整组 HasItem 门直翻领奖态并下发奖励窗；SELECT5 报告页、
			// 39/20002 检查对与 select6 失败页随规范交付段退场（未集齐零路由，关窗兜底交 DialogService）。
			// S2 canonical delivery: QUEST_SELECT(s1→reward) gated by the whole hand-in set flips REWARD and
			// shows the reward window; the report page, the check pairs and the failure page retire.
			QuestTransition delivery = route(definition, "s1", FLORA, QuestDialogAction.QUEST_SELECT);
			assertEquals("reward", delivery.targetNode(), "quest " + spec.questId() + " delivery target");
			assertEquals(spec.requirements().stream()
				.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count())).toList(),
				delivery.conditions(), "quest " + spec.questId() + " hand-in gate");
			assertEquals(spec.requirements().stream()
				.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count())).toList(),
				delivery.actions(), "quest " + spec.questId() + " item removals");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
				deliveryWindowPage(definition.metadata()))), delivery.afterCommit(),
				"quest " + spec.questId() + " delivery response");
			assertTrue(routes(definition, "s1", FLORA).stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()
						|| talk.dialogId() == QuestDialogAction.FINISH_DIALOG.id())),
				"quest " + spec.questId() + " 的检查对与失败页关闭出口必须随规范交付段退场");

			QuestTransition completion = route(definition, "reward", FLORA,
				QuestDialogAction.SELECTED_QUEST_REWARD1);
			assertEquals("complete", completion.targetNode(), "quest " + spec.questId() + " completion target");
			assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)),
				"quest " + spec.questId() + " completion action");
			assertTrue(routes(definition, "reward", ERDOS).isEmpty(),
				"quest " + spec.questId() + " intermediate NPC must not complete the quest");
		}
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	private static QuestDefinition compile(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId).definition();
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private record RequiredItem(int itemId, int count) {
	}

	private record Spec(int questId, int workItemId, List<RequiredItem> requirements) {
	}
}
