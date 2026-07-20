package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ShugoImperialTombMigrationTest {
	@Test
	void raidWavesUsePersistentDeadlinesAndCounters() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ShugoImperialTombInstance.java"));
		assertTrue(source.contains("scheduleDeadline(spawnKey, spawnAt"));
		assertTrue(source.contains("if (spawnAt <= now)"));
		assertTrue(source.contains("runtimeState().put(spawnKey + \".dead\", true)"));
		assertTrue(source.contains("runtimeState().getBoolean(spawnKey + \".dead\", false)"));
		assertTrue(source.contains("tomb.kills.diligent"));
		assertTrue(source.contains("restoreRaid"));
		assertTrue(source.contains("restoreFinishedSpawns"));
		assertTrue(source.contains("tomb.spawn.\" + key + \".spawned"));
		assertTrue(source.contains("tomb.boss.letu.killed"));
		assertTrue(source.contains("tomb.boss.captain.killed"));
		String create = source.substring(source.indexOf("public void onInstanceCreate"), source.indexOf("public void onDie"));
		assertFalse(create.contains("spawn(831095"));
		assertFalse(create.contains("spawn(831110"));
		assertFalse(source.contains("tomb.spawn.\" + key + \".done"));
		assertFalse(source.contains("Future<?>"));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("imperialTombTask"));
		assertFalse(source.contains("onDropRegistered"));
		assertFalse(source.contains("regDropItem"));
	}

	@Test
	void mapHasNoConditionSpawnOwnershipForWaveReplacement() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		assertFalse(Pattern.compile("<world id=\"300560000\"", Pattern.DOTALL).matcher(conditions).find());

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300560000_Shugo_Imperial_Tomb.xml"));
		assertTrue(staticSpawns.contains("npc_id=\"831095\""));
		assertTrue(staticSpawns.contains("npc_id=\"831110\""));
	}
}
