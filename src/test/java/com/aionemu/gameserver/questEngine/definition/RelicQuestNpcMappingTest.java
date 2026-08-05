package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelicQuestNpcMappingTest {
	private static final Map<Integer, Set<Integer>> EXPECTED_NPCS = Map.ofEntries(
		Map.entry(18849, Set.of(831252, 831382)),
		Map.entry(18850, Set.of(831210, 831413)),
		Map.entry(21281, Set.of(799949, 205634)),
		Map.entry(21282, Set.of(799950, 205635)),
		Map.entry(21283, Set.of(799951, 205636)),
		Map.entry(21284, Set.of(799952, 205637)),
		Map.entry(21285, Set.of(799957, 205621)),
		Map.entry(21286, Set.of(799958, 205622)),
		Map.entry(21287, Set.of(799959, 205623)),
		Map.entry(21288, Set.of(799960, 205624)),
		Map.entry(28849, Set.of(831253, 831383)),
		Map.entry(28850, Set.of(831235, 831414))
	);

	@Test
	void completedRelicDefinitionsUseAuthoritativeExchangeNpcMappings() throws Exception {
		ClassLoader loader = getClass().getClassLoader();
		for (var entry : EXPECTED_NPCS.entrySet()) {
			String resource = "aion/data/static_data/quest_definition/quests/" + entry.getKey() + ".xml";
			try (InputStream input = loader.getResourceAsStream(resource)) {
				assertNotNull(input, resource);
				CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(input);
				Set<Integer> npcIds = definition.definition().transitions().stream()
					.map(QuestTransition::event)
					.filter(QuestEvent.TalkToNpc.class::isInstance)
					.map(QuestEvent.TalkToNpc.class::cast)
					.map(QuestEvent.TalkToNpc::npcId)
					.collect(Collectors.toSet());
				assertEquals(entry.getValue(), npcIds, "quest " + entry.getKey());
			}
		}
	}
}
