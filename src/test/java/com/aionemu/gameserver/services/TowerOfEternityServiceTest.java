package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import com.aionemu.gameserver.services.towerofeternityservice.TowerOfEternity;
import org.junit.jupiter.api.Test;

/**
 * 永恒之塔开关归属测试：持续时间计时器只能关闭自己注册的实例。
 * Tower of Eternity ownership tests: a duration timer may only close the instance it registered.
 */
class TowerOfEternityServiceTest {

	private static final int LOCATION_ID = 1;

	@Test
	void staleDurationTimerKeepsNewerInstanceRegistered() throws Exception {
		TowerOfEternityService service = new TowerOfEternityService();
		TowerOfEternity<TowerOfEternityLocation> newer = tower();
		TowerOfEternity<TowerOfEternityLocation> stale = tower();
		activeTowers(service).put(LOCATION_ID, newer);

		assertFalse(service.stopTowerOfEternity(LOCATION_ID, stale));
		assertTrue(service.isActive(LOCATION_ID));
		assertSame(newer, service.getActiveTower(LOCATION_ID));
		assertFalse(newer.isClosed());
	}

	@Test
	void owningDurationTimerStopsRegisteredInstance() throws Exception {
		TowerOfEternityService service = new TowerOfEternityService();
		TowerOfEternity<TowerOfEternityLocation> owner = tower();
		activeTowers(service).put(LOCATION_ID, owner);

		assertTrue(service.stopTowerOfEternity(LOCATION_ID, owner));
		assertFalse(service.isActive(LOCATION_ID));
		assertTrue(owner.isClosed());
	}

	private static TowerOfEternity<TowerOfEternityLocation> tower() {
		return new TowerOfEternity<>(null) {
            @Override
            protected void startTowerOfEternity() {
            }

            @Override
            protected void stopTowerOfEternity() {
            }
        };
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentMap<Integer, TowerOfEternity<?>> activeTowers(TowerOfEternityService service) throws Exception {
		Field field = TowerOfEternityService.class.getDeclaredField("activeTowerOfEternity");
		field.setAccessible(true);
		return (ConcurrentMap<Integer, TowerOfEternity<?>>) field.get(service);
	}
}
