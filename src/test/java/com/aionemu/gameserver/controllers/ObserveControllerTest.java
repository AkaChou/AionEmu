package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
