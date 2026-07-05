package com.aionemu.gameserver.ai.housing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.controllers.observer.GaleCycloneObserver;
import com.aionemu.gameserver.world.knownlist.KnownList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class GaleCycloneAI2Test {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void clearRemovesAllObserversWithoutConcurrentModification() throws Exception {
		TestGaleCycloneAI2 ai = new TestGaleCycloneAI2();
		Map<Integer, GaleCycloneObserver> observed = new LinkedHashMap<Integer, GaleCycloneObserver>();
		observed.put(1, objenesis.newInstance(TestObserver.class));
		observed.put(2, objenesis.newInstance(TestObserver.class));
		setField(ai, "observed", observed);

		assertDoesNotThrow(() -> invokeClear(ai));
		assertTrue(observed.isEmpty());
	}

	private static void invokeClear(GaleCycloneAI2 ai) throws ReflectiveOperationException {
		Method clear = GaleCycloneAI2.class.getDeclaredMethod("clear");
		clear.setAccessible(true);
		clear.invoke(ai);
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = GaleCycloneAI2.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static class TestGaleCycloneAI2 extends GaleCycloneAI2 {
		private final KnownList knownList = new KnownList(null);

		@Override
		protected KnownList getKnownList() {
			return knownList;
		}
	}

	private static class TestObserver extends GaleCycloneObserver {

		private TestObserver() {
			super(null, null);
		}

		@Override
		public void onMove() {
		}
	}
}
