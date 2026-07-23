package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StonespearReachRetailMigrationTest {

	@Test
	void retailDataOwnsCombatWhileHandlerOwnsPersistentLifecycle() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"301500000\"", "</world>");
		for (String variable : new String[] { "boss_on", "f1_t1_obj_die", "hidden_on", "legion_on", "race_dark",
				"race_light", "t2_fobj_on" }) {
			assertTrue(world.contains("name=\"" + variable + "\""), variable);
		}
		assertTrue(world.contains("boss_on == 10"));
		assertTrue(world.contains("npc id=\"855843\""));

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_f5_legion_jsm.xml"));
		assertTrue(patterns.contains("<name>Legion_04_Boss_01</name>"));
		assertTrue(patterns.contains("<string>boss_on</string>"));

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/StonespearReachInstance.java"));
		assertTrue(handler.contains("RetailConditionSpawnEngine.consumeConditionSpawnDeath"));
		assertTrue(handler.contains("scoreEventKey(stableKey, npc.getObjectId())"));
		assertTrue(handler.contains("scheduleDeadline(\"prepare\""));
		assertTrue(handler.contains("scheduleDeadline(\"expire\""));
		assertFalse(handler.contains("spawn("));

		String coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301500000\"")).findFirst().orElseThrow();
		assertTrue(coverage.contains("retail static spawns, seven condition variables, Legion Pattern and npc-scores own actors"));
	}

	private static String block(String value, String startMarker, String endMarker) {
		int start = value.indexOf(startMarker);
		int end = value.indexOf(endMarker, start);
		return value.substring(start, end + endMarker.length());
	}
}
