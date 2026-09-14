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
 * 验证任务 3934 的客户端对话顺序、最终交任务提示和奖励握手。
 * Verifies quest 3934's client dialog order, final turn-in prompt, and reward handshake.
 */
class Quest3934ClientDialogAlignmentTest {
	private static final int REWARD_NPC_ID = 203701;
	private static final int SENTINEL_NPC_ID = 203752;
	private static final int OATH_STONE_ID = 186000080;
	private static final List<Integer> REWARD_ITEMS = List.of(
		111100821, 111300825, 111500808, 111600800, 111301464);
	private static final List<Stage> STAGES = List.of(
		new Stage(1, 798359, QuestDialogPage.SELECT1, QuestDialogAction.SELECT1_1, QuestDialogAction.SETPRO1),
		new Stage(2, 798360, QuestDialogPage.SELECT2, QuestDialogAction.SELECT2_1, QuestDialogAction.SETPRO2),
		new Stage(3, 798361, QuestDialogPage.SELECT3, QuestDialogAction.SELECT3_1, QuestDialogAction.SETPRO3),
		new Stage(4, 798362, QuestDialogPage.SELECT4, QuestDialogAction.SELECT4_1, QuestDialogAction.SETPRO4),
		new Stage(5, 798363, QuestDialogPage.SELECT5, QuestDialogAction.SELECT5_1, QuestDialogAction.SETPRO5),
		new Stage(6, 798364, QuestDialogPage.SELECT6, QuestDialogAction.SELECT6_1, QuestDialogAction.SETPRO6),
		new Stage(7, 798365, QuestDialogPage.SELECT7, QuestDialogAction.SELECT7_1, QuestDialogAction.SETPRO7),
		new Stage(8, 798366, QuestDialogPage.SELECT8, QuestDialogAction.SELECT8_1, QuestDialogAction.SETPRO8));

	@Test
	void orderedStagesProjectTheLegacyVar0Sequence() throws Exception {
		CompiledQuestDefinition definition = load();

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		for (Stage stage : STAGES) {
			assertNode(definition, "s" + stage.step(), QuestStatus.START, stage.step());
		}
		assertNode(definition, "reward", QuestStatus.REWARD, 8);
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
	void finalSentinelRequiresTheOathStoneAndPreservesMissingItemPage() throws Exception {
		CompiledQuestDefinition definition = load();
		List<QuestCondition> stageCondition = List.of(new QuestCondition.QuestVariableIs("var0", 8));

		QuestTransition open = route(definition, "s8", "s8", SENTINEL_NPC_ID, QuestDialogAction.QUEST_SELECT);
		assertEquals(stageCondition, open.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT9.id())), open.afterCommit());

		QuestTransition response = route(definition, "s8", "s8", SENTINEL_NPC_ID, QuestDialogAction.SELECT9_1);
		assertEquals(stageCondition, response.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT9_1.id())), response.afterCommit());

		QuestTransition success = routeWithOathStone(definition, 0, true);
		assertEquals("s8", success.sourceNode());
		assertEquals("reward", success.targetNode());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 8),
			new QuestCondition.HasItem(OATH_STONE_ID, 1)), success.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 8),
			new QuestAction.RemoveItem(OATH_STONE_ID, 1)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), success.afterCommit());

		QuestTransition failure = routeWithOathStone(definition, 1, false);
		assertEquals("s8", failure.sourceNode());
		assertEquals("s8", failure.targetNode());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 8),
			new QuestCondition.HasItem(OATH_STONE_ID, 1, false)), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT9_2.id())),
			failure.afterCommit());

		QuestTransition finish = route(definition, "s8", "s8", SENTINEL_NPC_ID, QuestDialogAction.FINISH_DIALOG);
		assertEquals(stageCondition, finish.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finish.afterCommit());
	}

	@Test
	void rewardOwnerShowsTheFinalPromptBeforeRewardSelection() throws Exception {
		CompiledQuestDefinition definition = load();

		assertTrue(definition.definition().transitions().stream()
			.noneMatch(transition -> transition.targetNode().equals("reward")
				&& List.of("started", "s1", "s2", "s3", "s4", "s5", "s6", "s7").contains(transition.sourceNode())),
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
			.filter(transition -> "s8".equals(transition.sourceNode())
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
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/3934.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record Stage(int step, int npcId, QuestDialogPage page, QuestDialogAction response,
			QuestDialogAction progress) {
		private QuestDialogPage responsePage() {
			return switch (step) {
				case 1 -> QuestDialogPage.SELECT1_1;
				case 2 -> QuestDialogPage.SELECT2_1;
				case 3 -> QuestDialogPage.SELECT3_1;
				case 4 -> QuestDialogPage.SELECT4_1;
				case 5 -> QuestDialogPage.SELECT5_1;
				case 6 -> QuestDialogPage.SELECT6_1;
				case 7 -> QuestDialogPage.SELECT7_1;
				case 8 -> QuestDialogPage.SELECT8_1;
				default -> throw new IllegalArgumentException("unsupported stage " + step);
			};
		}
	}
}
