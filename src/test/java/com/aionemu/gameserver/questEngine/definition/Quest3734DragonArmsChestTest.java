package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Quest3734DragonArmsChestTest {
	private static final int DRAGON_ARMS_CHEST = 700415;

	@Test
	void dragonArmsChestUsesTheObjectRouteOnlyAfterAcceptance() {
		CompiledQuestDefinition definition = load(3734);
		QuestEvent event = new QuestEvent.CanAct(
			DRAGON_ARMS_CHEST, "ACTION_ITEM_USE");
		QuestTransition gate = route(definition, "started", "started", event);

		QuestNode startedNode = definition.definition().nodes().stream()
			.filter(node -> node.label().equals("started"))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.START, startedNode.projection().status());
		assertEquals(Map.of("var0", 0), startedNode.projection().variables());
		assertEquals(List.of(), gate.conditions());
		assertEquals(List.of(), gate.actions());
		assertEquals(List.of(), gate.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("unaccepted") && transition.event().equals(event)));
	}

	private static QuestTransition route(CompiledQuestDefinition definition,
		String source, String target, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/"
			+ questId + ".xml";
		try (InputStream input = Quest3734DragonArmsChestTest.class
			.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception exception) {
			throw new AssertionError("failed to load " + resource, exception);
		}
	}
}
