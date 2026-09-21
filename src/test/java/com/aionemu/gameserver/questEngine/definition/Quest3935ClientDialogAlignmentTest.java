package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 3935 的客户端对话顺序、物品交付、最终交任务提示和奖励握手。
 * Verifies quest 3935's dialog order, item delivery, final turn-in prompt, and reward handshake.
 */
class Quest3935ClientDialogAlignmentTest {
	private static final int REWARD_NPC_ID = 203701;
	private static final int ITEM_NPC_ID = 203329;
	private static final int SENTINEL_NPC_ID = 203752;
	private static final int FEATHER_ID = 186000078;
	private static final int OATH_STONE_ID = 186000080;
	private static final List<Integer> REWARD_ITEMS = List.of(
		112100779, 112300776, 112500764, 112600775, 112301407);
	private static final List<Stage> STAGES = List.of(
		new Stage(1, 203316, QuestDialogPage.SELECT1, QuestDialogAction.SELECT1_1, QuestDialogAction.SETPRO1),
		new Stage(2, 203702, QuestDialogPage.SELECT2, QuestDialogAction.SELECT2_1, QuestDialogAction.SETPRO2),
		new Stage(3, ITEM_NPC_ID, QuestDialogPage.SELECT3, QuestDialogAction.SELECT3_1, QuestDialogAction.SETPRO3));

	@Test
	void orderedStagesProjectTheLegacyVar0Sequence() throws Exception {
		CompiledQuestDefinition definition = load();

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		for (Stage stage : STAGES) {
			assertNode(definition, "s" + stage.step(), QuestStatus.START, stage.step());
		}
		assertNode(definition, "s4", QuestStatus.START, 4);
		assertNode(definition, "reward", QuestStatus.REWARD, 4);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		for (Stage stage : STAGES) {
			String source = stage.step() == 1 ? "started" : "s" + (stage.step() - 1);
			String target = "s" + stage.step();
			List<QuestCondition> expectedCondition = List.of(
				new QuestCondition.QuestVariableIs("var0", stage.step() - 1));

			QuestTransition open = route(definition, source, source, stage.npcId(), QuestDialogAction.QUEST_SELECT);
			assertEquals(expectedCondition, open.conditions(), "stage " + stage.step() + " open condition");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(stage.page().id())), open.afterCommit(),
				"stage " + stage.step() + " open page");

