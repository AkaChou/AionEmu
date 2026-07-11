package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerAppearanceTest {

	@Test
	void convertsClientScaleToWorldHeight() {
		PlayerAppearance appearance = new PlayerAppearance();
		appearance.setHeight(1.0f);

		assertEquals(1.75f, appearance.getBoundHeight());
	}
}
