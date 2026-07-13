package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import org.junit.jupiter.api.Test;

class ObserveControllerAttackContextTest {

	@Test
	void forwardsSkillIdAndMagicalAttackType() {
		ObserveController controller = new ObserveController();
		AtomicInteger skillId = new AtomicInteger();
		AtomicBoolean magical = new AtomicBoolean();
		controller.addObserver(new ActionObserver(ObserverType.ATTACK) {
			@Override
			public void attack(Creature target, int currentSkillId) {
				skillId.set(currentSkillId);
			}
		});
		controller.addObserver(new ActionObserver(ObserverType.ATTACKED) {
			@Override
			public void attacked(Creature attacker, boolean magicalAttack) {
				magical.set(magicalAttack);
			}
		});

		controller.notifyAttackObservers(null, 42);
		controller.notifyAttackedObservers(null, true);

		assertEquals(42, skillId.get());
		assertTrue(magical.get());
	}
}
