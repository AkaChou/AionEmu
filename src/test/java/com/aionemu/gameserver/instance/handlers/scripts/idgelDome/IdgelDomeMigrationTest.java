package com.aionemu.gameserver.instance.handlers.scripts.idgelDome;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.playerreward.IdgelDomePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.LandMarkPlayerReward;

class IdgelDomeMigrationTest {
	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/idgelDome");

	@Test
	void usesRetailDeadlinesScoresAndLedgerSettlement() throws Exception {
		String dome = source("IdgelDomeInstance.java");
		String landmark = source("IdgelDomeLandmarkInstance.java");
		for (String handler : new String[] { dome, landmark }) {
			assertTrue(handler.contains("PREPARATION_MILLIS = 120_000"));
			assertTrue(handler.contains("BATTLE_MILLIS = 1_200_000"));
			assertTrue(handler.contains("scheduleDeadline(\"preparation\""));
			assertTrue(handler.contains("scheduleDeadline(\"battle\""));
			assertTrue(handler.contains("scheduleDeadline(\"exit\""));
			assertTrue(handler.contains("InstanceSettlementService.queueBattleground("));
			assertFalse(handler.contains("Future<?>"));
			assertFalse(handler.contains("AtomicBoolean"));
			assertFalse(handler.contains("GameThreadPoolServices"));
			assertFalse(handler.contains("onDropRegistered"));
			assertFalse(handler.contains("updateScore(player, player, -"));
		}
		assertTrue(dome.contains("updateScore(attacker, player, 200, true)"));
		assertTrue(landmark.contains("updateScore(attacker, player, 50, true)"));
		assertTrue(dome.indexOf("queueBattleground(") < dome.lastIndexOf("idgel.settled"));
		assertTrue(landmark.indexOf("queueBattleground(") < landmark.lastIndexOf("landmark.settled"));
	}

	@Test
	void restoresPersistedParticipationStart() throws Exception {
		assertNotNull(IdgelDomePlayerReward.class.getConstructor(int.class, byte.class, Race.class, long.class));
		assertNotNull(LandMarkPlayerReward.class.getConstructor(int.class, byte.class, Race.class, long.class));
	}

	@Test
	void removesOnlyObjectsAlreadyOwnedByRetailData() throws Exception {
		String landmark = source("IdgelDomeLandmarkInstance.java");
		assertFalse(landmark.contains("spawn(806303"));
		assertFalse(landmark.contains("spawn(806304"));

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.matches("(?s).*pc_kill_score=\"200\"[^>]*wait_time=\"120\"[^>]*world_id=\"301310000\".*"));
		assertTrue(rewards.matches("(?s).*pc_kill_score=\"50\"[^>]*wait_time=\"120\"[^>]*world_id=\"301680000\".*"));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		for (String mapping : new String[] { "702581:164000314", "702582:164000315", "702583:164000316",
			"834168:164000413", "834169:164000414" }) {
			String[] ids = mapping.split(":");
			assertTrue(drops.matches("(?s).*<npc_drop npc_id=\"" + ids[0]
				+ "\">.*?<drop item_id=\"" + ids[1] + "\".*"));
		}
	}

	private String source(String file) throws Exception {
		return Files.readString(HANDLERS.resolve(file));
	}
}
