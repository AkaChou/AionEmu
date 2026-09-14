package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 2947 的最终交任务合同：可选工作物品（黄金头盔）的整叠移除不得阻断完成。
 * Verifies quest 2947's final turn-in contract: removing the optional golden-helmet work item as a
 * whole stack must never block completion, whether or not the player still carries it.
 */
class Quest2947RewardTurnInTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 2947;
	private static final int REWARD_NPC = 204301;
	private static final int WORK_ITEM = 182207037;
	private static final int REWARD_ITEM = 167000497;
	private static final int FIRST_REWARD_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_REWARD_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
	private static final List<PlayerClass> CLASSES = List.of(
		PlayerClass.GLADIATOR, PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.ASSASSIN,
		PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER, PlayerClass.CLERIC, PlayerClass.CHANTER,
		PlayerClass.GUNSLINGER, PlayerClass.SONGWEAVER, PlayerClass.AETHERTECH);
	private static final List<Integer> CLASS_WEAPONS = List.of(
		100201365, 100001562, 100201365, 100201365, 100601285, 100601285,
		100101199, 100101199, 101801035, 102001058, 102100833);

	@Test
	void everyAdvancedClassRouteCompletesWhenTheHelmetIsMissing() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestNode source = node(definition, "s9");
		QuestNode complete = node(definition, "complete");
		assertEquals(QuestStatus.REWARD, source.projection().status());
		assertEquals(Map.of("var0", 9), source.projection().variables());
		assertEquals(QuestStatus.COMPLETE, complete.projection().status());
		assertEquals(Map.of("var0", 0), complete.projection().variables());

		for (int classIndex = 0; classIndex < CLASSES.size(); classIndex++) {
			PlayerClass playerClass = CLASSES.get(classIndex);
			QuestTransition turnIn = rewardTurnInRoute(definition, playerClass, classIndex, LAST_REWARD_ACTION);

			assertEquals("s9", turnIn.sourceNode());
			assertEquals("complete", turnIn.targetNode());
			assertEquals(classIndex, turnIn.priority());
			assertEquals(List.of(new QuestCondition.AdvancedClassIs(playerClass)), turnIn.conditions());
			assertEquals(finalTurnInActions(CLASS_WEAPONS.get(classIndex)), turnIn.actions());
			assertEquals(finalTurnInAfterCommit(), turnIn.afterCommit());

			QuestMutationPlan plan = plan(definition, turnIn, playerClass, Map.of());
			assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
			assertEquals(definition.definition().progressLayout().pack(Map.of("var0", 0, "var4", 10)),
				plan.nextPackedVariables());
			assertEquals(finalTurnInActions(CLASS_WEAPONS.get(classIndex)), plan.requiredActions());
			assertEquals(finalTurnInAfterCommit(), plan.afterCommit());
		}
	}

	@Test
	void everyRewardWindowActionSharesTheClassRoute() throws Exception {
		CompiledQuestDefinition definition = load();
		for (int action = FIRST_REWARD_ACTION; action <= LAST_REWARD_ACTION; action++) {
			for (int classIndex = 0; classIndex < CLASSES.size(); classIndex++) {
				QuestTransition turnIn = rewardTurnInRoute(definition, CLASSES.get(classIndex), classIndex, action);
				assertEquals(finalTurnInActions(CLASS_WEAPONS.get(classIndex)), turnIn.actions());
			}
		}
	}

	@Test
	void finalTurnInConsumesTheHelmetStackWhenPresent() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestTransition turnIn = rewardTurnInRoute(definition, PlayerClass.GLADIATOR, 0, LAST_REWARD_ACTION);

		QuestMutationPlan plan = plan(definition, turnIn, PlayerClass.GLADIATOR, Map.of(WORK_ITEM, 3));
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(finalTurnInActions(CLASS_WEAPONS.getFirst()), plan.requiredActions());
	}

	@Test
	void rewardPreviewOpensPageFiveForObjectAndRewardActions() throws Exception {
		CompiledQuestDefinition definition = load();
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = previewRoute(definition, action);
			assertEquals(List.of(), preview.conditions());
			assertEquals(List.of(), preview.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}
	}

	private static List<QuestAction> finalTurnInActions(int classWeapon) {
		return List.of(
			new QuestAction.GrantReward("ITEM", classWeapon, 1),
			new QuestAction.GrantReward("EXP", 0, 8325278, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("GOLD", 0, 4000, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", REWARD_ITEM, 1),
			new QuestAction.RemoveItem(WORK_ITEM, QuestAction.RemoveItem.ALL),
			new QuestAction.CompleteQuest(1));
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 2947 node: " + label));
	}

	private static List<AfterCommitAction> finalTurnInAfterCommit() {
		return List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition definition, QuestTransition transition,
			PlayerClass playerClass, Map<Integer, Integer> inventory) {
		int packed = definition.definition().progressLayout().pack(Map.of("var0", 9, "var4", 10));
		QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) transition.event();
		return QuestMutationPlanner.plan(definition,
			new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packed, inventory)
				.withPlayerClass(playerClass), event, transition)
			.orElseThrow(() -> new AssertionError("quest 2947 final turn-in produced no mutation plan for "
				+ playerClass + " action " + event.dialogId()));
	}

	private static QuestTransition rewardTurnInRoute(CompiledQuestDefinition definition, PlayerClass playerClass,
			int priority, int action) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(REWARD_NPC, action);
		List<QuestTransition> routes = definition.definition().transitions().stream()
			.filter(transition -> "s9".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& QuestEvent.matches(transition.event(), event)
				&& transition.conditions().contains(new QuestCondition.AdvancedClassIs(playerClass)))
			.toList();
		assertEquals(1, routes.size(), "s9 -> complete route count for " + playerClass + " action " + action);
		assertEquals(priority, routes.getFirst().priority());
		return routes.getFirst();
	}

	private static QuestTransition previewRoute(CompiledQuestDefinition definition, QuestDialogAction action) {
		List<QuestTransition> routes = definition.definition().transitions().stream()
			.filter(transition -> "s9".equals(transition.sourceNode())
				&& "s9".equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(REWARD_NPC, action.id())))
			.toList();
		assertEquals(1, routes.size(), "quest 2947 reward preview route count for " + action);
		return routes.getFirst();
	}

	private static CompiledQuestDefinition load() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/2947.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
