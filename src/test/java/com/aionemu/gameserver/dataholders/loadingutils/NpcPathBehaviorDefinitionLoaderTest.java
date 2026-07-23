package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.dataholders.NpcPathBehaviorData.PathfindFailReaction;

class NpcPathBehaviorDefinitionLoaderTest {

	@TempDir
	Path directory;

	@Test
	void loadsRetailPathBehaviorAndDefaults() throws Exception {
		Path file = directory.resolve("npc-ai.xml");
		Files.writeString(file, """
				<npc_ai_mappings>
				<npc id="1" generate_pathfind="false" max_chase_time="SP" react_to_pathfind_fail="pull_target"
					move_type_return="run" move_speed_return="200" decrease_sensory_range_return="40"/>
				<npc id="2"/>
				</npc_ai_mappings>
				""");

		var data = NpcPathBehaviorDefinitionLoader.load(file.toFile());

		assertEquals(2, data.size());
		assertEquals("SP", data.get(1).maxChaseTime());
		assertEquals(PathfindFailReaction.PULL_TARGET, data.get(1).pathfindFailReaction());
		assertEquals("run", data.get(1).returnMoveType());
		assertEquals(200, data.get(1).returnSpeedPercent());
		assertEquals(40, data.get(1).returnSensoryPercent());
		assertFalse(data.allowsPathfinding(1));
		assertNull(data.get(2).maxChaseTime());
		assertEquals(PathfindFailReaction.RETURN_TO_SP, data.get(2).pathfindFailReaction());
		assertEquals("walk", data.get(2).returnMoveType());
		assertEquals(150, data.get(2).returnSpeedPercent());
		assertEquals(50, data.get(2).returnSensoryPercent());
		assertTrue(data.allowsPathfinding(2));
		assertTrue(data.allowsPathfinding(3));
	}

	@Test
	void loadsDarkPoetaPathfindingFlags() {
		var data = NpcPathBehaviorDefinitionLoader.load(
			Path.of("src/main/resources/aion/definitions/compact/ai/npc-ai.xml").toFile());

		assertFalse(data.allowsPathfinding(214904));
		assertFalse(data.allowsPathfinding(281246));
		assertFalse(data.allowsPathfinding(281249));
		assertTrue(data.allowsPathfinding(281248));
	}
}
