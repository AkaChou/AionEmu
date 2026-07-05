package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShugoSweepActionTest {

	@Test
	void usesConfiguredSweepCount() {
		ShugoSweepAction action = new ShugoSweepAction();
		action.count = 5;

		assertEquals(8, action.addConfiguredCount(3));
	}

	@Test
	void defaultsMissingSweepCountToOne() {
		ShugoSweepAction action = new ShugoSweepAction();

		assertEquals(4, action.addConfiguredCount(3));
	}
}
