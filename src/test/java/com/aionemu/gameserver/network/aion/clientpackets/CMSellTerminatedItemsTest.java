package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CMSellTerminatedItemsTest {

	@Test
	void defersInventoryChangesUntilPacketExecution() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_SELL_TERMINATED_ITEMS.java"));
		String read = source.substring(source.indexOf("protected void readImpl()"), source.indexOf("protected void runImpl()"));
		String run = source.substring(source.indexOf("protected void runImpl()"));

		assertFalse(read.contains("terminatedItemToShop"));
		assertTrue(run.contains("terminatedItemToShop"));
	}
}