			QuestTransition response = route(definition, source, source, stage.npcId(), stage.response());
			assertEquals(expectedCondition, response.conditions(), "stage " + stage.step() + " response condition");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(stage.responsePage().id())),
				response.afterCommit(), "stage " + stage.step() + " response page");

			QuestTransition progress = route(definition, source, target, stage.npcId(), stage.progress());
			assertEquals(expectedCondition, progress.conditions(), "stage " + stage.step() + " progress condition");
			assertEquals(List.of(new QuestAction.SetVariable("var0", stage.step())), progress.actions(),
				"stage " + stage.step() + " progress action");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), progress.afterCommit(),
				"stage " + stage.step() + " progress response");
		}
	}

	@Test
	void itemDeliveryRequiresTenFeathersAndKeepsTheFailurePage() throws Exception {
		CompiledQuestDefinition definition = load();

		QuestTransition success = route(definition, "s3", "s4", ITEM_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(0, success.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 3),
			new QuestCondition.HasItem(FEATHER_ID, 10)), success.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(FEATHER_ID, 10),
			new QuestAction.SetVariable("var0", 4)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), success.afterCommit());

		QuestTransition failure = route(definition, "s3", "s3", ITEM_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(1, failure.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 3),
			new QuestCondition.HasItem(FEATHER_ID, 10, false)), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			failure.afterCommit());
	}

	@Test
	void finalSentinelRequiresTheOathStoneAndPreservesMissingItemPage() throws Exception {
		CompiledQuestDefinition definition = load();
		List<QuestCondition> stageCondition = List.of(new QuestCondition.QuestVariableIs("var0", 4));

		QuestTransition open = route(definition, "s4", "s4", SENTINEL_NPC_ID, QuestDialogAction.QUEST_SELECT);
		assertEquals(stageCondition, open.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())), open.afterCommit());

		QuestTransition response = route(definition, "s4", "s4", SENTINEL_NPC_ID, QuestDialogAction.SELECT5_1);
		assertEquals(stageCondition, response.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1.id())),
			response.afterCommit());

		QuestTransition success = routeWithOathStone(definition, 0, true);
		assertEquals("s4", success.sourceNode());
		assertEquals("reward", success.targetNode());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 4),
			new QuestCondition.HasItem(OATH_STONE_ID, 1)), success.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 4),
			new QuestAction.RemoveItem(OATH_STONE_ID, 1)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), success.afterCommit());

		QuestTransition failure = routeWithOathStone(definition, 1, false);
		assertEquals("s4", failure.sourceNode());
		assertEquals("s4", failure.targetNode());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 4),
			new QuestCondition.HasItem(OATH_STONE_ID, 1, false)), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_2.id())),
			failure.afterCommit());

		QuestTransition finish = route(definition, "s4", "s4", SENTINEL_NPC_ID, QuestDialogAction.FINISH_DIALOG);
		assertEquals(stageCondition, finish.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finish.afterCommit());
	}

	@Test
	void rewardOwnerShowsTheFinalPromptBeforeRewardSelection() throws Exception {
		CompiledQuestDefinition definition = load();

		assertTrue(definition.definition().transitions().stream()
			.noneMatch(transition -> transition.targetNode().equals("reward")
				// s4 是带誓约石条件的最终交付阶段，不属于需要阻断的中间阶段；
				// s4 is the oath-stone-guarded final delivery stage, not an intermediate stage to block.
				&& List.of("started", "s1", "s2", "s3").contains(transition.sourceNode())),
			"an intermediate stage must not enter REWARD");

		QuestTransition prompt = route(definition, "reward", "reward", REWARD_NPC_ID, QuestDialogAction.USE_OBJECT);
		assertEquals(List.of(), prompt.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			prompt.afterCommit());

		QuestTransition rewardWindow = route(definition, "reward", "reward", REWARD_NPC_ID,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(), rewardWindow.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardWindow.afterCommit());

		for (int index = 0; index < REWARD_ITEMS.size(); index++) {
			QuestDialogAction action = QuestDialogAction.fromId(QuestDialogAction.SELECTED_QUEST_REWARD1.id() + index);
			QuestTransition completion = route(definition, "reward", "complete", REWARD_NPC_ID, action);
			assertEquals(List.of(), completion.conditions(), "reward " + action + " conditions");
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 5390338, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", REWARD_ITEMS.get(index), 1, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), completion.actions(), "reward " + action);
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit(), "reward " + action + " response");
		}
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action) {
		List<QuestTransition> routes = definition.definition().transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()) && target.equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.toList();
		assertEquals(1, routes.size(), "route " + source + " -> " + target + " NPC " + npcId + " action " + action);
		return routes.getFirst();
	}

	private static QuestTransition routeWithOathStone(CompiledQuestDefinition definition, int priority,
			boolean expected) {
		return definition.definition().transitions().stream()
			.filter(transition -> "s4".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == SENTINEL_NPC_ID
				&& talk.dialogId() == QuestDialogAction.SET_SUCCEED.id())
			.filter(transition -> Objects.equals(priority, transition.priority()))
			.filter(transition -> transition.conditions().contains(new QuestCondition.HasItem(OATH_STONE_ID, 1, expected)))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing SET_SUCCEED route, priority " + priority
				+ ", expected item " + expected));
	}

	private static void assertNode(CompiledQuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.definition().nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), label + " status");
		assertEquals(Map.of("var0", var0), node.projection().variables(), label + " variables");
	}

	private static CompiledQuestDefinition load() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/3935.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record Stage(int step, int npcId, QuestDialogPage page, QuestDialogAction response,
			QuestDialogAction progress) {
		private QuestDialogPage responsePage() {
            switch (step) {
                case 1:
                    return QuestDialogPage.SELECT1_1;
                case 2:
                    return QuestDialogPage.SELECT2_1;
                case 3:
                    return QuestDialogPage.SELECT3_1;
                default:
                    throw new IllegalArgumentException("unsupported stage " + step);
            }
		}
	}
}
