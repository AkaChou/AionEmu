package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class ScheduledFutureWrapperTest {

	@Test
	void cancelDelegatesMayInterruptIfRunning() {
		RecordingScheduledFuture future = new RecordingScheduledFuture();
		ScheduledFutureWrapper wrapper = new ScheduledFutureWrapper(future);

		assertTrue(wrapper.cancel(true));

		assertTrue(future.cancelled);
		assertTrue(future.mayInterruptIfRunning);
	}

	@Test
	void cancelCanDelegateWithoutInterrupting() {
		RecordingScheduledFuture future = new RecordingScheduledFuture();
		ScheduledFutureWrapper wrapper = new ScheduledFutureWrapper(future);

		assertTrue(wrapper.cancel(false));

		assertTrue(future.cancelled);
		assertFalse(future.mayInterruptIfRunning);
	}

	private static final class RecordingScheduledFuture implements ScheduledFuture<Object> {

		private boolean cancelled;
		private boolean mayInterruptIfRunning;

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			this.cancelled = true;
			this.mayInterruptIfRunning = mayInterruptIfRunning;
			return true;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed other) {
			return 0;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return cancelled;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}
}
