package com.aionemu.gameserver.services.drop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DropServiceTest {

	@Test
	void bidIsPaidBeforeItemIsAddedAndNotPaidAgainDuringDistribution() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropService.java"));
		String collection = source.substring(source.indexOf("// handles distribution of item"),
				source.indexOf("private static long distributeEqually"));
		String settlement = source.substring(source.indexOf("private void winningBidActions"),
				source.indexOf("private void winningNormalActions"));
		int payment = collection.indexOf("tryDecreaseKinah");

		assertTrue(payment >= 0 && payment < collection.indexOf("ItemService.addItem"));
		assertFalse(settlement.contains("decreaseKinah"));
	}
}
