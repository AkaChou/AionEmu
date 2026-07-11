package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import org.junit.jupiter.api.Test;

class ShoutEventHandlerTest {

	@Test
	void distinguishesSupportCallerFromResponder() {
		assertEquals(ShoutEventType.HELPCALL, ShoutEventHandler.supportEventType(false));
		assertEquals(ShoutEventType.HELP, ShoutEventHandler.supportEventType(true));
	}
}
