package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.dao.TownDAO;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.town.Town;

/**
 * 核心服务构造器注入与 DAO 解耦单元测试。
 * Unit tests verifying constructor injection and DAO decoupling for core gameplay services.
 */
class ServiceConstructorInjectionTest {

	@Test
	void townServiceConstructsWithInjectedDaoAndResolvesTowns() {
		Town elyosTown = createTestTown(101, Race.ELYOS);
		Town asmoTown = createTestTown(201, Race.ASMODIANS);

		TownDAO stubTownDAO = new TownDAO() {
			@Override
			public Map<Integer, Town> load(Race race) {
				Map<Integer, Town> map = new HashMap<>();
				if (race == Race.ELYOS) {
					map.put(elyosTown.getId(), elyosTown);
				} else if (race == Race.ASMODIANS) {
					map.put(asmoTown.getId(), asmoTown);
				}
				return map;
			}

			@Override
			public void store(Town town) {
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		TownService townService = new TownService(stubTownDAO);

		assertEquals(elyosTown, townService.getTownById(101));
		assertEquals(asmoTown, townService.getTownById(201));
		assertNull(townService.getTownById(999));
	}

	private static Town createTestTown(int id, Race race) {
		Town town = new org.objenesis.ObjenesisStd().newInstance(Town.class);
		try {
			java.lang.reflect.Field idField = Town.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(town, id);

			java.lang.reflect.Field raceField = Town.class.getDeclaredField("race");
			raceField.setAccessible(true);
			raceField.set(town, race);
			return town;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void announcementServiceConstructsWithInjectedDao() {
		AnnouncementsDAO stubAnnouncementsDAO = new AnnouncementsDAO() {
			@Override
			public Set<Announcement> getAnnouncements() {
				return Collections.emptySet();
			}

			@Override
			public void addAnnouncement(Announcement announce) {
			}

			@Override
			public boolean delAnnouncement(int idAnnounce) {
				return true;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		AnnouncementService announcementService = new AnnouncementService(stubAnnouncementsDAO);
		assertNotNull(announcementService.getAnnouncements());
		assertEquals(0, announcementService.getAnnouncements().size());
	}

	@Test
	void abyssLandingServiceConstructsWithInjectedDao() {
		AbyssLandingDAO stubAbyssLandingDAO = new AbyssLandingDAO() {
			@Override
			public boolean loadLandingLocations(Map<Integer, LandingLocation> locations) {
				return true;
			}

			@Override
			public void store(LandingLocation location) {
			}

			@Override
			public boolean updateLandingLocation(LandingLocation location) {
				return true;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		AbyssLandingService service = new AbyssLandingService(stubAbyssLandingDAO);
		assertNotNull(service);
	}

	@Test
	void exchangeServiceConstructsWithInjectedDao() {
		InventoryDAO concreteStubDao = new InventoryDAO() {
			@Override
			public Storage loadStorage(int playerId, StorageType storageType) {
				return null;
			}

			@Override
			public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
				return Collections.emptyList();
			}

			@Override
			public Equipment loadEquipment(Player player) {
				return null;
			}

			@Override
			public List<Item> loadEquipment(int playerId) {
				return Collections.emptyList();
			}

			@Override
			public boolean store(Player player) {
				return true;
			}

			@Override
			public boolean store(Item item, Player player) {
				return true;
			}

			@Override
			public boolean store(List<Item> items, int playerId) {
				return true;
			}

			@Override
			public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
				return true;
			}

			@Override
			public boolean deletePlayerItems(int playerId) {
				return true;
			}

			@Override
			public void deleteAccountWH(int accountId) {
			}

			@Override
			public int[] getUsedIDs() {
				return new int[0];
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		ExchangeService service = new ExchangeService(concreteStubDao);
		assertNotNull(service);
	}

	@Test
	void petitionServiceConstructsWithInjectedDaoAndLoadsPetitions() {
		Petition stubPetition = new Petition(501);
		PetitionDAO stubPetitionDAO = new PetitionDAO() {
			@Override
			public Set<Petition> getPetitions() {
				return Collections.singleton(stubPetition);
			}

			@Override
			public Petition getPetitionById(int petitionId) {
				return stubPetition;
			}

			@Override
			public void deletePetition(int playerObjId) {
			}

			@Override
			public void insertPetition(Petition ptt) {
			}

			@Override
			public void setReplied(int petitionId) {
			}

			@Override
			public int getNextAvailableId() {
				return 502;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		PetitionService service = new PetitionService(stubPetitionDAO);
		assertEquals(1, service.getRegisteredPetitions().size());
	}

	@Test
	void servicesFailFastOnNullDaoInjection() {
		assertThrows(NullPointerException.class, () -> new TownService(null));
		assertThrows(NullPointerException.class, () -> new AnnouncementService(null));
		assertThrows(NullPointerException.class, () -> new AbyssLandingService(null));
		assertThrows(NullPointerException.class, () -> new ExchangeService(null));
		assertThrows(NullPointerException.class, () -> new PetitionService(null));
	}
}
