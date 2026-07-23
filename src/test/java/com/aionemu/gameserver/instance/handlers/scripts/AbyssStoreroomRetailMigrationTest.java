package com.aionemu.gameserver.instance.handlers.scripts;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssStoreroomRetailMigrationTest {
	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void retailDataOwnsStoreroomMechanics() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		int[] worldIds = { 300120000, 300130000, 300140000 };
		int[] conditionCounts = { 12, 11, 11 };
		int[] npcCounts = { 32, 38, 38 };
		for (int i = 0; i < worldIds.length; i++) {
			String world = block(conditions, "<world id=\"" + worldIds[i] + "\"", "</world>");
			assertEquals(conditionCounts[i], count(world, "<condition "));
			assertEquals(npcCounts[i], count(world, "<npc "));
		}

		String[] spawnFiles = {
			"300120000_Grave_Of_Steel_Storeroom.xml",
			"300130000_Twilight_Battlefield_Storeroom.xml",
			"300140000_Isle_Of_Roots_Storeroom.xml"
		};
		int[] spotCounts = { 212, 219, 221 };
		for (int i = 0; i < spawnFiles.length; i++) {
			String spawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/" + spawnFiles[i]));
			assertEquals(spotCounts[i], count(spawns, "<spot "));
			assertFalse(spawns.contains("731580"));
			assertFalse(spawns.contains("254574"));
		}

		String chests = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/chests/chest_templates.xml"));
		String npcTemplates = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (String npcId : new String[] { "700540", "700542", "700544" }) {
			assertTrue(chests.contains("<chest npcid=\"" + npcId + "\">"), npcId);
			String template = block(npcTemplates, "<npc_template npc_id=\"" + npcId + "\"", "</npc_template>");
			assertTrue(template.contains("ai=\"chest\""), npcId);
		}

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		for (String group : new String[] { "CROTAN", "DKISAS", "LAMIREN" }) {
			assertTrue(drops.contains("KEY_IDAB1_REWARD_" + group), group);
		}
		for (String itemId : new String[] { "185000059", "185000060", "185000064", "185000065",
			"185000069", "185000070" }) {
			assertTrue(drops.contains("item_id=\"" + itemId + "\""), itemId);
		}
		for (String wrong : new String[] { "KEY_IDAB1_REWARD_ROOT", "KEY_IDAB1_REWARD_IRON",
			"KEY_IDAB1_REWARD_TWILIGHT", "185000251", "185000256", "185000261" }) {
			assertFalse(drops.contains(wrong), wrong);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AbyssStoreroomInstance.java"));
		assertTrue(handler.contains("onPlayerLogOut"));
		for (String legacy : new String[] { "onLeaveInstance", "onDropRegistered", "onDie(", "scheduleDeadline",
			"spawn(", "CHEST_STAGE_DURATION", "Config" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (int worldId : worldIds) {
			String world = block(coverage, "<world ", "/>", coverage.indexOf("id=\"" + worldId + "\""));
			assertTrue(world.contains("behavior=\"HANDLER\""), Integer.toString(worldId));
			assertTrue(world.contains("handler logout key cleanup"), Integer.toString(worldId));
		}
	}

	@Test
	void retailDataAndHandlersOwnLowerStoreroomMechanics() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		int[] worldIds = { 300050000, 300060000, 300070000, 300080000, 300090000 };
		int[] conditionCounts = { 12, 156, 12, 6, 274 };
		int[] npcCounts = { 12, 468, 12, 168, 826 };
		for (int i = 0; i < worldIds.length; i++) {
			String world = block(conditions, "<world id=\"" + worldIds[i] + "\"", "</world>");
			assertEquals(conditionCounts[i], count(world, "<condition "));
			assertEquals(conditionCounts[i], count(world, "<slot>"));
			assertEquals(npcCounts[i], count(world, "<npc "));
			assertTrue(world.contains("<variable name=\"lightin\"/>"));
			assertTrue(world.contains("<variable name=\"darkin\"/>"));
		}

		String[] spawnFiles = {
			"300050000_Carpus_Isle_Storeroom.xml",
			"300060000_Sulfur_Tree_Nest.xml",
			"300070000_Hamate_Isle_Storeroom.xml",
			"300080000_Left_Wing_Chamber.xml",
			"300090000_Right_Wing_Chamber.xml"
		};
		int[] spotCounts = { 49, 238, 44, 197, 172 };
		for (int i = 0; i < spawnFiles.length; i++) {
			String spawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/" + spawnFiles[i]));
			assertEquals(spotCounts[i], count(spawns, "<spot "), spawnFiles[i]);
			assertTrue(spawns.contains("npc_id=\"283080\""), spawnFiles[i]);
		}
		String carpusSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300050000_Carpus_Isle_Storeroom.xml"));
		assertEquals(40, count(carpusSpawns, "alternate_id="));
		assertEquals(40, count(carpusSpawns, "select_prob="));
		assertTrue(carpusSpawns.contains("alternate_id=\"214752,214755,214754\" select_prob=\"7500,5000,2500\""));
		assertTrue(carpusSpawns.contains("alternate_id=\"214761,214762\" select_prob=\"6666,3333\""));

		String hamateSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300070000_Hamate_Isle_Storeroom.xml"));
		assertEquals(39, count(hamateSpawns, "alternate_id="));
		assertEquals(39, count(hamateSpawns, "select_prob="));
		assertTrue(hamateSpawns.contains("x=\"390.360504\" y=\"507.690796\" z=\"104.753662\" h=\"113\" alternate_id=\"214780,214781\" select_prob=\"6666,3333\""));
		assertTrue(hamateSpawns.contains("x=\"617.857422\" y=\"460.123230\" z=\"104.753662\" h=\"53\" alternate_id=\"214784\" select_prob=\"5000\""));
		assertTrue(hamateSpawns.contains("x=\"504.002960\" y=\"612.168579\" z=\"103.785400\" h=\"90\" alternate_id=\"215450\" select_prob=\"5000\""));

		String carpusZones = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/zones/zones_300050000.xml"));
		String carpusTimerZone = block(carpusZones,
			"<zone mapid=\"300050000\" name=\"CARPUS_ISLE_STOREROOM_TIMER_300050000\"", "</zone>");
		assertTrue(carpusTimerZone.contains("bottom=\"196.759430\" top=\"214.759430\""));
		assertEquals(6, count(carpusTimerZone, "<point "));
		assertTrue(carpusTimerZone.contains("x=\"478.164185\" y=\"560.796875\""));
		assertTrue(carpusTimerZone.contains("x=\"533.403320\" y=\"596.967468\""));

		String hamateZones = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/zones/zones_300070000.xml"));
		String hamateTimerZone = block(hamateZones,
			"<zone mapid=\"300070000\" name=\"HAMATE_ISLE_STOREROOM_TIMER_300070000\"", "</zone>");
		assertTrue(hamateTimerZone.contains("bottom=\"84.487892\" top=\"112.487892\""));
		assertEquals(6, count(hamateTimerZone, "<point "));
		assertTrue(hamateTimerZone.contains("x=\"499.636536\" y=\"410.997559\""));
		assertTrue(hamateTimerZone.contains("x=\"473.416016\" y=\"466.433838\""));

		String[] lowerZoneFiles = { "zones_300060000.xml", "zones_300080000.xml", "zones_300090000.xml" };
		String[] lowerZoneNames = { "SULFUR_TREE_NEST_TIMER_300060000", "LEFT_WING_CHAMBER_TIMER_300080000",
			"RIGHT_WING_CHAMBER_TIMER_300090000" };
		String[] lowerZoneHeights = { "bottom=\"160.000000\" top=\"180.000000\"",
			"bottom=\"350.000000\" top=\"380.000000\"", "bottom=\"100.000000\" top=\"130.000000\"" };
		String[][] lowerZonePoints = {
			{ "443.120331,380.000000", "480.593506,380.026062", "528.160156,383.527344",
				"543.018860,404.392731", "546.612183,434.819427", "381.118286,434.744507",
				"383.683289,404.708862", "398.858093,383.843475" },
			{ "518.667480,570.844910", "518.961060,601.927246", "576.131897,602.158569",
				"576.131958,570.516724" },
			{ "186.231094,305.222046", "207.269241,350.633484", "246.538422,360.804810",
				"283.890503,360.786163", "321.142273,349.957031", "339.833496,305.306549" }
		};
		for (int i = 0; i < lowerZoneFiles.length; i++) {
			String zones = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/zones/" + lowerZoneFiles[i]));
			String timerZone = block(zones, "<zone mapid=\"", "</zone>", zones.indexOf(lowerZoneNames[i]));
			assertTrue(timerZone.contains(lowerZoneHeights[i]), lowerZoneNames[i]);
			assertEquals(lowerZonePoints[i].length, count(timerZone, "<point "), lowerZoneNames[i]);
			for (String point : lowerZonePoints[i]) {
				String[] coordinates = point.split(",");
				assertTrue(timerZone.contains("x=\"" + coordinates[0] + "\" y=\"" + coordinates[1] + "\""),
					lowerZoneNames[i] + ": " + point);
			}
		}

		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		assertTrue(npcAi.contains("<npc id=\"283080\" name=\"Ab_RaceCheck\" ai=\"Ab_RaceCheck\""));
		assertTrue(npcAi.contains("<npc id=\"856595\" name=\"BAb1_Race_Check\" ai=\"IDAb_Race_Check\""));
		String raceAttackPattern = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml"));
		assertTrue(raceAttackPattern.contains("<string>LIGHTIN</string><set>1</set>"));
		assertTrue(raceAttackPattern.contains("<string>DARKIN</string><set>1</set>"));
		String raceSightPattern = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ab1_ver49_ssh.xml"));
		assertTrue(raceSightPattern.contains("<string>racecheck</string><set>1</set>"));
		assertTrue(raceSightPattern.contains("<string>racecheck</string><set>2</set>"));
		for (int worldId : new int[] { 300050000, 300070000 }) {
			String world = block(conditions, "<world id=\"" + worldId + "\"", "</world>");
			assertTrue(world.contains("<variable name=\"racecheck\"/>"));
		}
		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId = 185000033; itemId <= 185000038; itemId++) {
			assertTrue(drops.contains("item_id=\"" + itemId + "\""), Integer.toString(itemId));
		}
		for (String ownership : new String[] { "ownership_world=\"300050000\"",
			"ownership_world=\"300070000\"" }) {
			assertTrue(items.contains(ownership), ownership);
		}

		String carpus = Files.readString(HANDLERS.resolve("CarpusIsleStoreroomInstance.java"));
		String hamate = Files.readString(HANDLERS.resolve("HamateIsleStoreroomInstance.java"));
		for (String handler : new String[] { carpus, hamate }) {
			assertTrue(handler.contains("onPlayerLogOut"));
			assertTrue(handler.contains("new SM_QUEST_ACTION"));
			assertTrue(handler.contains("public void onEnterZone(Player player, ZoneInstance zone)"));
			assertTrue(handler.contains("private synchronized void start"));
			assertTrue(handler.contains("scheduleDeadline(\"treasure\", deadline"));
			assertTrue(handler.contains("if (deadline > 0 && deadline <= System.currentTimeMillis())"));
			assertTrue(handler.contains("(deadline - System.currentTimeMillis()) / 1000"));
			assertTrue(handler.contains("STR_MSG_INSTANCE_START_IDABRE"));
			assertFalse(handler.contains("onLeaveInstance"));
			assertFalse(handler.contains("onDropRegistered"));
			assertFalse(handler.contains("FlyRing"));
			assertFalse(handler.contains("onPassFlyingRing"));
		}
		assertTrue(carpus.contains("CARPUS_ISLE_STOREROOM_TIMER_300050000"));
		assertTrue(carpus.contains("runtimeState().getLong(\"carpus.deadline\", 0) != 0"));
		assertTrue(hamate.contains("HAMATE_ISLE_STOREROOM_TIMER_300070000"));
		assertTrue(hamate.contains("runtimeState().getLong(\"hamate.deadline\", 0) != 0"));
		assertFalse(hamate.contains("spawnSelectedNpc"));
		assertFalse(hamate.contains("Rnd."));
		String sulfur = Files.readString(HANDLERS.resolve("SulfurTreeNestInstance.java"));
		String left = Files.readString(HANDLERS.resolve("LeftWingChamberInstance.java"));
		String right = Files.readString(HANDLERS.resolve("RightWingChamberInstance.java"));
		String[] lowerHandlers = { sulfur, left, right };
		String[] lowerHandlerNames = { "sulfur", "leftwing", "rightwing" };
		for (int i = 0; i < lowerHandlers.length; i++) {
			String handler = lowerHandlers[i];
			assertTrue(handler.contains("public void onEnterZone(Player player, ZoneInstance zone)"));
			assertTrue(handler.contains("private synchronized void startTimer()"));
			assertTrue(handler.contains("System.currentTimeMillis() + 900_000"));
			assertTrue(handler.contains("runtimeState().getLong(\"" + lowerHandlerNames[i] + ".deadline\", 0)"));
			assertTrue(handler.contains("scheduleDeadline(\"treasure\", deadline, this::expireTreasure)"));
			assertTrue(handler.contains("if (deadline > 0 && deadline <= System.currentTimeMillis())"));
			assertTrue(handler.contains("(deadline - System.currentTimeMillis()) / 1000"));
			assertTrue(handler.contains("new SM_QUEST_ACTION(0, 900)"));
			assertTrue(handler.contains("STR_MSG_INSTANCE_START_IDABRE"));
			assertTrue(handler.contains("sendMsg(1400244)"));
			assertTrue(handler.contains("deleteTreasureBoxes()"));
			String enterInstance = block(handler, "public void onEnterInstance", "\n\t}");
			assertTrue(enterInstance.contains("syncTimer(player)"));
			assertFalse(enterInstance.contains("startTimer()"));
			for (String legacy : new String[] { "FlyRing", "onPassFlyingRing", "onDie(", "CHEST_STAGE",
				"TeleportService2", "EXIT_DELAY", "215424", "219617" }) {
				assertFalse(handler.contains(legacy), lowerHandlerNames[i] + ": " + legacy);
			}
		}
		assertTrue(sulfur.contains("SULFUR_TREE_NEST_TIMER_300060000"));
		assertTrue(left.contains("LEFT_WING_CHAMBER_TIMER_300080000"));
		assertTrue(right.contains("RIGHT_WING_CHAMBER_TIMER_300090000"));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (int worldId : worldIds) {
			String world = block(coverage, "<world ", "/>", coverage.indexOf("id=\"" + worldId + "\""));
			assertTrue(world.contains("behavior=\"HANDLER\""), Integer.toString(worldId));
			assertTrue(world.contains("exact timer zone"), Integer.toString(worldId));
			assertTrue(world.contains("persistent 15-minute deadline"), Integer.toString(worldId));
		}
	}

	private static String block(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start);
		return source.substring(start, end);
	}

	private static String block(String source, String startToken, String endToken, int position) {
		int start = source.lastIndexOf(startToken, position);
		int end = source.indexOf(endToken, position);
		return source.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
