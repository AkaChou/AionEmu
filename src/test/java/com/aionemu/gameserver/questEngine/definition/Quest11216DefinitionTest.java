package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest11216DefinitionTest {
	@Test
	void starInteractionsAreGatedAndIdempotent() {
		CompiledQuestDefinition definition = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader())
			.find(11216).orElseThrow();

		Set<Integer> canActNpcIds = definition.definition().transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.CanAct.class::isInstance)
			.map(QuestEvent.CanAct.class::cast)
			.filter(event -> "ACTION_ITEM_USE".equals(event.actionType()))
			.map(QuestEvent.CanAct::templateId)
			.collect(Collectors.toSet());
		assertEquals(Set.of(700624, 700625, 700626, 700627), canActNpcIds);

		Map<Integer, Long> useObjectRoutes = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == -1
				&& Set.of(700624, 700625, 700626, 700627).contains(talk.npcId()))
			.collect(Collectors.groupingBy(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId(),
				Collectors.counting()));
		assertEquals(Map.of(700624, 2L, 700625, 2L, 700626, 2L, 700627, 2L), useObjectRoutes);
		assertTrue(definition.definition().transitions().stream()
			.anyMatch(transition -> transition.actions().stream().anyMatch(QuestAction.GiveItem.class::isInstance)));
	}
}
