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
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;

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

	@Test
	void skipsEmptySpawnWhenSearchingForNpc() throws Exception {
		SpawnsData2 oldSpawns = DataManager.SPAWNS_DATA2;
		WorldMapsData oldWorldMaps = DataManager.WORLD_MAPS_DATA;
		try {
			SpawnsData2 data = unmarshal("""
				<spawns>
					<spawn_map map_id="100"><spawn npc_id="101"/></spawn_map>
					<spawn_map map_id="200"><spawn npc_id="101"><spot x="1" y="2" z="3" h="0"/></spawn></spawn_map>
				</spawns>
				""");
			DataManager.SPAWNS_DATA2 = data;
			DataManager.WORLD_MAPS_DATA = unmarshalWorldMaps(
				"<world_maps><map id=\"100\" flags=\"RECALL\"/><map id=\"200\" flags=\"RECALL\"/></world_maps>");

			SpawnSearchResult result = data.getFirstSpawnByNpcId(100, 101);

			assertEquals(200, result.getWorldId());
			assertEquals(1, result.getSpot().getX());
		} finally {
			DataManager.SPAWNS_DATA2 = oldSpawns;
			DataManager.WORLD_MAPS_DATA = oldWorldMaps;
		}
	}

	@Test
	void reloadsSpawnFilesFromNestedDirectories(@TempDir Path directory) throws Exception {
		Files.writeString(directory.resolve("first.xml"), """
			<spawns><spawn_map map_id="100"><spawn npc_id="101"/></spawn_map></spawns>
			""");
		Path nested = Files.createDirectory(directory.resolve("nested"));
		Files.writeString(nested.resolve("second.xml"), """
			<spawns><spawn_map map_id="200"><spawn npc_id="202"/></spawn_map></spawns>
			""");
		Files.writeString(nested.resolve("new_ignored.xml"), "<invalid/>");

		SpawnsData2 data = SpawnsData2.load(directory.toFile(), null);

		assertEquals(101, data.getSpawnsByWorldId(100).getFirst().getNpcId());
		assertEquals(202, data.getSpawnsByWorldId(200).getFirst().getNpcId());
	}

	private static SpawnsData2 unmarshal(String xml) throws Exception {
		return (SpawnsData2) JAXBContext.newInstance(SpawnsData2.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}

	private static WorldMapsData unmarshalWorldMaps(String xml) throws Exception {
		return (WorldMapsData) JAXBContext.newInstance(WorldMapsData.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}
}
