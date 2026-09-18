package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;

/**
 * 固化 {@link PlayerStorageRegistry} 的类型路由语义。
 * Pins down {@link PlayerStorageRegistry} type-routing semantics.
 */
class PlayerStorageRegistryTest {

	@Test
	void routesStoragesByType() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerStorage cube = new PlayerStorage(StorageType.CUBE);
		PlayerStorage regular = new PlayerStorage(StorageType.REGULAR_WAREHOUSE);
		PlayerStorage account = new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE);

		PlayerStorageRegistry.setStorage(player, cube, StorageType.CUBE);
		PlayerStorageRegistry.setStorage(player, regular, StorageType.REGULAR_WAREHOUSE);
		PlayerStorageRegistry.setStorage(player, account, StorageType.ACCOUNT_WAREHOUSE);

		assertSame(cube, PlayerStorageRegistry.getStorage(player, StorageType.CUBE.getId()));
		assertSame(regular, PlayerStorageRegistry.getStorage(player, StorageType.REGULAR_WAREHOUSE.getId()));
		assertSame(account, PlayerStorageRegistry.getStorage(player, StorageType.ACCOUNT_WAREHOUSE.getId()));
		assertNull(PlayerStorageRegistry.getStorage(player, Integer.MAX_VALUE));
	}
}
