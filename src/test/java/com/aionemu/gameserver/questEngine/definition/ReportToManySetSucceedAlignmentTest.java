package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportToManySetSucceedAlignmentTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void clientSetSucceedRoutesPreserveExistingProgressionSemantics() throws Exception {
		for (AliasQuest quest : List.of(
			new AliasQuest(29683, "started", 806710, 1009, null),
			new AliasQuest(35055, "started", 804874, 10000, 0),
			new AliasQuest(35056, "started", 804704, 10000, null),
			new AliasQuest(35057, "started", 804709, 10000, null),
			new AliasQuest(45055, "started", 804902, 1352, null),
			new AliasQuest(45056, "started", 804729, 10000, null),
			new AliasQuest(45057, "started", 804924, 10000, null))) {
			QuestDefinition definition = compile(quest.id());
			QuestTransition reference = route(definition, quest.source(), quest.npcId(), quest.referenceDialogId(),
				quest.priority());
			QuestTransition setSucceed = route(definition, quest.source(), quest.npcId(), 10255, quest.priority());

			assertEquals(reference.sourceNode(), setSucceed.sourceNode(), "quest " + quest.id() + " source");
			assertEquals(reference.targetNode(), setSucceed.targetNode(), "quest " + quest.id() + " target");
			assertEquals(reference.conditions(), setSucceed.conditions(), "quest " + quest.id() + " conditions");
			assertEquals(reference.actions(), setSucceed.actions(), "quest " + quest.id() + " actions");
			assertEquals(reference.afterCommit(), setSucceed.afterCommit(), "quest " + quest.id() + " after commit");
		}
	}

	@Test
	void flattenedReportToManyQuestsUseOrderedNpcSteps() throws Exception {
		for (OrderedQuest quest : List.of(
			new OrderedQuest(1876, 278501, 278501, List.of(
				new Step(278502, 1352), new Step(278503, 1693))),
			new OrderedQuest(2876, 278001, 278001, List.of(
				new Step(278016, 1352), new Step(278017, 1693))),
			new OrderedQuest(11323, 798928, 798928, List.of(
				new Step(702725, 1352), new Step(702727, 1693), new Step(702743, 2034),
				new Step(702744, 2375), new Step(702745, 2716))),
			new OrderedQuest(15401, 804699, 805351, List.of(new Step(804782, 1352))),
			new OrderedQuest(15402, 805351, 805352, List.of(
				new Step(805352, 1352), new Step(805381, 1693), new Step(805382, 2034))),
			new OrderedQuest(18970, 804709, 805215, List.of(new Step(804865, 1352))),
			new OrderedQuest(21323, 799226, 799226, List.of(
				new Step(702726, 1352), new Step(702746, 1693), new Step(702748, 2034),
				new Step(702747, 2375), new Step(702728, 2716))),
			new OrderedQuest(25000, 804718, 804718, List.of(
				new Step(804753, 1352), new Step(804990, 1693), new Step(805000, 2034))),
			new OrderedQuest(25401, 804719, 805356, List.of(new Step(804753, 1352))),
			new OrderedQuest(25402, 805356, 805357, List.of(
				new Step(805357, 1352), new Step(805404, 1693), new Step(805405, 2034))),
			new OrderedQuest(28970, 804927, 805218, List.of(new Step(804924, 1352))))) {
			assertOrderedQuest(quest);
		}
	}

	private static void assertOrderedQuest(OrderedQuest quest) throws Exception {
		QuestDefinition definition = compile(quest.id());
		assertEquals(List.of(quest.startNpcId()), dialogNpcs(definition, "unaccepted", 31),
			"quest " + quest.id() + " start NPCs");
		assertEquals(List.of(quest.endNpcId()), completionNpcs(definition),
			"quest " + quest.id() + " completion NPCs");

		for (int index = 0; index < quest.steps().size(); index++) {
			Step step = quest.steps().get(index);
			boolean last = index == quest.steps().size() - 1;
			String source = index == 0 ? "started" : "s" + index;
			String target = last ? "reward" : "s" + (index + 1);
			int progressDialogId = last ? 10255 : 10000 + index;

			QuestTransition open = route(definition, source, step.npcId(), 31, null);
			assertEquals(source, open.targetNode(), "quest " + quest.id() + " step " + index + " open target");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(step.pageId())), open.afterCommit(),
				"quest " + quest.id() + " step " + index + " page");

			QuestTransition progress = route(definition, source, step.npcId(), progressDialogId, null);
			assertEquals(target, progress.targetNode(), "quest " + quest.id() + " step " + index + " target");
			if (last) {
				assertTrue(progress.actions().isEmpty(), "quest " + quest.id() + " final actions");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestSelectionDialog(10)), progress.afterCommit(),
					"quest " + quest.id() + " final protocol");
			} else {
				assertEquals(List.of(new QuestAction.SetVariable("var0", index + 1)), progress.actions(),
					"quest " + quest.id() + " step " + index + " variable");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestSelectionDialog(10)), progress.afterCommit(),
					"quest " + quest.id() + " step " + index + " protocol");
			}
		}
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId, int dialogId,
			Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> priority == null || priority.equals(transition.priority()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		return routes.getFirst();
	}

	private static List<Integer> dialogNpcs(QuestDefinition definition, String source, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static List<Integer> completionNpcs(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()) && "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private record AliasQuest(int id, String source, int npcId, int referenceDialogId, Integer priority) {
	}

	private record OrderedQuest(int id, int startNpcId, int endNpcId, List<Step> steps) {
	}

	private record Step(int npcId, int pageId) {
	}
}
