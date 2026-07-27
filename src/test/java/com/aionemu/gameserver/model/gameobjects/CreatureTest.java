package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CreatureTest {

	@Test
	void usesRetailPlayerStatRatioCurve() {
		assertEquals(1000, Creature.getPlayerStatRatio(65));
		assertEquals(1015, Creature.getPlayerStatRatio(66));
		assertEquals(1150, Creature.getPlayerStatRatio(75));
	}
}
