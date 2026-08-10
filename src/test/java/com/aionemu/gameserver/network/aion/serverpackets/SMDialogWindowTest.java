package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.DialogPage;

class SMDialogWindowTest {

	@Test
	void nullPageAlwaysProducesTargetlessClosePacket() throws Exception {
		assertFields(new SM_DIALOG_WINDOW(204160, DialogPage.NULL.id()), 0, 0, 0);
		assertFields(new SM_DIALOG_WINDOW(204160, DialogPage.NULL.id(), 1001), 0, 0, 0);
	}

	@Test
	void nonNullPagePreservesTargetAndQuestContext() throws Exception {
		assertFields(new SM_DIALOG_WINDOW(204160, 1011), 204160, 1011, 0);
		assertFields(new SM_DIALOG_WINDOW(204160, 1011, 1001), 204160, 1011, 1001);
	}

	private static void assertFields(SM_DIALOG_WINDOW packet, int targetObjectId, int dialogId, int questId)
			throws Exception {
		assertEquals(targetObjectId, intField(packet, "targetObjectId"));
		assertEquals(dialogId, intField(packet, "dialogID"));
		assertEquals(questId, intField(packet, "questId"));
	}

	private static int intField(SM_DIALOG_WINDOW packet, String name) throws Exception {
		Field field = SM_DIALOG_WINDOW.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(packet);
	}
}
