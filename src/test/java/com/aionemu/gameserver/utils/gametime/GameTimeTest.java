package com.aionemu.gameserver.utils.gametime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class GameTimeTest {

	@Test
	void equalityUsesTotalMinutes() {
		GameTime first = new GameTime(1234);
		GameTime same = new GameTime(1234);

		assertEquals(first, same);
		assertEquals(first.hashCode(), same.hashCode());
		assertEquals(1, new HashSet<>(List.of(first, same)).size());
		assertNotEquals(first, new GameTime(1235));
		assertNotEquals(first, null);
		assertNotEquals(first, 1234);
	}
}
