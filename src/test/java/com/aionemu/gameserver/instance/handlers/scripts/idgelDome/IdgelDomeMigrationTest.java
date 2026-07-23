package com.aionemu.gameserver.instance.handlers.scripts.idgelDome;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void landmarkUsesUniquePersistentRetailTerminalScores() throws Exception {
		String landmark = source("IdgelDomeLandmarkInstance.java");
		assertTrue(landmark.contains("npcId == 833914 && scoreApplyType == 1"));
		assertTrue(landmark.contains("npcId == 833922 && scoreApplyType == 2"));
		assertTrue(landmark.contains("npc.getSpawn().getStableKey()"));
		assertTrue(landmark.contains("reward.addPointsByRace(scoreApplyType == 1 ? Race.ELYOS : Race.ASMODIANS, points)"));
		assertTrue(landmark.contains("npc.getNpcId() != 243965 && npc.getNpcId() != 243966"));

		String first = IdgelDomeLandmarkInstance.scoreEventKey("condition:1031:generation.1", 100);
		assertEquals(first, IdgelDomeLandmarkInstance.scoreEventKey("condition:1031:generation.1", 101));
		assertFalse(first.equals(IdgelDomeLandmarkInstance.scoreEventKey("condition:1034:generation.1", 100)));
		assertEquals("landmark.score.event.object.100", IdgelDomeLandmarkInstance.scoreEventKey(null, 100));
	}

	@Test
	void restoresPersistedParticipationStart() throws Exception {
		assertNotNull(IdgelDomePlayerReward.class.getConstructor(int.class, byte.class, Race.class, long.class));
		assertNotNull(LandMarkPlayerReward.class.getConstructor(int.class, byte.class, Race.class, long.class));
	}

	@Test
	void removesOnlyObjectsAlreadyOwnedByRetailData() throws Exception {
		String dome = source("IdgelDomeInstance.java");
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

		for (String handler : new String[] { dome, landmark }) {
			assertFalse(method(handler, "onLeaveInstance").contains("removeItems(player)"));
			assertFalse(method(handler, "onPlayerLogOut").contains("removeItems(player)"));
			assertTrue(method(handler, "onExitInstance").contains("removeItems(player)"));
		}
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId = 164000314; itemId <= 164000316; itemId++) {
			assertTrue(item(items, itemId).contains("ownership_world=\"301310000\""));
		}
		for (int itemId = 164000413; itemId <= 164000414; itemId++) {
			assertTrue(item(items, itemId).contains("ownership_world=\"301680000\""));
		}
	}

	@Test
	void keepsIdgelDomeHandlerSpawnsWithoutConditionProducer() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		assertFalse(conditions.contains("<world id=\"301310000\""));

		String dome = source("IdgelDomeInstance.java");
		for (String required : new String[] {
			"scheduleLegacyEvents", "spawnSupplies", "spawnBoss", "spawnTrap",
			"spawn(234190", "spawn(234751", "spawn(702581", "spawn(702404",
			"case 802192", "case 802193", "idgel.supply.deadline", "idgel.boss.deadline"
		}) {
			assertTrue(dome.contains(required), required);
		}

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301310000_Idgel_Dome.xml"));
		assertTrue(spawns.contains("npc_id=\"802192\""));
		assertTrue(spawns.contains("npc_id=\"802193\""));
	}

	private String source(String file) throws Exception {
		return Files.readString(HANDLERS.resolve(file));
	}

	private static String method(String source, String name) {
		int start = source.indexOf(name + "(");
		return source.substring(start, source.indexOf("\n\t}", start));
	}

	private static String item(String source, int itemId) {
		int start = source.indexOf("<item_template id=\"" + itemId + "\"");
		return source.substring(start, source.indexOf("</item_template>", start));
	}
}
