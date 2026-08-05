package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMinionTutorialRetailAlignmentTest {
	@Test
	void tutorialOwnersUseTheRetailQuestWorkItemsAndItemPlayLifecycle() {
		assertTutorial(19900, 190080020, 836073);
		assertTutorial(29900, 190080021, 836074);
	}

	private static void assertTutorial(int questId, int workItemId, int npcId) {
		QuestDefinition definition = load(questId).definition();

		assertTrue(definition.metadata().itemRequirements().isEmpty());
		assertEquals(List.of(new QuestItemRequirement(workItemId, 1)),
			definition.metadata().questWorkItems());

		QuestTransition start = definition.transitions().stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started")
				&& t.event() instanceof QuestEvent.LevelUp)
			.findFirst().orElseThrow();
		assertTrue(start.actions().contains(new QuestAction.GiveItem(workItemId, 1)));

		QuestTransition itemPlay = definition.transitions().stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.ItemPlay play
				&& play.itemId() == workItemId)
			.findFirst().orElseThrow();
		assertEquals(1500, ((QuestEvent.ItemPlay) itemPlay.event()).animationMillis());
		assertEquals(List.of(new QuestAction.RemoveItem(workItemId, 1)), itemPlay.actions());
		assertFalse(definition.transitions().stream().anyMatch(t ->
			t.event() instanceof QuestEvent.UseItem use && use.itemId() == workItemId));

		assertTrue(definition.transitions().stream().anyMatch(t ->
			t.sourceNode().equals("unaccepted") && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(31).equals(talk.dialogId())));
		assertTrue(definition.nodes().stream()
			.filter(n -> n.label().equals("reward") || n.label().equals("complete"))
			.allMatch(n -> n.projection().variables().get("var0") == 0));
		assertEquals(QuestStatus.REWARD, definition.nodes().stream()
			.filter(n -> n.label().equals("reward"))
			.findFirst().orElseThrow().projection().status());
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestMinionTutorialRetailAlignmentTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("failed to load " + resource, e);
		}
	}
}
