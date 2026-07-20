package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ShugoImperialTombMigrationTest {
	@Test
	void conditionClockRestoresFromAbsoluteStartTime() {
		assertEquals(1, ShugoImperialTombInstance.conditionValue(1_000, 1_000, 130));
		assertEquals(25, ShugoImperialTombInstance.conditionValue(1_000, 25_000, 130));
		assertEquals(130, ShugoImperialTombInstance.conditionValue(1_000, 500_000, 130));
	}

	@Test
	void retailConditionsOwnWavesAndBosses() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ShugoImperialTombInstance.java"));
		assertTrue(handler.contains("RetailConditionSpawnEngine.setVariable"));
		assertTrue(handler.contains(".started_at"));
		assertTrue(handler.contains("scheduleDeadline(stageKey(variable), nextDeadline"));
		for (String removed : new String[] {
			"scheduleRaid", "spawnRaidNpc", "RAID_WAVE_OFFSETS", "tomb.kills.", "spawn(219", "Future<?>",
			"GameThreadPoolServices", "onDropRegistered", "regDropItem"
		}) {
			assertFalse(handler.contains(removed), removed);
		}

		String ai = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/shugoImperialTomb/Crown_Prince_AdmirerAI2.java"))
			+ Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/shugoImperialTomb/Empress_AdmirerAI2.java"))
			+ Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/shugoImperialTomb/Emperor_AdmirerAI2.java"));
		assertTrue(ai.contains("startConditionStage(\"Condition_S2\", 130)"));
		assertTrue(ai.contains("startConditionStage(\"Condition_S3\", 173)"));
		assertTrue(ai.contains("startConditionStage(\"Condition_S4\", 141)"));
		assertFalse(ai.contains("GameThreadPoolServices"));
		assertFalse(ai.contains("spawn(219"));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		Matcher world = Pattern.compile("<world id=\"300560000\".*?</world>", Pattern.DOTALL).matcher(conditions);
		assertTrue(world.find());
		String block = world.group();
		assertTrue(block.contains("<variable name=\"condition_s2\"/>"));
		assertTrue(block.contains("<variable name=\"condition_s3\"/>"));
		assertTrue(block.contains("<variable name=\"condition_s4\"/>"));
		assertEquals(453, Pattern.compile("<condition id=").matcher(block).results().count());

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300560000_Shugo_Imperial_Tomb.xml"));
		for (int npcId : new int[] { 831110, 831111, 831112 }) {
			assertTrue(staticSpawns.contains("npc_id=\"" + npcId + "\""));
		}
	}
}
