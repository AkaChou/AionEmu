package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LunaChestActionTest {

	@Test
	void rewardsEveryBundleInTheStack() {
		assertEquals(10, LunaChestAction.getOpenCount(10));
		assertEquals(200, LunaChestAction.getLunaReward(20, 10));
	}

	@Test
	void keepsEmptyOrNegativeStacksAtOneUse() {
		assertEquals(1, LunaChestAction.getOpenCount(0));
		assertEquals(1, LunaChestAction.getOpenCount(-5));
	}
}
