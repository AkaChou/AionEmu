package com.aionemu.gameserver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChatUtilTest {

	@Test
	void selectsAbyssMapPart() {
		assertEquals(-1, ChatUtil.getMapPart(210010000, 0, 0, 0));
		assertEquals(1, ChatUtil.getMapPart(400010000, 1000, 1000, 2000));
		assertEquals(2, ChatUtil.getMapPart(400010000, 2000, 2000, 2000));
		assertEquals(3, ChatUtil.getMapPart(400010000, 1000, 1000, 2300));
	}
}
