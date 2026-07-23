package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarkPoetaRetailMigrationTest {

	@Test
	void retailDataOwnsDarkPoetaSpawnsPatternsScoresAndDrops() throws Exception {
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/darkPoeta")));
		String npcTemplates = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (String legacyAi : new String[] { "marabata_of_strength", "marabata_of_aether",
				"marabata_of_poisoning", "telepathycontroller", "tahabatapyrelord", "calindiflamelord",
				"enraged_inferno_demon", "inferno_demon", "marabatacontroller", "crazy_scar", "drana_lump",
				"faithfulsubordinate" }) {
			assertFalse(npcTemplates.contains("ai=\"" + legacyAi + "\""), legacyAi);
		}

		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300040000");
		assertEquals(73, count(world, "<condition "));
		assertEquals(20, count(world, "<variable "));
		for (String variable : new String[] { "avanq_die", "svanq_die", "boss_kill", "grade", "light", "dark",
				"master_boss", "middleboss_a_kill", "middleboss_b_kill", "middleboss_c_kill", "nagaboss_kill",
				"vanq", "aboss_die", "sboss_die", "specialserver_cond" }) {
			assertTrue(world.contains(variable), variable);
		}
		for (String source : new String[] { "#3", "#4", "#5", "#18", "#19", "#28" }) {
			assertTrue(world.contains("source=\"idlf1/world_N.xml" + source + "\""), source);
		}
		assertTrue(world.contains("npc id=\"206478\""));
		assertEquals(3, count(world, "npc id=\"206478\""));
		assertEquals(2, count(world, "npc id=\"856603\""));
		assertEquals(1, count(world, "npc id=\"237373\""));
		assertEquals(3, count(world, "<sensory_area bottom=\"133.198227\" top=\"433.198242\">"));
		assertTrue(world.contains("(vanq == 4) &amp;&amp; (sboss_die == 1)"));
		assertFalse(world.contains("(vanq == 4) &amp;&amp; (sboss_die == 1))"));
		assertTrue(world.contains("(vanq == 4) &amp;&amp; (aboss_die == 1)"));
		assertFalse(world.contains("(vanq == 4) &amp;&amp; (aboss_die == 1))"));

		String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml"));
		for (String pattern : new String[] { "IDLF1_Generator", "IDLF1_Gener_02", "IDLF1_Gener_03",
				"XDrakan_LastBoss", "Dragon_G1", "Dragon_G2", "Dragon_G3", "Dragon_G4", "Dragon_G5" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}

		String scores = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		for (String[] score : new String[][] { { "214864", "789" }, { "214894", "789" },
				{ "214895", "377" }, { "214904", "954" } }) {
			String entry = scoreEntry(scores, score[0]);
			assertTrue(entry.contains("value=\"" + score[1] + "\""), score[0]);
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_026.xml"));
		for (String npcId : new String[] { "214864", "214849", "214850", "214851", "214904", "215280",
				"215281", "215282", "215283", "215284", "215389", "702658", "702659", "856605", "856606" }) {
			assertTrue(npcDropBlock(drops, npcId).contains("<drop"), npcId);
		}

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DarkPoetaInstance.java"));
		for (String required : new String[] { "runtimeState()", "scheduleDeadline(\"prepare\"",
				"scheduleDeadline(\"expire\"", "scheduleDeadline(\"settle\"", "scheduleDeadline(\"leave\"",
				"InstanceSettlementService.darkPoetaRank", "InstanceSettlementService.darkPoetaBossGrade",
				"InstanceSettlementService.darkPoetaGatherScore", "DataManager.RETAIL_AI_DATA.getNpcScore",
				"setCondition(\"grade\"", "setCondition(\"boss_kill\"",
				"setCondition(\"nagaboss_kill\"", "setCondition(\"middleboss_a_kill\"",
				"setCondition(\"middleboss_b_kill\"", "setCondition(\"middleboss_c_kill\"",
				"setCondition(\"specialserver_cond\"", "spawnPage == 2 ? 1 : 0" }) {
			assertTrue(handler.contains(required), required);
		}
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("GameWorldServices"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("Future"));
		assertFalse(handler.contains("settleTimeAttack"));
		for (String oldThreshold : new String[] { "19643", "17046", "13055", "9334", "6556", "1254" }) {
			assertFalse(handler.contains(oldThreshold), oldThreshold);
		}
		assertTrue(handler.contains("setCondition(\"grade\", grade);\n\t\tsetCondition(\"boss_kill\", 1);"));
		for (String generatedSpawn : new String[] { "spawn(215280", "spawn(215281", "spawn(215282",
				"spawn(215283", "spawn(215284", "spawn(217166", "spawn(214904", "spawn(700478",
				"spawn(731666", "spawn(856605", "spawn(856606", "spawn(215429", "spawn(215430" }) {
			assertFalse(handler.contains(generatedSpawn), generatedSpawn);
		}
		assertTrue(handler.contains("sendMovieOnce(killer, 426)"));
		assertTrue(handler.contains("sendMovieOnce(instance.getPlayersInside().stream().findFirst().orElse(null), 427)"));
		assertTrue(handler.contains("doorId != 33"));
		assertTrue(handler.contains("setDoorState(doorId, true)"));
		for (String itemId : new String[] { "122001039", "123000929", "123000930", "170490001", "188053083",
				"188053276", "188053277", "188053278", "188053279", "188053280", "188053286", "188053287",
				"188053290", "188053291", "188053292", "188053579", "188053580", "188053788", "188054178",
				"188054179", "188054183", "190020175" }) {
			assertFalse(handler.contains(itemId), itemId);
		}

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300040000_Dark_Poeta.xml"));
		String fighter = spawnBlock(staticSpawns, "215429");
		String assassin = spawnBlock(staticSpawns, "215430");
		assertTrue(fighter.contains("difficult_id=\"1\""));
		assertTrue(fighter.contains("x=\"660.261353\" y=\"224.123947\""));
		assertTrue(fighter.contains("x=\"565.488342\" y=\"256.223938\""));
		assertEquals(2, count(fighter, "<spot "));
		assertTrue(assassin.contains("difficult_id=\"1\""));
		assertTrue(assassin.contains("x=\"610.017883\" y=\"213.537796\""));
		assertTrue(assassin.contains("x=\"470.791779\" y=\"378.285004\""));
		assertEquals(2, count(assassin, "<spot "));
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static String scoreEntry(String xml, String npcId) {
		int start = xml.lastIndexOf("<npc_score ", xml.indexOf("npc_id=\"" + npcId + "\""));
		int end = xml.indexOf("/>", start);
		return xml.substring(start, end);
	}

	private static String npcDropBlock(String xml, String npcId) {
		int start = xml.indexOf("<npc_drop npc_id=\"" + npcId + "\">");
		int end = xml.indexOf("</npc_drop>", start);
		return xml.substring(start, end);
	}

	private static String spawnBlock(String xml, String npcId) {
		int start = xml.indexOf("<spawn npc_id=\"" + npcId + "\"");
		int end = xml.indexOf("</spawn>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
