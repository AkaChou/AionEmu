package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

class ContaminatedUnderpathMigrationTest {

	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void eventAndLunaHandlersUseRetailSpawnsScoresAndPersistentSettlement() throws Exception {
		assertMigrated("event/Event_ContaminatedUnderpathInstance.java", "TIMEATTACK_PLAY_START",
			"InstanceSettlementService.settleTimeAttack(");
		assertMigrated("luna/ContaminatedUnderpathInstance.java", "IDLUNA_DEF_PHASE_1_1",
			"InstanceSettlementService.settleLuna(");
	}

	@Test
	void eventRetailConditionsOwnBothFinalBosses() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = conditions.substring(conditions.indexOf("<world id=\"301631000\""));
		world = world.substring(0, world.indexOf("</world>"));
		assertTrue(world.contains("<variable name=\"wave_4_start\"/>"));
		assertTrue(world.contains("Wave_4_Start == 1) &amp;&amp; (SpecialServer_Cond == 0"));
		assertTrue(world.contains("Wave_4_Start == 1) &amp;&amp; (SpecialServer_Cond == 1"));
		assertTrue(world.contains("npc id=\"248525\""));
		assertTrue(world.contains("npc id=\"248947\""));
	}

	@Test
	void inactiveHardMapHasNoPrivateServerHandlerOrSpawns() throws Exception {
		assertFalse(Files.exists(HANDLERS.resolve("event/IDEvent_Def_HInstance.java")));
		assertFalse(Files.exists(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301632000_IDEvent_Def_H.xml")));

		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(Path.of("src/main/resources/aion/data/static_data/world_maps.xml").toFile());
		var maps = document.getElementsByTagName("map");
		boolean activeEventMap = false;
		boolean inactiveHardMap = false;
		for (int i = 0; i < maps.getLength(); i++) {
			String id = maps.item(i).getAttributes().getNamedItem("id").getNodeValue();
			activeEventMap |= id.equals("301631000");
			inactiveHardMap |= id.equals("301632000");
		}
		assertTrue(activeEventMap);
		assertFalse(inactiveHardMap);
	}

	@Test
	void itemOwnershipPreservesLogoutAndPersistentEventCurrency() throws Exception {
		for (String relative : new String[] { "event/Event_ContaminatedUnderpathInstance.java",
			"luna/ContaminatedUnderpathInstance.java" }) {
			assertFalse(Files.readString(HANDLERS.resolve(relative)).contains("decreaseByItemId"), relative);
		}

		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		assertTrue(item(items, "182007405").contains("ownership_world=\"301630000\""));
		assertFalse(item(items, "186000470").contains("ownership_world"));
		assertFalse(item(items, "186000495").contains("ownership_world"));
	}

	private static void assertMigrated(String relative, String startVariable, String settlement) throws Exception {
		String source = Files.readString(HANDLERS.resolve(relative));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"" + startVariable + "\""), relative);
		assertTrue(source.contains("score.scoreApplyType() == 3"), relative);
		assertTrue(source.contains("scheduleDeadline(\"prepare\""), relative);
		assertTrue(source.contains("scheduleDeadline(\"expire\""), relative);
		assertTrue(source.contains("scheduleDeadline(\"settle\""), relative);
		assertTrue(source.contains("runtimeState().snapshot(STATE + \"kill.\")"), relative);
		assertTrue(source.contains("runtimeState().put(playerRewardKey("), relative);
		assertTrue(source.contains(settlement), relative);
		for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "onDropRegistered",
			"handleUseItemFinish", "ItemService.addItem", "RewardType.QUEST", "protected void sp",
			"startContaminedUnderPath", "spawn(" }) {
			assertFalse(source.contains(legacy), relative + ": " + legacy);
		}
	}

	private static String item(String source, String itemId) {
		int start = source.indexOf("<item_template id=\"" + itemId + "\"");
		return source.substring(start, source.indexOf("</item_template>", start));
	}
}
