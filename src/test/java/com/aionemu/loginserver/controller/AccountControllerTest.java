package com.aionemu.loginserver.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AccountControllerTest {

	@Test
	void characterCountsAreReturnedAsReadOnlySnapshot() {
		int accountId = 92837461;
		AccountController.addGSCharacterCountFor(accountId, 1, 2);

		Map<Integer, Integer> counts = AccountController.getGSCharacterCountsFor(accountId);

		assertEquals(Map.of(1, 2), counts);
		assertThrows(UnsupportedOperationException.class, () -> counts.put(2, 4));
		assertEquals(Map.of(1, 2), AccountController.getGSCharacterCountsFor(accountId));
	}
}
