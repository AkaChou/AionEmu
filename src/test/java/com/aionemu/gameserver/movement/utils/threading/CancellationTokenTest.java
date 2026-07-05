package com.aionemu.gameserver.movement.utils.threading;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class CancellationTokenTest {

	@Test
	void addActionReturnsBeforeCancelAndActionRunsWhenCancelled() throws Exception {
		CancellationToken token = new CancellationToken();
		AtomicBoolean actionRan = new AtomicBoolean();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<Void> addAction = executor.submit(() -> {
				token.addAction(() -> actionRan.set(true));
				return null;
			});

			waitFor(addAction);
			assertFalse(actionRan.get());

			token.cancel();

			assertTrue(actionRan.get());
		} finally {
			executor.shutdownNow();
		}
	}

	private static void waitFor(Future<Void> future) throws InterruptedException, ExecutionException, TimeoutException {
		future.get(200, TimeUnit.MILLISECONDS);
	}
}
