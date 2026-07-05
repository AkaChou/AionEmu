package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmotionTypeTest {

	@Test
	void mapsGlidingEmotionIds() {
		assertEquals(EmotionType.GLIDING, EmotionType.getEmotionTypeById(0x2E));
		assertEquals(EmotionType.GLIDING_END, EmotionType.getEmotionTypeById(0x2F));
	}
}
