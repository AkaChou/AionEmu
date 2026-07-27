package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class CradleOfEternityMigrationTest {

	private static final String WORLD = "//world[@id='301550000']";

	@Test
	void retailConditionsOwnCradleProgressionAndRaceSpawns() throws Exception {
		Document conditions = parse("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		assertEquals(51, count(conditions, WORLD + "/variable"));
		assertEquals(358, count(conditions, WORLD + "/condition"));
		assertEquals(382, count(conditions, WORLD + "//slot"));
		assertEquals(0, count(conditions, WORLD + "//party"));

		for (String variable : new String[] { "ideternity_02_d_button", "ideternity_02_da", "ideternity_02_da_01",
			"ideternity_02_da_02", "ideternity_02_da_03", "ideternity_02_da_04", "ideternity_02_fly",
			"ideternity_02_li", "ideternity_02_li_01", "ideternity_02_li_02", "ideternity_02_li_03",
			"ideternity_02_li_04", "ideternity_02_save_01", "ideternity_02_save_02",
			"ideternity_02_save_03", "ideternity_02_save_04", "ideternity_02_strong_a",
			"ideternity_02_strong_b", "ideternity_02_strong_c", "ideternity_02_strong_d",
			"ideternity_02_teleport_01", "named_die_01", "named_die_02", "named_die_03" }) {
			assertTrue(exists(conditions, WORLD + "/variable[@name='" + variable + "']"), variable);
		}

		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_li == 1']//npc[@id='806281']"));
		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_da == 1']//npc[@id='806286']"));
		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_strong_c == 2']//npc[@id='834123']"));
		assertTrue(exists(conditions, WORLD + "/condition[contains(@expression, 'ideternity_02_da_03 > 11') "
			+ "and contains(@expression, 'ideternity_02_li_03 > 11')]//npc[@id='220664']"));
		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_d_button == 1']//npc[@id='834007']"));
		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_d_button == 2']//npc[@id='220566']"));
		assertTrue(exists(conditions, WORLD + "/condition[@expression='ideternity_02_teleport_01 == 1']//npc[@id='834000']"));
		assertTrue(exists(conditions, WORLD + "/condition[contains(@expression, 'named_die_03 == 2')]//npc[@id='835349']"));
		assertTrue(exists(conditions, WORLD + "/condition[contains(@expression, 'named_die_03 == 2')]//npc[@id='835350']"));
	}

	@Test
	void retailDataKeepsControllersTreasuresAndQuestSensoryMarkers() throws Exception {
		Document conditions = parse("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");
		for (String npcId : new String[] { "220557", "220680", "834000", "834153" }) {
			assertTrue(exists(conditions, WORLD + "/condition[@source='IDEternity_02/world_N.xml#selected-unconditional']"
				+ "//npc[@id='" + npcId + "']"), npcId);
		}
		for (String npcId : new String[] { "206548", "206555", "206590", "206591", "206592" }) {
			assertTrue(exists(conditions, WORLD + "//npc[@id='" + npcId + "']/sensory_area"), npcId);
		}

		Document spawns = parse(
			"src/main/resources/aion/data/static_data/spawns/Instances/301550000_Cradle_Of_Eternity.xml");
		for (String npcId : new String[] { "206547", "206549", "206551", "703374" }) {
			assertTrue(exists(spawns, "//spawn[@npc_id='" + npcId + "']"), npcId);
		}
	}

	@Test
	void retailPatternsOwnBossesDoorsAndStageVariables() throws Exception {
		String commonPatterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ideternity_02_ssh.xml"));
		for (String value : new String[] { "IDEternity_02_Start", "IDEternity_02_MovingRoom_Lever_01",
			"IDEternity_02_D_Swich", "IDEternity_Maidengolem", "IDEternity_02_C_Key_Ra_75_Ae",
			"ideternity_02_teleport_01", "ideternity_02_fly" }) {
			assertTrue(commonPatterns.contains(value), value);
		}

		String namedPatterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_ideternity_02_named_ssh.xml"));
		for (String value : new String[] { "IDEternity_02_Tower", "IDEternity_02_Nepilim",
			"IDEternity_02_SnakeM_Fly", "named_die_01", "named_die_02", "named_die_03" }) {
			assertTrue(namedPatterns.contains(value), value);
		}
	}

	@Test
	void retailDropsOwnAllThreeBossRewardTiers() throws Exception {
		Document drops = parse("src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_009.xml");
		assertEquals(6, count(drops, "//npc_drop[@npc_id='220526']/common_drop_group"));
		assertEquals(6, count(drops, "//npc_drop[@npc_id='220534']/common_drop_group"));
		assertEquals(9, count(drops, "//npc_drop[@npc_id='220593']/common_drop_group"));
		for (String npcId : new String[] { "220526", "220534", "220593" }) {
			assertTrue(exists(drops, "//npc_drop[@npc_id='" + npcId
				+ "']/common_drop_group[@name='MATTER_ENCHANT_CPSTONE_ID_01']"), npcId);
		}
	}

	@Test
	void scriptNpcsOwnAllSixRetailAltarCallbacks() throws Exception {
		Document scripts = parse("src/main/resources/aion/definitions/compact/script-npcs.xml");
		assertEquals(6, count(scripts, "//item_gate_variable[@world_id='301550000']"));
		for (String npcId : new String[] { "834006", "834019", "834020", "834021", "834022", "834007" }) {
			assertTrue(exists(scripts, "//item_gate_variable[@npc_id='" + npcId + "']"), npcId);
		}
		Document templates = parse("src/main/resources/aion/data/static_data/npcs/npc_template.xml");
		for (String npcId : new String[] { "834006", "834019", "834020", "834021", "834022", "834007" }) {
			assertTrue(exists(templates, "//npc_template[@npc_id='" + npcId + "' and @ai='useitem']"), npcId);
		}
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/cradleOfEternity/Altar_Of_EarthAI2.java")));
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/CradleOfEternityInstance.java"));
		assertFalse(handler.contains("handleUseItemFinish("));
		assertTrue(handler.contains("removeEffect(21340)"));
		assertTrue(handler.contains("removeEffect(21344)"));
		for (String legacy : new String[] { "Rnd", "onDropRegistered(", "onInstanceCreate(", "onEnterInstance(",
			"onDie(", "onEnterZone(", "runtimeState()", "setDoorState(", "SpawnIDEternity02Race(",
			"spawn(", "806056", "281446", "834005" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
	}

	private static Document parse(String path) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Path.of(path).toFile());
	}

	private static boolean exists(Document document, String expression) throws Exception {
		return (boolean) XPathFactory.newInstance().newXPath().evaluate(expression, document, XPathConstants.BOOLEAN);
	}

	private static int count(Document document, String expression) throws Exception {
		return ((Double) XPathFactory.newInstance().newXPath().evaluate("count(" + expression + ")", document,
			XPathConstants.NUMBER)).intValue();
	}
}
