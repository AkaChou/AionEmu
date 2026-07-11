package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FireworksUseActionTest {

	@Test
	void consumesAndBroadcastsSynchronouslyWithoutItemUseTask() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/model/templates/item/actions/FireworksUseAction.java"));

		assertTrue(source.contains("decreaseByObjectId(parentItem.getObjectId(), 1)"));
		assertTrue(source.indexOf("decreaseByObjectId") < source.indexOf("broadcastPacket"));
		assertFalse(source.contains("TaskId.ITEM_USE"));
		assertFalse(source.contains("ItemUseObserver"));
		assertFalse(source.contains("schedule("));
	}
}
