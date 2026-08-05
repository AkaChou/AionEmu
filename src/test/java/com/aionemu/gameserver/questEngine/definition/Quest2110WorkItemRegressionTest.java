package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest2110WorkItemRegressionTest {
	@Test
	void preservesLegacyReportToWorkItemLifecycle() {
		CompiledQuestDefinition definition = load();
		assertEquals(List.of(new QuestItemRequirement(182203110, 1)),
			definition.definition().metadata().questWorkItems());

		List<QuestTransition> starts = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203522 && transition.sourceNode().equals("unaccepted")
				&& transition.targetNode().equals("started"))
			.toList();
		assertEquals(2, starts.size());
		assertTrue(starts.stream().allMatch(transition -> transition.actions()
			.contains(new QuestAction.GiveItem(182203110, 1))));

		QuestTransition report = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203533 && talk.dialogId() == 1009)
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD,
			definition.definition().nodes().stream().filter(node -> node.label().equals("reward"))
			.findFirst().orElseThrow().projection().status());
		assertTrue(report.conditions().contains(new QuestCondition.HasItem(182203110, 1)));
		assertTrue(report.actions().contains(new QuestAction.RemoveItem(182203110, 1)));
		assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("started") && transition.targetNode().equals("started")
				&& transition.event().equals(new QuestEvent.TalkToNpc(203533, 1009))
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10))));
	}

	private static CompiledQuestDefinition load() {
		try (InputStream input = Objects.requireNonNull(
			Quest2110WorkItemRegressionTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/2110.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load quest 2110", e);
		}
	}
}
