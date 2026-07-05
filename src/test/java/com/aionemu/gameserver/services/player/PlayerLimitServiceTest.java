package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;

class PlayerLimitServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private boolean originalLimitsEnabled;
	private int originalLimitsRate;
	private Map<Integer, Long> originalSellLimit;

	@BeforeEach
	void rememberStaticState() throws Exception {
		originalLimitsEnabled = CustomConfig.LIMITS_ENABLED;
		originalLimitsRate = CustomConfig.LIMITS_RATE;
		originalSellLimit = sellLimit();
		CustomConfig.LIMITS_ENABLED = true;
		CustomConfig.LIMITS_RATE = 1;
	}

	@AfterEach
	void restoreStaticState() throws Exception {
		CustomConfig.LIMITS_ENABLED = originalLimitsEnabled;
		CustomConfig.LIMITS_RATE = originalLimitsRate;
		setSellLimit(originalSellLimit);
	}

	@Test
	void sellLimitIndexIsSafeForPlayerAndCronThreads() throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(PlayerLimitService.class.getDeclaredField("sellLimit").getType()));
	}

	@Test
	void updateSellLimitDebitsAccountLimitAtomically() throws Exception {
		int accountId = 100;
		BarrierOnGetMap limits = new BarrierOnGetMap(2);
		limits.put(accountId, 1L);
		setSellLimit(limits);
		Player player = player(accountId);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> first = executor.submit(() -> PlayerLimitService.updateSellLimit(player, 1));
			Future<Boolean> second = executor.submit(() -> PlayerLimitService.updateSellLimit(player, 1));

			int successfulSales = (first.get(2, TimeUnit.SECONDS) ? 1 : 0) + (second.get(2, TimeUnit.SECONDS) ? 1 : 0);

			assertEquals(1, successfulSales);
			assertEquals(0L, limits.get(accountId));
		} finally {
			executor.shutdownNow();
		}
	}

	private Player player(int accountId) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		setField(player, "playerAccount", new Account(accountId));
		return player;
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Long> sellLimit() throws ReflectiveOperationException {
		Field field = PlayerLimitService.class.getDeclaredField("sellLimit");
		field.setAccessible(true);
		return (Map<Integer, Long>) field.get(null);
	}

	private static void setSellLimit(Map<Integer, Long> value) throws ReflectiveOperationException {
		Field field = PlayerLimitService.class.getDeclaredField("sellLimit");
		field.setAccessible(true);
		field.set(null, value);
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class BarrierOnGetMap extends ConcurrentHashMap<Integer, Long> {
		private final CyclicBarrier barrier;

		private BarrierOnGetMap(int parties) {
			this.barrier = new CyclicBarrier(parties);
		}

		@Override
		public Long get(Object key) {
			Long value = super.get(key);
			if (Long.valueOf(1).equals(value)) {
				awaitBarrier();
			}
			return value;
		}

		private void awaitBarrier() {
			try {
				barrier.await(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			} catch (BrokenBarrierException | java.util.concurrent.TimeoutException e) {
				throw new AssertionError(e);
			}
		}
	}
}
