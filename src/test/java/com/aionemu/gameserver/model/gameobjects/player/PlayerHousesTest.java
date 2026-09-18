package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

/**
 * 固化 {@link PlayerHouses} 的状态位与空缓存语义。
 * Pins down {@link PlayerHouses} state-bit and empty-cache semantics.
 */
class PlayerHousesTest {

	@Test
	void stateBitsSetAndUnset() {
		Player player = emptyPlayerWithCachedHouses();

		PlayerHouses.setBuildingOwnerState(player, PlayerHouseOwnerFlags.IS_OWNER.getId());
		assertTrue(player.isBuildingInState(PlayerHouseOwnerFlags.IS_OWNER));

		PlayerHouses.unsetBuildingOwnerState(player, PlayerHouseOwnerFlags.IS_OWNER.getId());
		assertFalse(player.isBuildingInState(PlayerHouseOwnerFlags.IS_OWNER));
	}

	@Test
	void emptyHouseCacheYieldsNoActiveHouse() {
		Player player = emptyPlayerWithCachedHouses();

		assertNull(player.getActiveHouse());
		assertEquals(0, player.getHouseOwnerId());
	}

	@Test
	void resetHousesClearsAndNullsCache() throws Exception {
		Player player = emptyPlayerWithCachedHouses();

		player.resetHouses();

		assertNull(player.getHousesOrNull());
	}

	private static Player emptyPlayerWithCachedHouses() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		try {
			Field field = Player.class.getDeclaredField("houses");
			field.setAccessible(true);
			field.set(player, new ArrayList<>());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		return player;
	}
}
