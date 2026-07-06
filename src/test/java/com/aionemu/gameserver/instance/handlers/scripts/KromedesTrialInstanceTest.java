package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class KromedesTrialInstanceTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KromedesTrialInstance.java");

	@Test
	void onDieDoesNotDereferenceMissingDamageOwnerForClassTreasure() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		assertTrue(onDie.contains("Player player = getDeathRewardPlayer(npc);"));
		assertFalse(onDie.contains("player.getPlayerClass()"));
		assertTrue(onDie.contains("spawnClassTreasure(player, 740.83966f, 535.38837f, 199.12067f, (byte) 89);"));
		assertTrue(onDie.contains("spawnClassTreasure(player, 512.89886f, 570.039f, 216.89487f, (byte) 31);"));
	}

	@Test
	void ladyAngerrDeathKeepsStorySpawnBeforeNullableClassTreasure() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		int storySpawn = onDie.indexOf("spawn(217001, 650.679f, 774.197f, 215.584f, (byte) 60);");
		int classTreasure = onDie.indexOf("spawnClassTreasure(player, 512.89886f, 570.039f, 216.89487f, (byte) 31);");

		assertTrue(storySpawn >= 0, "Lady Angerr death should still spawn Distraught Lady Angerr");
		assertTrue(classTreasure > storySpawn, "class-specific treasure should be skipped independently of the story spawn");
	}

	@Test
	void finalBossRewardSkipsPlayerActionsWhenDamageOwnerIsMissing() throws IOException {
		String onDie = methodBody(Files.readString(SOURCE), "public void onDie(Npc npc)");

		int finalBossCase = onDie.indexOf("case 217005:");
		int playerGuard = onDie.indexOf("if (player != null)", finalBossCase);
		int sendMovie = onDie.indexOf("sendMovie(player, 455);", finalBossCase);

		assertTrue(finalBossCase >= 0);
		assertTrue(playerGuard > finalBossCase);
		assertTrue(sendMovie > playerGuard);
	}

	@Test
	void classTreasureSpawnIgnoresMissingDamageOwner() throws IOException {
		String spawnClassTreasure = methodBody(Files.readString(SOURCE),
				"private void spawnClassTreasure(Player player, float x, float y, float z, byte heading)");

		int nullGuard = spawnClassTreasure.indexOf("if (player == null)");
		int classSwitch = spawnClassTreasure.indexOf("switch (player.getPlayerClass())");

		assertTrue(nullGuard >= 0);
		assertTrue(classSwitch > nullGuard);
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}
}
