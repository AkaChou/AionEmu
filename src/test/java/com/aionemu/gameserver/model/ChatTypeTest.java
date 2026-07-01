package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatTypeTest {

	@Test
	void resolvesClientType31() {
		ChatType type = ChatType.getChatTypeByInt(0x1F);

		assertEquals(0x1F, type.toInteger());
		assertTrue(type.isSysMsg());
	}
}
