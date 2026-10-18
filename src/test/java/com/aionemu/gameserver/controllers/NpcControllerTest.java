package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.ThreadPoolManager;

class NpcControllerTest {

	private static ThreadPoolManager threadPoolManager;

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@BeforeAll
	static void installThreadPoolProvider() {
		// scheduleRespawn 经 RespawnService 使用静态线程池门面，静态兜底已退役。
		// scheduleRespawn goes through the static thread-pool facade in RespawnService, whose fallback is retired.
		threadPoolManager = new ThreadPoolManager();
		TestServiceProviders.install(ThreadPoolManager.class, threadPoolManager);
	}

	@AfterAll
	static void clearThreadPoolProvider() {
		TestServiceProviders.clear(ThreadPoolManager.class);
		threadPoolManager.shutdown();
	}

	@Test
	void onBeforeSpawnPreservesConfiguredHeight() throws Exception {
		String controller = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/NpcController.java"));
		String onBeforeSpawn = controller.substring(controller.indexOf("public void onBeforeSpawn()"),
				controller.indexOf("public void onAfterSpawn()"));
		assertFalse(onBeforeSpawn.contains(".setZ("));
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
		int deathPacket = controller.indexOf("new SM_EMOTION(owner, EmotionType.DIE, 0, 0)");
		assertTrue(controller.indexOf("DropService.getInstance().see(player, owner)", deathPacket) > deathPacket);

		String dropService = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropService.java"));
		String seeMethod = dropService.substring(dropService.indexOf("public void see("), dropService.indexOf("private void uniqueDropAnnounce"));
		assertFalse(seeMethod.contains("schedule("));
	}

	private static final class TestNpc extends Npc {
		private SpawnTemplate spawn;

		private TestNpc() {
			super(0, new NpcController(), null, null);
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
