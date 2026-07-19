package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class NpcControllerTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void onlyGroundMonstersHaveTheirSpawnHeightCorrected() {
		assertTrue(NpcController.shouldCorrectGroundSpawnHeight(true, true, false, NpcType.ATTACKABLE));
		assertTrue(NpcController.shouldCorrectGroundSpawnHeight(true, true, false, NpcType.AGGRESSIVE));
		assertFalse(NpcController.shouldCorrectGroundSpawnHeight(true, true, true, NpcType.ATTACKABLE));
		assertFalse(NpcController.shouldCorrectGroundSpawnHeight(false, true, false, NpcType.ATTACKABLE));
		assertFalse(NpcController.shouldCorrectGroundSpawnHeight(true, false, false, NpcType.ATTACKABLE));
		assertFalse(NpcController.shouldCorrectGroundSpawnHeight(true, true, false, NpcType.NON_ATTACKABLE));
	}

	@Test
	void scheduleRespawnReturnsTaskSoDuplicateSchedulesCanBeCancelled() {
		NpcController controller = new NpcController();
		TestNpc npc = objenesis.newInstance(TestNpc.class);
		npc.spawn = objenesis.newInstance(TestSpawnTemplate.class);
		controller.setOwner(npc);

		Future<?> respawnTask = controller.scheduleRespawn();

		try {
			assertNotNull(respawnTask);
		} finally {
			if (respawnTask != null) {
				respawnTask.cancel(false);
			}
		}
	}

	@Test
	void deadNpcRestoresLootStatusImmediatelyAfterDeathPacket() throws Exception {
		String controller = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/NpcController.java"));
		int deathPacket = controller.indexOf("new SM_EMOTION(owner, EmotionType.DIE");
		assertTrue(controller.indexOf("dropService().see(player, owner)", deathPacket) > deathPacket);

		String dropService = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropService.java"));
		String seeMethod = dropService.substring(dropService.indexOf("public void see("), dropService.indexOf("private void uniqueDropAnnounce"));
		assertFalse(seeMethod.contains("schedule("));
	}

	private static final class TestNpc extends Npc {
		private SpawnTemplate spawn;

		private TestNpc() {
			super(0, new NpcController(), null, (NpcTemplate) null);
		}

		@Override
		public SpawnTemplate getSpawn() {
			return spawn;
		}

		@Override
		public int getInstanceId() {
			return 1;
		}
	}

	private static final class TestSpawnTemplate extends SpawnTemplate {
		private TestSpawnTemplate() {
			super(null, 0, 0, 0, (byte) 0, 0, null, 0, 0);
		}

		@Override
		public boolean isNoRespawn() {
			return false;
		}

		@Override
		public int getRespawnTime() {
			return 3600;
		}
	}
}
