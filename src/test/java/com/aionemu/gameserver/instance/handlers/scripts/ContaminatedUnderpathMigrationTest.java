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
}
