package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteelRakeRetailMigrationTest {

	@Test
	void retailDataOwnsSteelRakeConditionsRandomSpawnsAndDrops() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300100000");
		assertEquals(15, count(world, "<condition "));
		assertEquals(2, count(world, "<variable "));
		assertTrue(world.contains("<variable name=\"idshulackship_ph_kill\"/>"));
		assertTrue(world.contains("<variable name=\"lever_ver30\"/>"));

		String mate = conditionBlock(world, "idshulackship/world_N.xml#1");
		assertTrue(mate.contains("expression=\"IDSHULACKSHIP_PH_KILL == 1\""));
		assertTrue(mate.contains("id=\"215069\" probability=\"10000\" x=\"473.177826\" y=\"508.965637\" z=\"1034.71997\""));
		assertTrue(mate.contains("walker=\"retail:300100000:idship_mobpath_shulackasfirstmateknmd_45_ae\""));
		assertTrue(mate.contains("despawn_at_attack_state=\"true\""));

		String delivery = conditionBlock(world, "idshulackship/world_N.xml#unconditional-random-8");
		for (String entry : new String[] { "215054:1500", "215055:2000", "215076:1500",
				"215077:1000", "215074:2000", "215075:2000" }) {
			String[] values = entry.split(":");
			assertTrue(delivery.contains("id=\"" + values[0] + "\" probability=\"" + values[1] + "\""), entry);
		}
		assertTrue(delivery.contains("x=\"461.93335\" y=\"510.545654\" z=\"879.832275\""));

		String thirdFloor = conditionBlock(world, "idshulackship/world_N.xml#unconditional-random-9");
		assertTrue(thirdFloor.contains("group_mode=\"one\""));
		assertEquals(1, count(thirdFloor, "<group probability=\"334\">"));
		assertEquals(2, count(thirdFloor, "<group probability=\"333\">"));
		for (String probability : new String[] { "215425:8000", "215426:1900", "215422:100" }) {
			String[] values = probability.split(":");
			assertEquals(3, count(thirdFloor, "id=\"" + values[0] + "\" probability=\"" + values[1] + "\""));
		}

		String fixedNamed = conditionBlock(world, "idshulackship/world_N.xml#fixed-retail-named");
		assertTrue(fixedNamed.contains("id=\"219028\" probability=\"10000\" x=\"727.222107\" y=\"541.030029\" z=\"945\""));
		assertTrue(fixedNamed.contains("id=\"219029\" probability=\"10000\" x=\"740.972595\" y=\"458.115265\" z=\"945\""));
		assertEquals(2, count(fixedNamed, "respawn_time=\"60\" respawn_time_extra=\"15\""));

		String waypoints = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml"));
		String patrol = walkerBlock(waypoints, "retail:300100000:npcpathidshulackship_npc76");
		assertEquals(16, count(patrol, "<routestep "));
		assertTrue(patrol.contains("x=\"314.976379\" y=\"572.697937\" z=\"901.801025\" rest_time=\"1000\""));

		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/300100000_Steel Rake.xml"));
		for (String npcId : new String[] { "215069", "215072", "215073", "215421" }) {
			assertFalse(staticSpawns.contains("<spawn npc_id=\"" + npcId + "\""), npcId);
		}
		assertFalse(staticSpawns.contains("x=\"516.198\" y=\"489.708\""));
		assertFalse(staticSpawns.contains("x=\"314.98\" y=\"572.7\""));

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/steelRake/SteelRakeInstance.java"));
		assertTrue(handler.contains("npc.getNpcId() == 214968"));
		assertTrue(handler.contains("RetailConditionSpawnEngine.setVariable(instance, \"IDSHULACKSHIP_PH_KILL\", 1, 0)"));
		for (String legacy : new String[] { "onDropRegistered", "onInstanceCreate", "Rnd", "GameWorldServices",
				"spawn(", "sendMsg", "despawnNpc", "isInstanceDestroyed" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		for (String npcId : new String[] { "215056", "215057", "215058", "215062", "215063", "215064",
				"215065", "215066", "215067", "215069", "215070", "215078", "215080", "215081",
				"215401", "215411", "215412", "215421", "215489", "700554", "700555" }) {
			String block = npcDropBlock(drops, npcId);
			assertTrue(block.contains("<drop ") || block.contains("<common_drop_group"), npcId);
		}
		String groggetDrops = npcDropBlock(drops, "215081");
		assertTrue(groggetDrops.contains("item_id=\"188051416\" chance=\"50.06\""));
		assertFalse(groggetDrops.contains("188053787"));

			String patterns = Files.readString(Path.of(
					"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idshulackship_kjs.xml"));
			assertTrue(patterns.contains("<name>IDSShip_KK</name>"));
			assertTrue(patterns.contains("<set_condition_spawn_variable><string>Lever_ver30</string><set>1</set><modify>0</modify>"));

			String ownership = Files.readAllLines(Path.of(
				"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
				.filter(line -> line.contains("id=\"300100000\"")).findFirst().orElseThrow();
			assertTrue(ownership.contains("retail condition/static spawns, waypoint pools and Pattern own encounter/random spawns"));
			assertTrue(ownership.contains("npc_drops owns loot"));
			assertTrue(ownership.contains(
				"handler only bridges 214968 death to IDSHULACKSHIP_PH_KILL and exits players"));
		}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static String conditionBlock(String world, String source) {
		int sourceIndex = world.indexOf("source=\"" + source + "\"");
		int start = world.lastIndexOf("<condition ", sourceIndex);
		int end = world.indexOf("</condition>", sourceIndex);
		return world.substring(start, end);
	}

	private static String walkerBlock(String xml, String routeId) {
		int start = xml.indexOf("<walker_template route_id=\"" + routeId + "\"");
		int end = xml.indexOf("</walker_template>", start);
		return xml.substring(start, end);
	}

	private static String npcDropBlock(String xml, String npcId) {
		int start = xml.indexOf("<npc_drop npc_id=\"" + npcId + "\">");
		int end = xml.indexOf("</npc_drop>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
