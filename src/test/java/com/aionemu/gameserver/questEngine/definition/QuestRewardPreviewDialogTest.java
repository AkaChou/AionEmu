package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRewardPreviewDialogTest {
	@Test
	void npcSelectableRewardsHandleTheSelectRewardPreview() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> missingPreview = new ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			List<QuestTransition> transitions = compiled.definition().transitions();
			for (QuestTransition legacyPreview : transitions) {
				String rewardNode = legacyPreview.sourceNode();
				if (!(legacyPreview.event() instanceof QuestEvent.TalkToNpc talk)
						|| talk.dialogId() == null || talk.dialogId() != -1
						|| rewardNode == null || !rewardNode.equals(legacyPreview.targetNode())) {
					continue;
				}
				boolean hasSelectableCompletion = transitions.stream().anyMatch(transition ->
					rewardNode.equals(transition.sourceNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc candidate
						&& candidate.npcId() == talk.npcId() && candidate.dialogId() != null
						&& candidate.dialogId() >= 8 && candidate.dialogId() <= 22
						&& transition.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
				if (!hasSelectableCompletion) {
					continue;
				}
				boolean hasSelectRewardPreview = transitions.stream().anyMatch(transition ->
					rewardNode.equals(transition.sourceNode())
						&& rewardNode.equals(transition.targetNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc candidate
						&& candidate.npcId() == talk.npcId() && candidate.dialogId() != null
						&& candidate.dialogId() == 1009
						&& transition.afterCommit().stream().anyMatch(action ->
							action instanceof AfterCommitAction.ShowQuestDialog page && page.dialogId() != 1009));
				if (!hasSelectRewardPreview) {
					missingPreview.add(compiled.id() + ":" + rewardNode + " npc=" + talk.npcId());
				}
			}
		}

		assertTrue(missingPreview.isEmpty(),
			"NPC selectable rewards must route SELECT_REWARD to a real preview page: " + missingPreview);
	}
}
