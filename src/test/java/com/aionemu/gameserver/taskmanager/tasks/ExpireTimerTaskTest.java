package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;

class ExpireTimerTaskTest {

	@Test
	void removePlayerRemovesMatchingExpirables() {
		ExpireTimerTask task = new ExpireTimerTask();
		TestExpirable expirable = new TestExpirable(-1, true);

		task.addTask(expirable, null);
		task.removePlayer(null);
		task.run();

		assertEquals(0, expirable.expireEndCalls);
	}

	@Test
	void expiredTaskRunsOnceAndIsRemoved() {
		ExpireTimerTask task = new ExpireTimerTask();
		TestExpirable expirable = new TestExpirable(-1, true);

		task.addTask(expirable, null);
		task.run();
		task.run();

		assertEquals(1, expirable.expireEndCalls);
	}

	private static final class TestExpirable implements IExpirable {

		private final int secondsFromNow;
		private final boolean canExpireNow;
		private int expireEndCalls;

		private TestExpirable(int secondsFromNow, boolean canExpireNow) {
			this.secondsFromNow = secondsFromNow;
			this.canExpireNow = canExpireNow;
		}

		@Override
		public int getExpireTime() {
			return (int) (System.currentTimeMillis() / 1000) + secondsFromNow;
		}

		@Override
		public void expireEnd(Player player) {
			expireEndCalls++;
		}

		@Override
		public boolean canExpireNow() {
			return canExpireNow;
		}

		@Override
		public void expireMessage(Player player, int time) {
		}
	}
}
