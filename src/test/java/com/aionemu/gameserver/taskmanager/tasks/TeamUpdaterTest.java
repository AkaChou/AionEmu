package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;

class TeamUpdaterTest {

	@Test
	void effectUpdateTriggeredDuringSendRemainsQueued() {
		TeamEffectUpdater updater = new TeamEffectUpdater();
		TestPlayer player = player();
		player.onlineCheck = () -> updater.startTask(player);
		updater.startTask(player);

		updater.run();

		assertTrue(updater.hasTask(player));
	}

	@Test
	void moveUpdateTriggeredDuringSendRemainsQueued() {
		TeamMoveUpdater updater = new TeamMoveUpdater();
		TestPlayer player = player();
		player.groupCheck = () -> updater.startTask(player);
		updater.startTask(player);

		updater.run();

		assertTrue(updater.hasTask(player));
	}

	private static TestPlayer player() {
		return new ObjenesisStd().newInstance(TestPlayer.class);
	}

	private static final class TestPlayer extends Player {

		private Runnable onlineCheck;
		private Runnable groupCheck;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public boolean isOnline() {
			onlineCheck.run();
			return false;
		}

		@Override
		public boolean isInGroup2() {
			groupCheck.run();
			return false;
		}

		@Override
		public boolean isInAlliance2() {
			return false;
		}
	}
}
