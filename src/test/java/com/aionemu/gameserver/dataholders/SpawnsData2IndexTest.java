package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;

class SpawnsData2IndexTest {

	@Test
	void indexesWorldSpawnsAndKeepsCustomOverride() throws Exception {
		SpawnsData2 data = unmarshal("""
			<spawns>
				<spawn_map map_id="100">
					<spawn npc_id="101"/>
					<spawn npc_id="102"/>
					<spawn npc_id="101" custom="true"/>
				</spawn_map>
			</spawns>
			""");

		List<SpawnGroup2> spawns = data.getSpawnsByWorldId(100);
		assertEquals(2, spawns.size());
		assertEquals(102, spawns.get(0).getNpcId());
		assertEquals(101, spawns.get(1).getNpcId());
		assertTrue(data.getSpawnsForNpc(100, 101).isCustom());
	}

	@Test
	void buildsStartupIndexesThroughReusableLookupHelpers() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/dataholders/SpawnsData2.java"));
		String afterUnmarshal = source.substring(source.indexOf("public void afterUnmarshal"),
				source.indexOf("\n\tpublic void clearTemplates()"));

		assertTrue(source.contains("private Map<Integer, SimpleEntry<SpawnGroup2, Spawn>> spawnIndexForWorld(int mapId)"));
		assertTrue(source.contains(
				"private List<SpawnGroup2> spawnGroupsFor(IntObjectHashMap<List<SpawnGroup2>> spawnMaps, int id)"));
		assertFalse(afterUnmarshal.contains("allSpawnMaps.get(mapId)"));
		assertFalse(afterUnmarshal.contains(".containsKey(id)"));
		assertFalse(afterUnmarshal.contains(".get(id).add(spawnGroup)"));
	}

	private static SpawnsData2 unmarshal(String xml) throws Exception {
		return (SpawnsData2) JAXBContext.newInstance(SpawnsData2.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}
}
