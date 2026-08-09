package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestCompletionDialogTerminationTest {
	@Test
	void npcRewardConfirmationRoutesAlwaysLeaveTheRewardDialog() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> missingDialogEnd = new ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			for (QuestTransition transition : compiled.definition().transitions()) {
				if (!transition.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance)
					|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)
					|| talk.dialogId() == null || talk.dialogId() < 8 || talk.dialogId() > 23) {
					continue;
				}
				boolean leavesRewardDialog = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.CloseDialog
						|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
						|| action instanceof AfterCommitAction.ShowDialogWindow window && window.dialogId() == 0);
				if (!leavesRewardDialog) {
					missingDialogEnd.add(compiled.id() + ":" + transition.sourceNode()
						+ " npc=" + talk.npcId() + " dialog=" + talk.dialogId());
				}
			}
		}

		assertTrue(missingDialogEnd.isEmpty(),
			"NPC reward completion routes must leave the reward confirmation dialog: " + missingDialogEnd);
	}
}
