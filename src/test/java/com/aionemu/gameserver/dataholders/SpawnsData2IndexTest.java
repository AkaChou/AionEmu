package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
	void keepsSpawnPageAndInitialDelayVariants() throws Exception {
		SpawnsData2 data = unmarshal("""
			<spawns>
				<spawn_map map_id="100">
					<spawn npc_id="101" spawn_page="1" spawn_page_end="41" initial_delay="125"/>
					<spawn npc_id="101" spawn_page="11" initial_delay="130"/>
					<spawn npc_id="102" initial_delay="420"/>
					<spawn npc_id="102" initial_delay="720"/>
				</spawn_map>
			</spawns>
			""");

		List<SpawnGroup2> spawns = data.getSpawnsByWorldId(100);
		assertEquals(4, spawns.size());
		assertEquals(List.of(1, 11), spawns.stream().filter(spawn -> spawn.getNpcId() == 101)
				.map(SpawnGroup2::getSpawnPage).toList());
		assertEquals(41, spawns.stream().filter(spawn -> spawn.getNpcId() == 101).findFirst().orElseThrow()
				.getSpawnPageEnd());
		assertEquals(List.of(420, 720), spawns.stream().filter(spawn -> spawn.getNpcId() == 102)
				.map(SpawnGroup2::getInitialDelay).toList());
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

	@Test
	void loadsGeneratedSpawnMetadataThroughSchema() throws Exception {
		Path spawnDirectory = Path.of("src/main/resources/aion/data/static_data/spawns");
		var schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(spawnDirectory.resolve("spawns.xsd").toFile());

		SpawnsData2 data = SpawnsData2.load(spawnDirectory.toFile(), schema);

		SpawnGroup2 sharedArenaSpawn = data.getSpawnsByWorldId(300350000).stream()
				.filter(spawn -> spawn.getNpcId() == 207037 && spawn.getSpawnPage() != 0).findFirst().orElseThrow();
		assertEquals(1, sharedArenaSpawn.getSpawnPage());
		assertEquals(41, sharedArenaSpawn.getSpawnPageEnd());
		assertEquals(List.of(420, 720), data.getSpawnsByWorldId(301310000).stream()
				.filter(spawn -> spawn.getNpcId() == 234751).map(SpawnGroup2::getInitialDelay).toList());
	}

	private static SpawnsData2 unmarshal(String xml) throws Exception {
		return (SpawnsData2) JAXBContext.newInstance(SpawnsData2.class).createUnmarshaller()
				.unmarshal(new StringReader(xml));
	}
}
