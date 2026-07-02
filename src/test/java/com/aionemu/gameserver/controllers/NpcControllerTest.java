package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class NpcControllerTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

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
