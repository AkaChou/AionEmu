package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest14026ClientDialogAlignmentTest {
	private static final int KIMEIA_NPC_ID = 204044;
	private static final int LEGACY_REMEDY_ITEM_ID = 182201013;

	@Test
	void finalDefenseTurnInRemainsReachableWhenLegacyRemedyIsAbsent() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();

		QuestTransition page = transition(definition, "defense-done", "defense-done",
			new QuestEvent.TalkToNpc(KIMEIA_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			page.afterCommit());

		QuestTransition finalDefenseTurnIn = transition(definition, "defense-done", "reward",
			new QuestEvent.TalkToNpc(KIMEIA_NPC_ID, QuestDialogAction.SETPRO4.id()));
		assertEquals(List.of(new QuestAction.RemoveItem(LEGACY_REMEDY_ITEM_ID, QuestAction.RemoveItem.ALL)),
			finalDefenseTurnIn.actions());
		assertEquals(List.of(
			new AfterCommitAction.TeleportPlayer(210020000, 271.69f, 2787.04f, 272.47f, (byte) 50),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), finalDefenseTurnIn.afterCommit());

		int packed = definition.progressLayout().pack(Map.of("var0", 4));
		QuestSnapshot snapshot = new QuestSnapshot(7, 14026, QuestStatus.START, packed, Map.of());
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(KIMEIA_NPC_ID, QuestDialogAction.SETPRO4.id()), finalDefenseTurnIn)
			.orElseThrow();

		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(List.of(new QuestAction.RemoveItem(LEGACY_REMEDY_ITEM_ID, QuestAction.RemoveItem.ALL)),
			plan.requiredActions());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/14026.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14026.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
