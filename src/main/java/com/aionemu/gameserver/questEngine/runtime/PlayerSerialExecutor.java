package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes all quest event work for one player while allowing other players to proceed.
 *
 * <p>Locks are reference-counted: a lock stays in the map only while at least one
 * execution is using it and is removed once the last execution finishes, so a
 * long-lived server never keeps one lock per historical player id. {@link #execute}
 * increments the count atomically with lock (re)use via {@code ConcurrentHashMap.compute},
 * and the removal re-checks the count inside the map's per-bin lock, so a freshly
 * re-acquired lock is never evicted under a concurrent writer.</p>
 */
public final class PlayerSerialExecutor {
	private static final class CountingLock extends ReentrantLock {
		final AtomicInteger users = new AtomicInteger();
	}

	private final ConcurrentHashMap<Integer, CountingLock> locks = new ConcurrentHashMap<>();

	public <T> T execute(int playerId, Callable<T> operation) throws Exception {
		if (playerId <= 0) {
			throw new IllegalArgumentException("playerId must be positive");
		}
		Objects.requireNonNull(operation, "operation");
		CountingLock lock = locks.compute(playerId, (id, existing) -> {
			CountingLock candidate = existing != null ? existing : new CountingLock();
			candidate.users.incrementAndGet();
			return candidate;
		});
		lock.lock();
		try {
			return operation.call();
		} finally {
			lock.unlock();
			if (lock.users.decrementAndGet() == 0) {
				// 移除与并发接入互斥:仅在仍为 0(无新用户复用此锁)时才驱逐。
				locks.computeIfPresent(playerId, (id, present) -> present == lock && lock.users.get() == 0 ? null : present);
			}
		}
	}

	public int trackedPlayers() {
		return locks.size();
	}
}
