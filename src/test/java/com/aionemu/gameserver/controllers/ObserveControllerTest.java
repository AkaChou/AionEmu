package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;

class ObserveControllerTest {

	@Test
	void notifyObserversAllowsObserversToBeAddedDuringCallback() {
		ObserveController controller = new ObserveController();
		AtomicInteger calls = new AtomicInteger();
		ActionObserver addedObserver = new ActionObserver(ObserverType.MOVE) {
			@Override
			public void moved() {
				calls.addAndGet(100);
			}
		};
		ActionObserver mutatingObserver = new ActionObserver(ObserverType.MOVE) {
			@Override
			public void moved() {
				calls.incrementAndGet();
				controller.addObserver(addedObserver);
			}
		};
		ActionObserver trailingObserver = new ActionObserver(ObserverType.MOVE) {
			@Override
			public void moved() {
				calls.incrementAndGet();
			}
		};
		controller.addObserver(mutatingObserver);
		controller.addObserver(trailingObserver);

		assertDoesNotThrow(controller::notifyMoveObservers);
		assertEquals(2, calls.get());
	}

	@Test
	void observerCollectionsAreMaterialisedOnlyOnFirstWrite() throws Exception {
		ObserveController controller = new ObserveController();

		// 未使用观察者的控制器共用空占位符：不再为每个生物预制两个 CopyOnWriteArrayList。
		Object placeholder = field(controller, "observers");
		assertTrue(((Collection<?>) placeholder).isEmpty());
		assertSame(placeholder, field(controller, "attackCalcObservers"));

		controller.addObserver(moveObserver());

		assertNotSame(placeholder, field(controller, "observers"));
		assertSame(placeholder, field(controller, "attackCalcObservers"));
		assertEquals(1, ((Collection<?>) field(controller, "observers")).size());
	}

	@Test
	void concurrentFirstWritesConvergeOnOneCollection() throws Exception {
		ObserveController controller = new ObserveController();
		int threads = 8;
		int perThread = 64;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try {
			for (int thread = 0; thread < threads; thread++) {
				futures.add(pool.submit(() -> {
					try {
						start.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new AssertionError(e);
					}
					for (int i = 0; i < perThread; i++) {
						controller.addObserver(moveObserver());
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		} finally {
			pool.shutdownNow();
		}

		// 并发首次写入必须收敛到同一集合：否则会有一批观察者落进被丢弃的列表而永久丢失。
		assertEquals(threads * perThread, ((Collection<?>) field(controller, "observers")).size());
	}

	@Test
	void removeAndClearOnAnUntouchedControllerAreNoOps() {
		ObserveController controller = new ObserveController();

		// 空占位符是不可变集合：remove/clear 必须被守卫而不是落在它上面（否则抛 UnsupportedOperationException）。
		assertDoesNotThrow(() -> controller.removeObserver(moveObserver()));
		assertDoesNotThrow(() -> controller.removeAttackCalcObserver(new AttackCalcObserver() {
			@Override
			public float getBasePhysicalDamageMultiplier(boolean isSkill) {
				return 1f;
			}
		}));
		assertDoesNotThrow(controller::clear);
		assertDoesNotThrow(controller::notifyMoveObservers);
		assertDoesNotThrow(() -> controller.getBasePhysicalDamageMultiplier(true));
	}

	private static ActionObserver moveObserver() {
		return new ActionObserver(ObserverType.MOVE) {
		};
	}

	private static Object field(Object target, String name) throws Exception {
		Field field = ObserveController.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	@Test
	void attackCalcObserversAllowObserversToBeAddedDuringCallback() {
		ObserveController controller = new ObserveController();
		AttackCalcObserver addedObserver = new AttackCalcObserver() {
			@Override
			public float getBasePhysicalDamageMultiplier(boolean isSkill) {
				return 100f;
			}
		};
		AttackCalcObserver mutatingObserver = new AttackCalcObserver() {
			@Override
			public float getBasePhysicalDamageMultiplier(boolean isSkill) {
				controller.addAttackCalcObserver(addedObserver);
				return 2f;
			}
		};
		AttackCalcObserver trailingObserver = new AttackCalcObserver() {
			@Override
			public float getBasePhysicalDamageMultiplier(boolean isSkill) {
				return 3f;
			}
		};
		controller.addAttackCalcObserver(mutatingObserver);
		controller.addAttackCalcObserver(trailingObserver);

		assertDoesNotThrow(() -> assertEquals(6f, controller.getBasePhysicalDamageMultiplier(true)));
	}
}
