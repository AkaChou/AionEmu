package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.bg.DeathmatchBg;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;

class ServiceMapImplementationTest {

	@Test
	void shieldServiceStoresRegisteredShieldsInJdkMaps() throws Exception {
		ShieldService service = new ShieldService();

		assertHashMap(service, "sphereShields");
		assertHashMap(service, "registeredShields");
	}

	@Test
	void eventWindowServiceDoesNotStorePlayerPendingEventsInServiceState() {
		assertThrows(NoSuchFieldException.class, () -> findField(EventWindowService.class, "sendActiveEventsForPlayer"));
	}

	@Test
	void eventServicesStorePreviousLocationsInJdkMaps() throws Exception {
		assertHashMap(new DeathmatchBg(), "previousLocations");
		assertHashMap(new FFAService(), "previousLocations");
	}

	@Test
	void timedActivityServicesStoreActiveIndexesInConcurrentMaps() {
		assertAll(
			() -> assertConcurrentMap(InstanceRiftService.class, "activeInstanceRift"),
			() -> assertConcurrentMap(AbyssLandingService.class, "activeLanding"),
			() -> assertConcurrentMap(AbyssLandingSpecialService.class, "activeSpecialLanding"),
			() -> assertConcurrentMap(IuService.class, "activeConcert"),
			() -> assertConcurrentMap(SvsService.class, "activeSvs"),
			() -> assertConcurrentMap(ConquestService.class, "activeConquest"),
			() -> assertConcurrentMap(VortexService.class, "activeInvasions"),
			() -> assertConcurrentMap(BeritraService.class, "activeInvasions"),
			() -> assertConcurrentMap(RvrService.class, "activeRvr"),
			() -> assertConcurrentMap(TowerOfEternityService.class, "activeTowerOfEternity"),
			() -> assertConcurrentMap(DynamicRiftService.class, "activeDynamicRift"),
			() -> assertConcurrentMap(MoltenusService.class, "activeMoltenus"),
			() -> assertConcurrentMap(ZorshivDredgionService.class, "activeZorshivDredgion"),
			() -> assertConcurrentMap(IdianDepthsService.class, "activeIdianDepths"),
			() -> assertConcurrentMap(NightmareCircusService.class, "activeNightmareCircus"),
			() -> assertConcurrentMap(AgentService.class, "activeFights"),
			() -> assertConcurrentMap(AnohaService.class, "activeAnoha"),
			() -> assertConcurrentMap(BalaurAssaultService.class, "fortressAssaults"));
	}

	@Test
	void dropRegistrationIndexesUseConcurrentMapsForLootThreads() {
		assertAll(
			() -> assertConcurrentMap(DropRegistrationService.class, "currentDropMap"),
			() -> assertConcurrentMap(DropRegistrationService.class, "dropRegistrationMap"));
	}

	@Test
	void brokerServiceUsesConcurrentMapsForRuntimeIndexes() {
		assertAll(
			() -> assertConcurrentMap(BrokerService.class, "elyosBrokerItems"),
			() -> assertConcurrentMap(BrokerService.class, "elyosSettledItems"),
			() -> assertConcurrentMap(BrokerService.class, "asmodianBrokerItems"),
			() -> assertConcurrentMap(BrokerService.class, "asmodianSettledItems"),
			() -> assertConcurrentMap(BrokerService.class, "playerBrokerCache"));
	}

	private void assertHashMap(Object target, String fieldName) throws Exception {
		Field field = findField(target.getClass(), fieldName);
		field.setAccessible(true);

		assertEquals(HashMap.class, field.get(target).getClass());
	}

	private void assertConcurrentMap(Class<?> type, String fieldName) throws Exception {
		assertTrue(ConcurrentMap.class.isAssignableFrom(findField(type, fieldName).getType()));
	}

	private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
