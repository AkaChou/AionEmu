package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OphidanBridgeRetailMigrationTest {

	@Test
	void retailConditionsOwnRunawayBossDoorAndExitFlow() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"300590000\"", "</world>");

		assertEquals(11, count(world, "<variable "));
		assertEquals(56, count(world, "<condition "));
		for (String variable : new String[] { "bridge_on", "mboss_die", "mboss_spawn", "ra_as_run",
				"ra_as_spawn", "ra_pr_run", "ra_pr_spawn", "ra_run_ok", "ra_wi_run", "ra_wi_spawn",
				"under_01_out" }) {
			assertTrue(world.contains("name=\"" + variable + "\""), variable);
		}
		for (int source = 1; source <= 22; source++) {
			assertTrue(world.contains("source=\"idldf5_under_01/world_N.xml#" + source + "\""),
				"source " + source);
		}

		String firstMiddleBoss = source(world, 1);
		for (String npcId : new String[] { "235772", "235773", "235774", "235775" }) {
			assertTrue(firstMiddleBoss.contains("id=\"" + npcId + "\""), npcId);
		}
		String finalBoss = source(world, 11);
		for (String npcId : new String[] { "235769", "235770", "235771" }) {
			assertTrue(finalBoss.contains("id=\"" + npcId + "\""), npcId);
		}
		assertTrue(source(world, 14).contains("id=\"731545\""));
		assertTrue(source(world, 16).contains("id=\"730868\""));
		assertTrue(source(world, 18).contains("id=\"731544\""));
	}

	@Test
	void handlerKeepsOnlyUnimportedDefensePoolsAndOpportunityBundle() throws Exception {
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ophidanBridge/OphidanBridgeInstance.java"));
		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300590000_Ophidan_Bridge.xml"));

		assertEquals(4, count(handler, "switch (Rnd.get"));
		for (String retained : new String[] { "spawn(235721", "spawn(235728", "spawn(235735", "spawn(235742",
				"spawn(802180", "186000051, 30", "186000052, 30", "186000236, 50", "186000237, 50" }) {
			assertTrue(handler.contains(retained), retained);
		}
		for (String removed : new String[] { "spawn(235768", "spawn(235772", "spawn(235776", "spawn(235780",
				"spawn(235781", "spawn(235782", "spawn(730868", "case 235786", "sendMsg(" }) {
			assertFalse(handler.contains(removed), removed);
		}

		assertTrue(spawns.contains("<spawn npc_id=\"856054\""));
		assertTrue(spawns.contains("<spawn npc_id=\"235768\">"));
		assertTrue(spawns.contains("x=\"322.421967\" y=\"491.168427\" z=\"613.944885\""));
		assertFalse(spawns.contains("<spawn npc_id=\"731544\""));
		assertFalse(spawns.contains("<spawn npc_id=\"731545\""));

		String coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300590000\"")).findFirst().orElseThrow();
		assertTrue(coverage.contains("Pattern own runaway route, middle/final boss variants, bridge doors and exit"));
		assertTrue(coverage.contains("four legacy defense random pools"));
		assertTrue(coverage.contains("opportunity bundle/drop bridge"));
	}

	@Test
	void luckyOphidanKeepsHandlerBridgeWithoutRunawayConditionClosure() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"301320000\"", "</world>");
		assertEquals(4, count(world, "<variable "));
		assertEquals(7, count(world, "<condition "));
		for (String missing : new String[] { "ra_as_spawn", "ra_pr_spawn", "ra_wi_spawn", "mboss_spawn" }) {
			assertFalse(world.contains(missing), missing);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ophidanBridge/Lucky_OphidanBridgeInstance.java"));
		int createStart = handler.indexOf("void onInstanceCreate");
		String creation = handler.substring(createStart, handler.indexOf("\n\t@Override", createStart));
		assertEquals(8, count(creation, "switch (Rnd.get"));
		for (String retained : new String[] { "spawn(235768", "spawn(235780", "spawn(235721", "spawn(235728",
				"spawn(235735", "spawn(235742", "spawn(235772", "spawn(702658", "spawn(730868", "spawn(802180" }) {
			assertTrue(handler.contains(retained), retained);
		}

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301320000_Lucky_Ophidan_Bridge.xml"));
		assertFalse(spawns.contains("<spawn npc_id=\"235768\">"));
		String coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301320000\"")).findFirst().orElseThrow();
		assertTrue(coverage.contains("lacks the true-server ra_* runaway/middle-boss condition closure"));
	}

	private static String source(String world, int source) {
		String marker = "source=\"idldf5_under_01/world_N.xml#" + source + "\"";
		int firstMarker = world.indexOf(marker);
		int lastMarker = world.lastIndexOf(marker);
		int start = world.lastIndexOf("<condition ", firstMarker);
		int end = world.indexOf("</condition>", lastMarker);
		return world.substring(start, end + "</condition>".length());
	}

	private static String block(String value, String startMarker, String endMarker) {
		int start = value.indexOf(startMarker);
		int end = value.indexOf(endMarker, start);
		return value.substring(start, end + endMarker.length());
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
