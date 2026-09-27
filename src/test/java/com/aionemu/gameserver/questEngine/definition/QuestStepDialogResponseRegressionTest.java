package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestStepDialogResponseRegressionTest {
	@Test
	void stepDialogsPreserveTheirLegacyTerminalResponses() {
		QuestTransition elimMessage = route(1115, "started", "v1", 203072, 10000);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), elimMessage.afterCommit());

		QuestTransition undeliveredArmor = route(1131, "started", "shugo", 799093, 10000);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), undeliveredArmor.afterCommit());

		QuestTransition villageSeal = route(1158, "started", "k1", 700003, 10000);
		assertTrue(villageSeal.actions().contains(new QuestAction.GiveItem(182200502, 1)));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), villageSeal.afterCommit());
	}

	private static QuestTransition route(int questId, String source, String target, int npcId, int dialogId) {
		CompiledQuestDefinition compiled = definition(questId);
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}
}
