package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RentusBaseRetailMigrationTest {

	@Test
	void handlerDoesNotDuplicateRetailWeaponAndOilSpawns() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/RentusBaseInstance.java"));
		for (String removed : new String[] { "spawnDirectFiringGunIDYun", "case 282394", "case 702677", "case 702688" }) {
			assertFalse(source.contains(removed), removed);
		}
		for (String retained : new String[] { "case 217313", "case 217315", "case 217316", "case 217317",
				"case 283000", "case 283001", "case 701151", "case 701100" }) {
			assertTrue(source.contains(retained), retained);
		}
	}

	@Test
	void retailRaceControllerOwnsWeaponSelectionAndConditionSpawns() throws Exception {
		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300280000_Rentus_base.xml"));
		assertTrue(spawns.contains("<spawn npc_id=\"855952\""));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300280000");
		for (int npcId = 702677; npcId <= 702688; npcId++) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), Integer.toString(npcId));
		}
		assertTrue(world.contains("weapon == 1"));
		assertTrue(world.contains("weapon == 2"));

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300280000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail race controller and condition spawns own faction siege weapons"));
		assertTrue(ownership.contains("Pattern owns oil-cask spill"));
	}

	private static String worldBlock(String conditions, String worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		return conditions.substring(start, conditions.indexOf("</world>", start));
	}
}
