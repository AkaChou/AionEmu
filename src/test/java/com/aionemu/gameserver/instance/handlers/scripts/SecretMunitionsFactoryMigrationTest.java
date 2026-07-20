package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SecretMunitionsFactoryMigrationTest {

	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/luna/SecretMunitionsFactoryInstance.java");
	private static final Path SCORE_PACKET = Path.of(
		"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_INSTANCE_SCORE.java");

	@Test
	void handlerUsesRetailScoresDeadlinesAndPersistentLunaSettlement() throws Exception {
		String source = Files.readString(HANDLER);
		for (String required : new String[] { "FINAL_BOSS = 244147", "score.scoreApplyType() == 3",
			"scheduleDeadline(\"prepare\"", "scheduleDeadline(\"expire\"", "scheduleDeadline(\"settle\"",
			"runtimeState().snapshot(STATE + \"kill.\")", "runtimeState().put(playerRewardKey(",
			"InstanceSettlementService.settleLuna(" }) {
			assertTrue(source.contains(required), required);
		}
		for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "onDropRegistered",
			"handleUseItemFinish", "ItemService.addItem", "Rnd.", "protected void sp", "spawn(", "243664",
			"startFactoryRaid", "startFactoryTask", "PlayerReviveService", "TeleportService2" }) {
			assertFalse(source.contains(legacy), legacy);
		}

		String packet = Files.readString(SCORE_PACKET);
		assertTrue(packet.contains("InstanceSettlementService.lunaPlan(mapId, smfr.getRank())"));
		assertTrue(packet.contains("plan.items().get(slot).itemId()"));
		assertFalse(packet.contains("188055648"));
	}
}
