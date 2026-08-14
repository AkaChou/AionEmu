package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.SpawnsData2;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;

class SpawnEngineInstanceMetadataTest {

	@Test
	void filtersDifficultyAndSpawnPageRangeIndependently() throws Exception {
		SpawnsData2 data = (SpawnsData2) JAXBContext.newInstance(SpawnsData2.class).createUnmarshaller().unmarshal(
				new StringReader("""
					<spawns><spawn_map map_id="100">
						<spawn npc_id="1"/>
						<spawn npc_id="2" difficult_id="2"/>
						<spawn npc_id="3" spawn_page="1" spawn_page_end="41"/>
						<spawn npc_id="4" spawn_page="11"/>
						<spawn npc_id="5" difficult_id="2" spawn_page="11" initial_delay="125"/>
						<spawn npc_id="6" spawn_page="0"/>
					</spawn_map></spawns>
					"""));
		List<SpawnGroup2> spawns = data.getSpawnsByWorldId(100);
		SpawnGroup2 common = spawn(spawns, 1);
		SpawnGroup2 difficulty = spawn(spawns, 2);
		SpawnGroup2 range = spawn(spawns, 3);
		SpawnGroup2 exactPage = spawn(spawns, 4);
		SpawnGroup2 both = spawn(spawns, 5);
		SpawnGroup2 explicitZero = spawn(spawns, 6);

		assertTrue(SpawnEngine.matchesInstance(common, 0, 0));
		assertTrue(SpawnEngine.matchesInstance(difficulty, 2, 0));
		assertFalse(SpawnEngine.matchesInstance(difficulty, 1, 0));
		assertTrue(SpawnEngine.matchesInstance(range, 0, 1));
		assertTrue(SpawnEngine.matchesInstance(range, 0, 41));
		assertFalse(SpawnEngine.matchesInstance(range, 0, 0));
		assertFalse(SpawnEngine.matchesInstance(range, 0, 42));
		assertTrue(SpawnEngine.matchesInstance(exactPage, 0, 11));
		assertFalse(SpawnEngine.matchesInstance(exactPage, 0, 1));
		assertTrue(SpawnEngine.matchesInstance(both, 2, 11));
		assertFalse(SpawnEngine.matchesInstance(both, 1, 11));
		assertTrue(SpawnEngine.matchesInstance(explicitZero, 0, 0));
		assertFalse(SpawnEngine.matchesInstance(explicitZero, 0, 1));
		assertEquals(126_000, SpawnEngine.initialSpawnDeadline(both, 1_000));
	}

	private static SpawnGroup2 spawn(List<SpawnGroup2> spawns, int npcId) {
		return spawns.stream().filter(spawn -> spawn.getNpcId() == npcId).findFirst().orElseThrow();
	}
}
