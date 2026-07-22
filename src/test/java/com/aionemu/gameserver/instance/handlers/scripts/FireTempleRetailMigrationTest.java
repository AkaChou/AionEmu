package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireTempleRetailMigrationTest {

	@Test
	void retailSpawnsOwnNamedVariantsAndHandlerKeepsOnlyChestFlow() throws Exception {
		String staticSpawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/320100000_Fire_Temple.xml"));
		for (String spawn : new String[] {
				"x=\"127.121834\" y=\"176.191223\" z=\"103\" h=\"15\" alternate_id=\"212839\" select_prob=\"3333\"",
				"x=\"153.003830\" y=\"299.778625\" z=\"126\" h=\"30\" alternate_id=\"212840\" select_prob=\"3333\"",
				"x=\"350.927582\" y=\"351.738922\" z=\"149\" h=\"45\" alternate_id=\"212841\" select_prob=\"3333\"",
				"x=\"322.319305\" y=\"431.269623\" z=\"137\" h=\"80\" alternate_id=\"212842\" select_prob=\"3333\"",
				"x=\"296.691101\" y=\"201.909195\" z=\"123\" h=\"15\" alternate_id=\"212843\" select_prob=\"3333\"",
				"x=\"421.993530\" y=\"93.189148\" z=\"122\" h=\"46\" alternate_id=\"214621\" select_prob=\"1000\""}) {
			assertTrue(staticSpawns.contains(spawn), spawn);
		}
		assertTrue(staticSpawns.contains("x=\"298.709503\" y=\"89.422447\" z=\"131\" h=\"45\""));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"214094\">"));
		assertFalse(staticSpawns.contains("<spawn npc_id=\"212845\""));
		assertFalse(staticSpawns.contains("alternate_id=\"212845\""));

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FireTempleInstance.java"));
		for (String legacy : new String[] { "onDropRegistered", "onInstanceCreate", "GameWorldServices",
				"188051411", "188051412", "188052826", "188053787", "188053994", "170030000" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
		assertTrue(handler.contains("case 212846, 214621"));
		for (String chestId : new String[] { "833523", "833524", "833525" }) {
			assertTrue(handler.contains(chestId), chestId);
		}
	}
}
