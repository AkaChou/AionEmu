package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;

class TeleportServiceCompletionTest {

	@Test
	void deadPlayerDoesNotInitiateOrCompleteBeamTeleport() {
		TestPlayer player = player(true, true);
		AtomicInteger completions = new AtomicInteger();

		boolean initiated = TeleportService2.teleportTo(player, 210010000, 1, 1, 2, 3, (byte) 4, completions::incrementAndGet);

		assertFalse(initiated);
		assertEquals(0, completions.get());
	}

	@Test
	void scheduledTeleportDoesNotCompleteForUnspawnedPlayer() {
		TestPlayer player = player(false, false);
		AtomicInteger positionChanges = new AtomicInteger();
		AtomicInteger completions = new AtomicInteger();

		TeleportService2.runScheduledTeleport(player, positionChanges::incrementAndGet, completions::incrementAndGet);

		assertEquals(0, positionChanges.get());
		assertEquals(0, completions.get());
	}

	@Test
	void scheduledTeleportDoesNotCompleteWhenPlayerDiedBeforeExecution() {
		TestPlayer player = player(true, true);
		AtomicInteger positionChanges = new AtomicInteger();
		AtomicInteger completions = new AtomicInteger();

		TeleportService2.runScheduledTeleport(player, positionChanges::incrementAndGet, completions::incrementAndGet);

		assertEquals(0, positionChanges.get());
		assertEquals(0, completions.get());
	}

	@Test
	void scheduledTeleportCompletesOnlyAfterPositionChangeReturns() {
		TestPlayer player = player(false, true);
		List<String> events = new ArrayList<>();

		TeleportService2.runScheduledTeleport(player, () -> events.add("position"), () -> events.add("completed"));

		assertEquals(List.of("position", "completed"), events);
	}

	@Test
	void staleAuthorizationPreventsScheduledPositionChangeAndCompletion() {
		TestPlayer player = player(false, true);
		AtomicInteger positionChanges = new AtomicInteger();
		AtomicInteger completions = new AtomicInteger();

		TeleportService2.runScheduledTeleport(player, () -> false, positionChanges::incrementAndGet, completions::incrementAndGet);

		assertEquals(0, positionChanges.get());
		assertEquals(0, completions.get());
	}

	@Test
	void authorizationFailureIsFailClosedBeforePhysicalSideEffects() {
		AtomicInteger positionChanges = new AtomicInteger();
		AtomicInteger completions = new AtomicInteger();

		assertThrows(IllegalStateException.class, () -> TeleportService2.runAuthorizedTeleport(() -> {
			throw new IllegalStateException("authorization unavailable");
		}, positionChanges::incrementAndGet, completions::incrementAndGet));
		assertEquals(0, positionChanges.get());
		assertEquals(0, completions.get());
	}

	@Test
	void failedPositionChangeDoesNotCompleteTeleport() {
		TestPlayer player = player(false, true);
		AtomicInteger completions = new AtomicInteger();

		assertThrows(IllegalStateException.class,
				() -> TeleportService2.runScheduledTeleport(player, () -> {
					throw new IllegalStateException("position change failed");
				}, completions::incrementAndGet));
		assertEquals(0, completions.get());
	}

	private static TestPlayer player(boolean dead, boolean spawned) {
		ObjenesisStd objenesis = new ObjenesisStd();
		TestPlayer player = objenesis.newInstance(TestPlayer.class);
		TestPlayerLifeStats lifeStats = objenesis.newInstance(TestPlayerLifeStats.class);
		lifeStats.dead = dead;
		player.lifeStats = lifeStats;
		player.spawned = spawned;
		return player;
	}

	private static final class TestPlayer extends Player {
		private PlayerLifeStats lifeStats;
		private boolean spawned;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public PlayerLifeStats getLifeStats() {
			return lifeStats;
		}

		@Override
		public boolean isSpawned() {
			return spawned;
		}
	}

	private static final class TestPlayerLifeStats extends PlayerLifeStats {
		private boolean dead;

		private TestPlayerLifeStats() {
			super(null);
		}

		@Override
		public boolean isAlreadyDead() {
			return dead;
		}
	}
}
