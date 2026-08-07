package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest80787To80794RetailAlignmentTest {
	private static final Map<Integer, Integer> QUEST_NPCS = Map.of(
		80787, 833671,
		80788, 833672,
		80789, 833671,
		80790, 833672,
		80791, 833673,
		80792, 833674,
		80793, 833673,
		80794, 833674);

	@Test
	void preservesGivingRotundStartDialogFromLegacyDefinitions() throws Exception {
		for (Map.Entry<Integer, Integer> entry : QUEST_NPCS.entrySet()) {
			QuestDefinition definition = load(entry.getKey());
			QuestTransition startDialog = dialogRoute(definition, "unaccepted", entry.getValue(), 31);
			assertEquals("unaccepted", startDialog.targetNode());
			assertEquals(java.util.List.of(new AfterCommitAction.ShowQuestDialog(4762)),
				startDialog.afterCommit());
		}
	}

	@Test
	void opensGivingRotundReportDialogWhenAcceptedQuestNpcIsTalkedToAgain() throws Exception {
		for (Map.Entry<Integer, Integer> entry : QUEST_NPCS.entrySet()) {
			QuestTransition reportDialog = dialogRoute(load(entry.getKey()), "started", entry.getValue(), 31);

			assertEquals("started", reportDialog.targetNode());
			assertEquals(java.util.List.of(new AfterCommitAction.ShowQuestDialog(10002)),
				reportDialog.afterCommit());
		}
	}

	private static QuestTransition dialogRoute(QuestDefinition definition, String source, int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.findFirst()
			.orElseThrow();
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest80787To80794RetailAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new AssertionError("missing quest " + questId + " resource");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
