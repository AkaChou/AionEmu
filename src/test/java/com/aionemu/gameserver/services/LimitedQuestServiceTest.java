package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.dao.LimitedQuestDAO;
import com.aionemu.gameserver.services.LimitedQuestService.Limit;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LimitedQuestServiceTest {

	@Test
	void concurrentAcquisitionNeverExceedsGlobalMaximum() throws Exception {
		TestLimitedQuestDAO dao = new TestLimitedQuestDAO();
		LimitedQuestService service = new LimitedQuestService(dao, Map.of(9645, new Limit(10, 5)));
		ExecutorService executor = Executors.newFixedThreadPool(16);
		try {
			List<Future<Boolean>> results = new ArrayList<>();
			for (int i = 0; i < 100; i++) {
				results.add(executor.submit(() -> service.acquire(9645, 1)));
			}
			int acquired = 0;
			for (Future<Boolean> result : results) {
				if (result.get()) {
					acquired++;
				}
			}

			assertEquals(10, acquired);
			assertEquals(0, dao.remaining(9645));
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void recoveryUsesConfiguredAmountAndCapsAtMaximum() {
		TestLimitedQuestDAO dao = new TestLimitedQuestDAO();
		LimitedQuestService service = new LimitedQuestService(dao, Map.of(9645, new Limit(10, 5)));
		for (int i = 0; i < 8; i++) {
			assertTrue(service.acquire(9645, 10));
		}

		assertTrue(service.chargeConfigured(9645, false));
		assertEquals(7, dao.remaining(9645));
		assertTrue(service.chargeConfigured(9645, false));
		assertEquals(10, dao.remaining(9645));
		assertTrue(service.chargeConfigured(9645, true));
		assertEquals(10, dao.remaining(9645));
	}

	@Test
	void loadsAllGeneratedRetailLimits() throws URISyntaxException {
		File file = new File(getClass().getClassLoader()
			.getResource("aion/definitions/compact/quests/limited-quests.xml").toURI());

		Map<Integer, Limit> limits = LimitedQuestService.loadLimits(file);

		assertEquals(5, limits.size());
		assertEquals(new Limit(10, 5), limits.get(9645));
		assertEquals(new Limit(240, 2), limits.get(9661));
		assertEquals(new Limit(240, 2), limits.get(9663));
		assertEquals(new Limit(240, 2), limits.get(13816));
		assertEquals(new Limit(240, 2), limits.get(23816));
	}

	private static final class TestLimitedQuestDAO extends LimitedQuestDAO {
		private final Map<Integer, AtomicInteger> remaining = new ConcurrentHashMap<>();

		@Override
		public boolean tryAcquire(int questId, int maxCount) {
			AtomicInteger counter = remaining.computeIfAbsent(questId, ignored -> new AtomicInteger(maxCount));
			while (true) {
				int current = counter.get();
				if (current == 0) {
					return false;
				}
				if (counter.compareAndSet(current, current - 1)) {
					return true;
				}
			}
		}

		@Override
		public boolean recover(int questId, int amount, int maxCount) {
			remaining.computeIfAbsent(questId, ignored -> new AtomicInteger(maxCount))
				.updateAndGet(current -> Math.min(current + amount, maxCount));
			return true;
		}

		int remaining(int questId) {
			return remaining.get(questId).get();
		}

		@Override
		public boolean supports(String database, int majorVersion, int minorVersion) {
			return true;
		}
	}
}
