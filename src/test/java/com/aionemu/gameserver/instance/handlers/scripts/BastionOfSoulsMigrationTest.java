package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class BastionOfSoulsMigrationTest {
	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BastionOfSoulsInstance.java");

	@Test
	void retailDropDataOwnsKeysAndTreasureChests() throws Exception {
		String source = Files.readString(HANDLER);
		assertFalse(source.contains("onDropRegistered"));
		assertFalse(source.contains("regDropItem"));
		for (String privateReward : new String[] { "188058413", "188058361", "190080005", "190200000" }) {
			assertFalse(source.contains(privateReward), privateReward);
		}

		Document npcDrops = parse("src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_017.xml");
		for (String drop : new String[] {
			"246727:185000308:1", "246730:185000309:1", "246731:185000310:1", "246798:185000311:5",
			"246881:164002394:1", "246885:185000302:1", "247181:185000302:1", "246895:185000303:1",
			"246905:185000304:1"
		}) {
			String[] values = drop.split(":");
			assertTrue(exists(npcDrops, "//npc_drop[@npc_id='" + values[0] + "']//drop[@item_id='" + values[1]
				+ "' and @chance='100.00' and @min_amount='" + values[2] + "' and @max_amount='" + values[2] + "']"), drop);
		}

		Document chestDrops = parse("src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml");
		for (String chest : new String[] { "835484", "835485", "835486" }) {
			assertTrue(exists(chestDrops, "//npc_drop[@npc_id='" + chest
				+ "']//drop[@item_id='188057568' and @chance='100.00' and @eachmember='true']"), chest);
			assertTrue(exists(chestDrops, "//npc_drop[@npc_id='" + chest + "']/common_drop_group"), chest);
		}
	}

	@Test
	void retailConditionsOwnFinalBossesChestsAndExits() throws Exception {
		String source = Files.readString(HANDLER);
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance"));
		for (String legacy : new String[] { "bossWitch", "spawnBastionEasyChest", "spawnBastionNormalChest",
			"spawnBastionHardChest", "spawn(246493", "spawn(246494", "spawn(246495", "spawn(246496",
			"spawn(246497", "spawn(246498", "spawn(835484", "spawn(835485", "spawn(835486",
			"spawn(731805", "spawn(731806" }) {
			assertFalse(source.contains(legacy), legacy);
		}

		Document conditions = parse("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		String world = "//world[@id='302340000']";
		assertTrue(exists(conditions, world));
		for (String variable : new String[] { "boss_po", "equip", "main_destroy_start", "start_event",
			"ui_gauge_01", "ui_gauge_02", "ui_gauge_03" }) {
			assertTrue(exists(conditions, world + "/variable[@name='" + variable + "']"), variable);
		}
		for (String npc : new String[] { "246671", "247026", "247070", "247071", "247072", "247073",
			"246923", "246924" }) {
			assertTrue(exists(conditions, world + "//npc[@id='" + npc + "']"), npc);
		}
		for (String variable : new String[] { "da_play", "li_play", "mission_end", "final_boss", "item_a", "item_b",
			"item_c", "boss_e_kill", "boss_n_kill", "boss_h_kill" }) {
			assertTrue(exists(conditions, world + "/variable[@name='" + variable + "']"), variable);
		}
		for (String npc : new String[] { "246493", "246494", "246495", "246496", "246497", "246498", "835484",
			"835485", "835486", "731805", "731806" }) {
			assertTrue(exists(conditions, world + "//npc[@id='" + npc + "']"), npc);
		}
	}

	private static Document parse(String path) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Path.of(path).toFile());
	}

	private static boolean exists(Document document, String expression) throws Exception {
		return (boolean) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.BOOLEAN);
	}
}
