package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class GameBattlefieldLifecycleTest {

	@Test
	void usesBattlefieldGatewayCollaborator() {
		assertEquals(GameBattlefieldGateway.class, fieldType("battlefieldGateway"));
	}

	@Test
	void startRunsUnifiedGatewayOnceAndRecordsLoadTime() {
		RecordingGameBattlefieldGateway gateway = new RecordingGameBattlefieldGateway(null);
		GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(gateway);

		lifecycle.start();
		lifecycle.start();

		assertTrue(lifecycle.isLoaded());
		assertEquals(1, gateway.calls);
		assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
	}

	@Test
	void failedStartRecordsFailureAndAllowsRetry() {
		IllegalStateException failure = new IllegalStateException("battlefield failed");
		RecordingGameBattlefieldGateway gateway = new RecordingGameBattlefieldGateway(failure);
		GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(gateway);

		assertSame(failure, assertThrows(IllegalStateException.class, lifecycle::start));
		assertSame(failure, lifecycle.getLastFailure());
		assertFalse(lifecycle.isLoaded());

		lifecycle.start();

		assertTrue(lifecycle.isLoaded());
		assertEquals(2, gateway.calls);
		assertNull(lifecycle.getLastFailure());
	}

	private static Class<?> fieldType(String name) {
		try {
			Field field = GameBattlefieldLifecycle.class.getDeclaredField(name);
			return field.getType();
		} catch (NoSuchFieldException e) {
			throw new AssertionError("Missing field: " + name, e);
		}
	}

	private static final class RecordingGameBattlefieldGateway extends GameBattlefieldGateway {
		private final RuntimeException firstFailure;
		private int calls;

		private RecordingGameBattlefieldGateway(RuntimeException firstFailure) {
			this.firstFailure = firstFailure;
		}

		@Override
		public void start() {
			calls++;
			if (calls == 1 && firstFailure != null) {
				throw firstFailure;
			}
		}
	}
}
