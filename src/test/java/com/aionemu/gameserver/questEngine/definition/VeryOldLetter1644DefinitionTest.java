package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeryOldLetter1644DefinitionTest {
	@Test
	void directoryAndProductionCatalogExposeTheMigratedQuest() {
		ClassLoader loader = getClass().getClassLoader();

		assertTrue(QuestDefinitionDirectoryLoader.compile(loader).find(1644).isPresent());
		assertTrue(QuestDefinitionCatalogManifest.compile(loader).find(1644).isPresent());
	}

	@Test
	void advancesThroughTheLetterAndUsesNpcSelectionDialogAfterEachStep() {
		CompiledQuestDefinition compiled = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader())
			.find(1644).orElseThrow();
		List<QuestTransition> transitions = compiled.definition().transitions();

		QuestTransition firstStep = talk(transitions, "started", 204545, 10000, "v1");
		QuestTransition secondStep = talk(transitions, "v1", 204537, 10001, "v2");
		QuestTransition finalStep = talk(transitions, "v2", 204545, 10002, "reward");
		QuestTransition completion = talk(transitions, "reward", 204546, 8, "complete");

		assertTrue(firstStep.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10)));
		assertTrue(secondStep.actions().contains(new QuestAction.GiveItem(182201765, 1)));
		assertTrue(secondStep.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10)));
		assertTrue(finalStep.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10)));
		assertTrue(completion.actions().contains(new QuestAction.RemoveItem(182201765, QuestAction.RemoveItem.ALL)));
		assertEquals(0, compiled.definition().nodes().stream()
			.filter(node -> "complete".equals(node.label()))
			.findFirst().orElseThrow().projection().variables().get("var0"));
		for (String source : List.of("started", "v1", "v2", "reward", "complete")) {
			QuestTransition blocker = transitions.stream()
				.filter(transition -> source.equals(transition.sourceNode())
					&& source.equals(transition.targetNode())
					&& transition.event().equals(new QuestEvent.UseItem(182201765)))
				.findFirst().orElseThrow();
			assertTrue(blocker.actions().contains(new QuestAction.BlockDefaultItemUse()));
		}
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			int dialogId, String target) {
		return transitions.stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& target.equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}
}
