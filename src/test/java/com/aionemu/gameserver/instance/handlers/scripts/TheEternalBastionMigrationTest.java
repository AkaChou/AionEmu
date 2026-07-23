package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TheEternalBastionMigrationTest {

	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheEternalBastionInstance.java");

	@Test
	void handlerUsesPersistentRetailRuntime() throws Exception {
		String source = Files.readString(HANDLER);
		assertTrue(source.contains("runtimeState()"));
		assertTrue(source.contains("scheduleDeadline"));
		assertTrue(source.contains("InstanceSettlementService.timeAttackPlan"));
		assertTrue(source.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(source.contains("supportsRetailNpcScore"));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("java.util.concurrent.Future"));
		assertFalse(source.contains("startInstanceTask"));
		assertFalse(source.contains("spawn("));
		assertFalse(source.contains("onDropRegistered"));
		assertFalse(source.contains("handleUseItemFinish"));
		assertFalse(source.contains("802185"));
		assertFalse(source.contains("186000051"));
		assertFalse(source.contains("186000052"));
		assertFalse(source.contains("186000236"));
		assertFalse(source.contains("186000237"));
	}

	@Test
	void retailDataCoversBastionFlowAndSettlement() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = conditions.substring(conditions.indexOf("<world id=\"300540000\""),
			conditions.indexOf("</world>", conditions.indexOf("<world id=\"300540000\"")));
		assertTrue(world.contains("<variable name=\"timewave_down\"/>"));
		assertTrue(world.contains("Wave_Z1_S4_01"));
		assertTrue(world.contains("Wave_04_Boss"));
		assertTrue(world.contains("castle_gate_02_Bomb"));
		assertTrue(world.contains("npc id=\"231130\""));

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		int row = rewards.indexOf("world_id=\"300540000\"");
		assertTrue(row >= 0);
		String rewardRow = rewards.substring(rewards.lastIndexOf("<row", row), rewards.indexOf("/>", row));
		assertTrue(rewardRow.contains("base_score=\"20000\""));
		assertTrue(rewardRow.contains("s_score_minimum=\"90000\""));
		assertTrue(rewardRow.contains("time_limit=\"1800\""));
	}

	@Test
	void temporaryItemsSurviveLogoutButAreRemovedOnDirectExit() throws Exception {
		String handler = Files.readString(HANDLER);
		assertTrue(method(handler, "onLeaveInstance").contains("removeItems(player)"));
		assertFalse(method(handler, "onPlayerLogOut").contains("removeItems(player)"));
		assertTrue(method(handler, "exitPlayers").contains("onLeaveInstance(player)"));

		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (String itemId : new String[] { "185000137", "182006996", "182006997" }) {
			int start = items.indexOf("<item_template id=\"" + itemId + "\"");
			String item = items.substring(start, items.indexOf("</item_template>", start));
			assertTrue(item.contains("ownership_world=\"300540000\""), itemId);
		}
	}

	private static String method(String source, String name) {
		int start = source.indexOf(name + "(");
		return source.substring(start, source.indexOf("\n\t}", start));
	}
}
