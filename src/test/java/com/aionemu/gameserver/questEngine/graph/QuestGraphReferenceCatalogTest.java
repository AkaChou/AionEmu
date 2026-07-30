package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.SpawnsData2;
import com.aionemu.gameserver.questEngine.graph.QuestGraphReferenceCatalog.StaticSpawnReference;

import jakarta.xml.bind.JAXBContext;

class QuestGraphReferenceCatalogTest {

	@Test
	void staticSpawnReferencesPreserveWorldAndRequireCoordinateSpot() throws Exception {
		SpawnsData2 spawns = (SpawnsData2) JAXBContext.newInstance(SpawnsData2.class).createUnmarshaller().unmarshal(new StringReader("""
			<spawns>
				<spawn_map map_id="220050000">
					<spawn npc_id="700759"><spot x="1" y="2" z="3" h="4"/></spawn>
					<spawn npc_id="203709"/>
				</spawn_map>
				<spawn_map map_id="220050001">
					<spawn npc_id="700759"><spot x="5" y="6" z="7" h="8"/></spawn>
					<spawn npc_id="700760"><spot x="9" y="10" z="11" h="12"/></spawn>
				</spawn_map>
				<spawn_map map_id="220050002">
					<spawn npc_id="700761"><spot x="13" y="14" z="15" h="16"/></spawn>
				</spawn_map>
			</spawns>
			"""));

		Set<Integer> worlds = Set.of(220050000, 220050001);
		assertEquals(Set.of(
			new StaticSpawnReference(220050000, 700759),
			new StaticSpawnReference(220050001, 700759),
			new StaticSpawnReference(220050001, 700760)),
			QuestGraphReferenceCatalog.collectStaticSpawnReferences(spawns, worlds));
		assertEquals(Set.of(700759, 700760), QuestGraphReferenceCatalog.collectStaticSpawnNpcIds(spawns, worlds));
	}
}
