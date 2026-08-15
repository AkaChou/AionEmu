package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMinionTutorialRetailAlignmentTest {
	@Test
	void tutorialOwnersUseTheRetailQuestWorkItemsAndItemPlayLifecycle() {
		assertTutorial(19900, 1007, 190080020, 836073);
		assertTutorial(29900, 2009, 190080021, 836074);
	}

	private static void assertTutorial(int questId, int prerequisiteId, int workItemId, int npcId) {
		CompiledQuestDefinition compiled = load(questId);
		QuestDefinition definition = compiled.definition();

		assertTrue(definition.metadata().itemRequirements().isEmpty());
		assertEquals(Set.of(prerequisiteId), definition.metadata().prerequisites());
		assertTrue(definition.metadata().startConditions().stream()
			.noneMatch(condition -> condition.questId() == prerequisiteId));
		assertEquals(List.of(new QuestReward("ITEM", 190080012, 1)), definition.metadata().rewards());
		assertEquals(List.of(new QuestItemRequirement(workItemId, 1)),
			definition.metadata().questWorkItems());

		assertStartTransition(definition, new QuestEvent.LevelUp(), workItemId);
		assertStartTransition(definition, new QuestEvent.EnterWorld(), workItemId);

		QuestTransition itemPlay = definition.transitions().stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.ItemPlay play
				&& play.itemId() == workItemId)
			.findFirst().orElseThrow();
		assertEquals(1500, ((QuestEvent.ItemPlay) itemPlay.event()).animationMillis());
		assertTrue(itemPlay.actions().isEmpty());
		var completedContractPlan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of()), itemPlay.event(), itemPlay)
			.orElseThrow();
		assertEquals(QuestStatus.REWARD, completedContractPlan.nextStatus());
		assertEquals(1, completedContractPlan.nextPackedVariables());
		assertTrue(completedContractPlan.requiredActions().isEmpty());
		assertFalse(definition.transitions().stream().anyMatch(t ->
			t.event() instanceof QuestEvent.UseItem use && use.itemId() == workItemId));

		assertTrue(definition.transitions().stream().anyMatch(t ->
			t.sourceNode().equals("unaccepted") && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(31).equals(talk.dialogId())));
		assertTrue(definition.nodes().stream()
			.filter(n -> n.label().equals("reward") || n.label().equals("complete"))
			.allMatch(n -> n.projection().variables().get("var0") == 1));
		assertEquals(0, definition.nodes().stream()
			.filter(n -> n.label().equals("legacy-reward"))
			.findFirst().orElseThrow().projection().variables().get("var0"));
		assertEquals(QuestStatus.REWARD, definition.nodes().stream()
			.filter(n -> n.label().equals("reward"))
			.findFirst().orElseThrow().projection().status());
	}

	private static void assertStartTransition(QuestDefinition definition, QuestEvent event, int workItemId) {
		QuestTransition start = definition.transitions().stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started")
				&& t.event().equals(event))
			.findFirst().orElseThrow();
		assertTrue(start.actions().contains(new QuestAction.GiveItem(workItemId, 1)));
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
