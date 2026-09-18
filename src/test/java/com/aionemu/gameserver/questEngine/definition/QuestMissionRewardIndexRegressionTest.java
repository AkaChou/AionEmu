package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 锁定两项守卫使命的单档奖励与组内装备选择，保留称号及完成副作用；只编译生产 XML，不启动服务。
 * Locks single reward group and equipment choices for both defense missions, preserving titles and completion effects.
 */
class QuestMissionRewardIndexRegressionTest {

	@ParameterizedTest
	@ValueSource(ints = {14026, 24026})
	void keepsMetadataRewardsAndTheLegacyRewardPreview(int questId) throws Exception {
		QuestDefinition definition = load(questId);
		List<QuestReward> expected = expectedRewards(questId);
		assertEquals(expected, definition.metadata().rewards());
		assertEquals(List.of("reward"), definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD)
			.map(QuestNode::label).toList());
		assertNode(definition, "reward", QuestStatus.REWARD);
		assertNode(definition, "complete", QuestStatus.COMPLETE);

		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT,
				QuestDialogAction.USE_OBJECT, QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = route(definition, questId, action);
			assertEquals("reward", preview.targetNode());
			assertEquals(List.of(), preview.conditions());
			assertEquals(List.of(), preview.actions());
			QuestDialogPage page = action == QuestDialogAction.QUEST_SELECT
				? QuestDialogPage.SELECT5 : QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1;
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), preview.afterCommit());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {14026, 24026})
	void everyEquipmentChoiceCompletesTierZeroWithItsTitleAndFixedRewards(int questId) throws Exception {
		QuestDefinition definition = load(questId);
		List<QuestDialogAction> choices = List.of(
			QuestDialogAction.SELECTED_QUEST_REWARD1, QuestDialogAction.SELECTED_QUEST_REWARD2,
			QuestDialogAction.SELECTED_QUEST_REWARD3, QuestDialogAction.SELECTED_QUEST_REWARD4,
			QuestDialogAction.SELECTED_QUEST_REWARD5, QuestDialogAction.SELECTED_QUEST_REWARD6);
		assertEquals(6, definition.transitions().stream()
			.filter(transition -> transition.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance))
			.count());
		assertEquals(9, definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")).count());

		for (int choice = 0; choice < choices.size(); choice++) {
			QuestTransition completion = route(definition, questId, choices.get(choice));
			assertEquals("complete", completion.targetNode());
			assertEquals(List.of(), completion.conditions());
			List<QuestAction> expected = completionActions(expectedRewards(questId), selectedItems(questId).get(choice));
			assertEquals(expected, completion.actions(), "quest=" + questId + " choice=" + (choice + 1));
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}
	}

	private static List<QuestAction> completionActions(List<QuestReward> rewards, int selectedItem) {
		List<QuestAction> actions = new ArrayList<>();
		for (QuestReward reward : rewards) {
			if (reward.kind().equals("SELECTABLE_ITEM") && reward.id() != selectedItem) {
				continue;
			}
			String kind = reward.kind().equals("SELECTABLE_ITEM") ? "ITEM" : reward.kind();
			QuestRewardAmountMode mode = kind.equals("EXP")
				? QuestRewardAmountMode.QUEST_BASE : QuestRewardAmountMode.EXACT;
			actions.add(new QuestAction.GrantReward(kind, reward.id(), reward.amount(), mode));
		}
		actions.add(new QuestAction.CompleteQuest(0));
		return List.copyOf(actions);
	}

	private static List<QuestReward> expectedRewards(int questId) {
		List<QuestReward> rewards = new ArrayList<>();
		rewards.add(new QuestReward("EXP", 0, 3504765));
		rewards.add(new QuestReward("TITLE", questId == 14026 ? 12 : 60, 1));
		for (int itemId : selectedItems(questId)) {
			rewards.add(new QuestReward("SELECTABLE_ITEM", itemId, 1));
		}
		if (questId == 14026) {
			rewards.add(new QuestReward("ITEM", 188053407, 3));
			rewards.add(new QuestReward("ITEM", 186000003, 40));
		} else {
			rewards.add(new QuestReward("ITEM", 186000008, 20));
		}
		rewards.add(new QuestReward("ITEM", 190200000, 10));
		return List.copyOf(rewards);
	}

	private static List<Integer> selectedItems(int questId) {
		return switch (questId) {
			case 14026 -> List.of(110101843, 110301826, 110301828, 110551154, 110551156, 110601629);
			case 24026 -> List.of(114101704, 114301833, 114301835, 114501742, 114501744, 114601583);
			default -> throw new IllegalArgumentException("unexpected quest " + questId);
		};
	}

	private static QuestTransition route(QuestDefinition definition, int questId, QuestDialogAction action) {
		QuestEvent event = new QuestEvent.TalkToNpc(questId == 14026 ? 203901 : 204301, action.id());
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward") && transition.event().equals(event))
			.toList();
		assertEquals(1, routes.size(), "quest=" + questId + " action=" + action);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label)).findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", 4), node.projection().variables());
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestMissionRewardIndexRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, "missing production quest " + questId);
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
