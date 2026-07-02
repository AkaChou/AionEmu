package com.aionemu.gameserver.skillengine.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CraftingTaskBatchTest {

	@Test
	void consumesOneAttemptAndKeepsRemainingBatchCrafts() {
		assertEquals(19, CraftingTask.getRemainingCraftsAfterAttempt(20));
		assertEquals(0, CraftingTask.getRemainingCraftsAfterAttempt(1));
	}
}
