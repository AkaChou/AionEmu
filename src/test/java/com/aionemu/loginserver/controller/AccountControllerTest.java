package com.aionemu.loginserver.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AccountControllerTest {

	@Test
	void characterCountsAreReturnedAsReadOnlySnapshot() {
		AccountController controller = new AccountController(null, null, null, null, null, null, null);
		int accountId = 92837461;
		controller.addGSCharacterCountFor(accountId, 1, 2);

		Map<Integer, Integer> counts = controller.getGSCharacterCountsFor(accountId);

		assertEquals(Map.of(1, 2), counts);
		assertThrows(UnsupportedOperationException.class, () -> counts.put(2, 4));
		assertEquals(Map.of(1, 2), controller.getGSCharacterCountsFor(accountId));
	}
}
