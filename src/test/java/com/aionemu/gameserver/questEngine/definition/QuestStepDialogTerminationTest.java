package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestStepDialogTerminationTest {
	@Test
	void npcStepRoutesAlwaysReturnAValidDialogResponse() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> missingDialogEnd = new ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			for (QuestTransition transition : compiled.definition().transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
					|| talk.dialogId() == null || talk.dialogId() < 10000 || talk.dialogId() > 10040
					|| intentionallyLeavesDialogToAnotherProtocol(compiled.id(), talk)) {
					continue;
				}
				boolean returnsDialogResponse = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.CloseDialog
						|| action instanceof AfterCommitAction.ShowQuestDialog
						|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
						|| action instanceof AfterCommitAction.ShowDialogWindow);
				if (!returnsDialogResponse) {
					missingDialogEnd.add(compiled.id() + ":" + transition.sourceNode()
						+ " npc=" + talk.npcId() + " dialog=" + talk.dialogId());
				}
			}
		}

		assertTrue(missingDialogEnd.isEmpty(),
			"NPC step routes must return a valid dialog response: " + missingDialogEnd);
	}

	private static boolean intentionallyLeavesDialogToAnotherProtocol(int questId, QuestEvent.TalkToNpc talk) {
		int dialogId = talk.dialogId();
		return switch (questId) {
			case 1006 -> talk.npcId() == 790001 && (dialogId == 10000 || dialogId >= 10004 && dialogId <= 10014);
			case 1929 -> talk.npcId() == 205110 && dialogId == 10003;
			case 2008 -> talk.npcId() == 203550 && dialogId >= 10004 && dialogId <= 10014;
			case 20032 -> talk.npcId() == 799325 && dialogId == 10002;
			case 24030 -> talk.npcId() == 205020 && dialogId == 10005;
			default -> false;
		};
	}
}
