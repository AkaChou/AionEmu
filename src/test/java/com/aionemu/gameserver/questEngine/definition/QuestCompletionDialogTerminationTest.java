package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestCompletionDialogTerminationTest {
	@Test
	void rewardOwnersAlwaysAcceptLegacyPreviewDialogs() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> missingPreview = new ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			for (QuestTransition completion : compiled.definition().transitions()) {
				if (!(completion.event() instanceof QuestEvent.TalkToNpc talk)
					|| talk.dialogId() == null || talk.dialogId() < 8 || talk.dialogId() > 23
					|| completion.sourceNode() == null || completion.targetNode() == null
					|| !completion.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance)
					|| !isRewardNode(compiled, completion.sourceNode())) {
					continue;
				}
				for (int previewDialog : List.of(-1, 1009)) {
					boolean present = compiled.definition().transitions().stream().anyMatch(preview ->
						completion.sourceNode().equals(preview.sourceNode())
							&& preview.event() instanceof QuestEvent.TalkToNpc previewTalk
							&& previewTalk.npcId() == talk.npcId()
							&& Integer.valueOf(previewDialog).equals(previewTalk.dialogId())
							&& (preview.afterCommit().stream()
								.anyMatch(AfterCommitAction.ShowQuestDialog.class::isInstance)
								|| preview.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance)));
					if (!present) {
						String missing = compiled.id() + ":" + completion.sourceNode()
							+ " npc=" + talk.npcId() + " dialog=" + previewDialog;
						if (!missingPreview.contains(missing)) {
							missingPreview.add(missing);
						}
					}
				}
			}
		}

		assertTrue(missingPreview.isEmpty(),
			"REWARD turn-ins must accept legacy preview dialogs: " + missingPreview);
	}

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

	private static boolean isRewardNode(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream()
			.anyMatch(node -> node.label().equals(label)
				&& node.projection().status() == com.aionemu.gameserver.questEngine.model.QuestStatus.REWARD);
	}
}
