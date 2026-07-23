package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EsoterraceRetailMigrationTest {

	@Test
	void handlerKeepsOnlyProvenRetailFallbacksAndNonPatternBridges() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/EsoterraceInstance.java"));
		for (String retained : new String[] { "case 282291", "case 217185", "case 217282", "case 286930",
				"case 217195", "case 282293", "case 217205", "case 282295", "case 217289" }) {
			assertTrue(source.contains(retained), retained);
		}
			for (String retailOwned : new String[] { "case 217281", "case 217283", "case 217284", "case 217204",
					"case 217206" }) {
				assertFalse(source.contains(retailOwned), retailOwned);
			}
			assertFalse(source.contains("onPlayerLogOut"));
			assertTrue(source.contains("onLeaveInstance"));
			assertTrue(source.contains("decreaseByItemId(185000111"));
			String item = Files.readAllLines(Path.of(
				"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml")).stream()
				.filter(line -> line.contains("id=\"185000111\"")).findFirst().orElseThrow();
			assertFalse(item.contains("ownership_world"));
	}

	@Test
	void retailConditionsOwnBossRewardsAndKeyManagerProgress() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300250000");
		assertTrue(world.contains("IDF4Re_Dra_01_Boss_Kill == 1"));
		assertTrue(world.contains("IDF4Re_Dra_02_KeyNamed_Kill == 3"));
		assertTrue(world.contains("IDF4Re_Dra_03_Boss_Kill &gt;= 1"));
		assertTrue(world.contains("IDF4Re_Dra_05_Boss_Kill &gt;= 1"));
		for (String npcId : new String[] { "217650", "799591", "282298", "701023", "282358", "701027", "205437" }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300250000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail condition spawns and supported Pattern own proven boss rewards, key-manager progress and doors"));
			assertTrue(ownership.contains("handler retains unsupported Pattern fallbacks, movies, legacy windstreams and normal-leave item cleanup"));
	}

	private static String worldBlock(String conditions, String worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		return conditions.substring(start, conditions.indexOf("</world>", start));
	}
}
