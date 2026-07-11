package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.skillengine.model.HealType;

class ObserveControllerLifeChangedTest {

	@Test
	void dispatchesLifeChangesAndCleansUpOnce() {
		ObserveController controller = new ObserveController();
		AtomicReference<HealType> type = new AtomicReference<>();
		AtomicInteger value = new AtomicInteger();
		AtomicBoolean removed = new AtomicBoolean();
		ActionObserver observer = new ActionObserver(ObserverType.LIFE_CHANGED) {
			@Override
			public void lifeChanged(HealType changedType, int currentValue) {
				type.set(changedType);
				value.set(currentValue);
			}

			@Override
			public void onRemoved() {
				assertTrue(removed.compareAndSet(false, true));
			}
		};

		controller.addObserver(observer);
		controller.notifyLifeChangedObservers(HealType.HP, 42);

		assertEquals(HealType.HP, type.get());
		assertEquals(42, value.get());
		assertTrue(ObserverType.ALL.matchesObserver(ObserverType.LIFE_CHANGED));

		controller.removeObserver(observer);
		controller.removeObserver(observer);
		controller.notifyLifeChangedObservers(HealType.MP, 7);

		assertTrue(removed.get());
		assertEquals(HealType.HP, type.get());
		assertEquals(42, value.get());
	}
}
