package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.instance.instancereward.DarkPoetaReward;

class DarkPoetaInstanceTest {
	private static final Path SCRIPTS = Path.of("src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void restoresPersistedScoreboard() {
		DarkPoetaReward reward = new DarkPoetaReward(300040000, 1);

		reward.restore(17_817, 42, 3, 1);

		assertEquals(17_817, reward.getPoints());
		assertEquals(42, reward.getNpcKills());
		assertEquals(3, reward.getGatherCollections());
		assertEquals(1, reward.getRank());
	}

	@Test
	void mapsMarabataControllersToTheirNearbyBoss() {
		for (int npcId = 700439; npcId <= 700447; npcId++) {
			assertEquals(214849 + (npcId - 700439) / 3, DarkPoetaInstance.marabataBossId(npcId));
		}
	}

	@Test
	void globalInstanceSpawnsAreNotRepeatedForEveryPlayer() throws Exception {
		assertFalse(source("HamateIsleStoreroomInstance").contains("public void onEnterInstance"));
		assertFalse(source("CarpusIsleStoreroomInstance").contains("public void onEnterInstance"));
		assertSpawnGuarded("DraupnirCaveInstance", "spawn(237276");
		String eternal = source("TheEternalBastionInstance");
		assertTrue(eternal.contains("runtimeState().getBoolean(STATE_PREFIX + \"completed\""));
		assertFalse(eternal.contains("instanceReward.addPoints(20000)"));
	}

	private static void assertSpawnGuarded(String className, String spawnMarker) throws Exception {
		String source = source(className);
		int enter = source.indexOf("public void onEnterInstance");
		int guard = source.indexOf("if (spawnRace != null)", enter);
		int assignment = source.indexOf("spawnRace = player.getRace();", guard);
		int spawn = source.indexOf(spawnMarker, assignment);

		assertTrue(enter >= 0 && guard > enter && assignment > guard && spawn > assignment);
	}

	private static String source(String className) throws Exception {
		return Files.readString(SCRIPTS.resolve(className + ".java"));
	}
}
