package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DummyEffectTest {

	@Test
	void applyIsNoOp() {
		assertDoesNotThrow(() -> new DummyEffect().applyEffect(null));
	}
}
