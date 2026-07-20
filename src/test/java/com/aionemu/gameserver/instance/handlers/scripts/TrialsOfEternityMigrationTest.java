package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TrialsOfEternityMigrationTest {

	private static final Path CONDITIONS = Path.of(
		"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/301560000_Trials_Of_Eternity.xml");
	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TrialsOfEternityInstance.java");

	@Test
	void retailDataOwnsTrialsFlowSpawnsAndDrops() throws Exception {
		String world = worldBlock(Files.readString(CONDITIONS), "301560000");
		assertEquals(42, count(world, "<variable "));
		assertEquals(574, count(world, "<condition "));
		assertEquals(576, count(world, "<slot>"));
		for (String required : new String[] { "semibossend", "waveend", "bossrise", "bossend",
				"<npc id=\"247035\"", "<npc id=\"247036\"" }) {
			assertTrue(world.contains(required), required);
		}

		assertEquals(5, count(world, "<party probability=\"2000\""));
		assertEquals(5, count(world, "<npc id=\"731745\""));
		assertEquals(20, count(world, "<npc id=\"731750\""));

		String spawns = Files.readString(SPAWNS);
		assertEquals(35, count(spawns, "<spot "));
		for (String legacy : new String[] { "npc_id=\"246408\"", "npc_id=\"247035\"",
				"npc_id=\"247036\"", "npc_id=\"731743\"", "npc_id=\"731745\"", "npc_id=\"731750\"" }) {
			assertFalse(spawns.contains(legacy), legacy);
		}

		String drops = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_017.xml"))
			+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		for (String retailDrop : new String[] { "npc_id=\"246410\"", "item_id=\"185000297\"",
				"npc_id=\"731745\"", "item_id=\"185000298\"", "npc_id=\"731746\"",
				"item_id=\"185000299\"", "npc_id=\"731747\"", "item_id=\"185000300\"",
				"npc_id=\"246408\"", "item_id=\"185000301\"" }) {
			assertTrue(drops.contains(retailDrop), retailDrop);
		}
	}

	@Test
	void handlerOnlyKeepsRestrictedLibraryAndInstanceItemCleanup() throws Exception {
		String source = Files.readString(HANDLER);
		for (String required : new String[] { "npc.getNpcId() != 731736", "decreaseByItemId(185000297, 1)",
				"TeleportService2.teleportTo", "new SM_SYSTEM_MESSAGE(1404075)", "onPlayerLogOut", "onLeaveInstance",
				"185000298", "185000299", "185000300", "185000301" }) {
			assertTrue(source.contains(required), required);
		}
		for (String legacy : new String[] { "Future", "GameThreadPoolServices", "ThreadPoolManager", "spawn(",
				"onDie", "onDropRegistered", "onPassFlyingRing", "AbyssPointsService", "ItemService.addItem",
				"SM_PLAY_MOVIE", "StaticDoor", "731751", "731752" }) {
			assertFalse(source.contains(legacy), legacy);
		}
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
