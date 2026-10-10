package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.dao.BaseDAO;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.BrokerDAO;
import com.aionemu.gameserver.dao.ChallengeTasksDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.HouseBidsDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.LimitedQuestDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerPassportsDAO;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.dao.SurveyControllerDAO;
import com.aionemu.gameserver.dao.TownDAO;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.player.PlayerServiceDaos;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;

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
	void abyssLandingSpecialServiceConstructsWithInjectedDao() {
		AbyssSpecialLandingDAO stubAbyssSpecialLandingDAO = new AbyssSpecialLandingDAO() {
			@Override
			public boolean loadLandingSpecialLocations(Map<Integer, LandingSpecialLocation> locations) {
				return true;
			}

			@Override
			public void store(LandingSpecialLocation location) {
			}

			@Override
			public boolean updateLandingSpecialLocation(LandingSpecialLocation location) {
				return true;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		AbyssLandingSpecialService service = new AbyssLandingSpecialService(stubAbyssSpecialLandingDAO);
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
	void brokerServiceConstructsWithInjectedDaos() {
		BrokerDAO stubBrokerDAO = new BrokerDAO() {
			@Override
			public List<BrokerItem> loadBroker() {
				return Collections.emptyList();
			}

			@Override
			public boolean store(BrokerItem brokerItem) {
				return true;
			}

			@Override
			public boolean preBuyCheck(int itemForCheck) {
				return true;
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

		InventoryDAO stubInventoryDAO = new com.aionemu.gameserver.dao.impl.InventoryDAO(createNoAccessJdbcClient());

		BrokerService brokerService = new BrokerService(stubBrokerDAO, stubInventoryDAO);
		assertNotNull(brokerService);
	}

	@Test
	void mailServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();

		MailDAO mailDAO = new com.aionemu.gameserver.dao.impl.MailDAO(client);
		InventoryDAO inventoryDAO = new com.aionemu.gameserver.dao.impl.InventoryDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		AbyssRankDAO abyssRankDAO = new com.aionemu.gameserver.dao.impl.AbyssRankDAO(client);

		MailService mailService = new MailService(mailDAO, inventoryDAO, playerDAO, abyssRankDAO);
		assertNotNull(mailService);
	}

	@Test
	void punishmentServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerPunishmentsDAO dao = new com.aionemu.gameserver.dao.impl.PlayerPunishmentsDAO(client);
		PunishmentService service = new PunishmentService(dao);
		assertNotNull(service);
	}

	@Test
	void renameServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		OldNamesDAO oldNamesDAO = new com.aionemu.gameserver.dao.impl.OldNamesDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		LegionDAO legionDAO = new com.aionemu.gameserver.dao.impl.LegionDAO(client);
		RenameService service = new RenameService(oldNamesDAO, playerDAO, legionDAO);
		assertNotNull(service);
	}

	@Test
	void surveyServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SurveyControllerDAO dao = new com.aionemu.gameserver.dao.impl.SurveyControllerDAO(client);
		SurveyService service = new SurveyService(dao);
		assertNotNull(service);
	}

	@Test
	void htmlServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		GuideDAO dao = new com.aionemu.gameserver.dao.impl.GuideDAO(client);
		HTMLService service = new HTMLService(dao);
		assertNotNull(service);
	}

	@Test
	void databaseCleaningServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDAO dao = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		DatabaseCleaningService service = new DatabaseCleaningService(dao);
		assertNotNull(service);
	}

	@Test
	void accountServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		InventoryDAO inventoryDAO = new com.aionemu.gameserver.dao.impl.InventoryDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		PlayerAppearanceDAO playerAppearanceDAO = new com.aionemu.gameserver.dao.impl.PlayerAppearanceDAO(client);
		PlayerPunishmentsDAO playerPunishmentsDAO = new com.aionemu.gameserver.dao.impl.PlayerPunishmentsDAO(client);
		LegionMemberDAO legionMemberDAO = new com.aionemu.gameserver.dao.impl.LegionMemberDAO(client);

		AccountService service = new AccountService(inventoryDAO, playerDAO, playerAppearanceDAO, playerPunishmentsDAO, legionMemberDAO);
		assertNotNull(service);
	}

	@Test
	void socialServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		BlockListDAO blockListDAO = new com.aionemu.gameserver.dao.impl.BlockListDAO(client);
		FriendListDAO friendListDAO = new com.aionemu.gameserver.dao.impl.FriendListDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);

		SocialService service = new SocialService(blockListDAO, friendListDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void limitedQuestServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		LimitedQuestDAO dao = new com.aionemu.gameserver.dao.impl.LimitedQuestDAO(client);
		LimitedQuestService service = new LimitedQuestService(dao);
		assertNotNull(service);
	}

	@Test
	void periodicSaveServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		AbyssRankDAO abyssRankDAO = new com.aionemu.gameserver.dao.impl.AbyssRankDAO(client);
		PlayerSkillListDAO playerSkillListDAO = new com.aionemu.gameserver.dao.impl.PlayerSkillListDAO(client);
		PlayerQuestListDAO playerQuestListDAO = new com.aionemu.gameserver.dao.impl.PlayerQuestListDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		PlayerStigmasEquippedDAO playerStigmasEquippedDAO = new com.aionemu.gameserver.dao.impl.PlayerStigmasEquippedDAO(client);
		InventoryDAO inventoryDAO = new com.aionemu.gameserver.dao.impl.InventoryDAO(client);
		ItemStoneListDAO itemStoneListDAO = new com.aionemu.gameserver.dao.impl.ItemStoneListDAO(client);

		PeriodicSaveService service = new PeriodicSaveService(abyssRankDAO, playerSkillListDAO, playerQuestListDAO,
				playerDAO, playerStigmasEquippedDAO, inventoryDAO, itemStoneListDAO);
		assertNotNull(service);
	}

	@Test
	void webshopServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		RewardServiceDAO dao = new com.aionemu.gameserver.dao.impl.RewardServiceDAO(client);
		WebshopService service = new WebshopService(dao);
		assertNotNull(service);
	}

	@Test
	void challengeTaskServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		ChallengeTasksDAO challengeTasksDAO = new com.aionemu.gameserver.dao.impl.ChallengeTasksDAO(client);
		TownDAO townDAO = new com.aionemu.gameserver.dao.impl.TownDAO(client);
		LegionMemberDAO legionMemberDAO = new com.aionemu.gameserver.dao.impl.LegionMemberDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		ChallengeTaskService service = new ChallengeTaskService(challengeTasksDAO, townDAO, legionMemberDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void baseServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		BaseDAO dao = new com.aionemu.gameserver.dao.impl.BaseDAO(client);
		BaseService service = new BaseService(dao);
		assertNotNull(service);
	}

	@Test
	void siegeServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SiegeDAO dao = new com.aionemu.gameserver.dao.impl.SiegeDAO(client);
		SiegeService service = new SiegeService(dao);
		assertNotNull(service);
	}

	@Test
	void housingBidServiceConstructsWithInjectedDaos() throws Exception {
		JdbcClient client = createNoAccessJdbcClient();
		HouseBidsDAO houseBidsDAO = new com.aionemu.gameserver.dao.impl.HouseBidsDAO(client);
		ServerVariablesDAO serverVariablesDAO = new com.aionemu.gameserver.dao.impl.ServerVariablesDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		HousingBidService service = new HousingBidService(houseBidsDAO, serverVariablesDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void housingServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		HousesDAO dao = new com.aionemu.gameserver.dao.impl.HousesDAO(client);
		HousingService service = new HousingService(dao);
		assertNotNull(service);
	}

	@Test
	void armsfusionServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		InventoryDAO dao = new com.aionemu.gameserver.dao.impl.InventoryDAO(client);
		ArmsfusionService service = new ArmsfusionService(dao);
		assertNotNull(service);
	}

	@Test
	void atreianPassportServiceConstructsWithInjectedDao() {
		PlayerPassportsDAO stubPassportsDAO = new PlayerPassportsDAO() {
			@Override
			public List<Integer> getPassports(int accountId) {
				return Collections.emptyList();
			}

			@Override
			public int getStamps(int accountId, int passportId) {
				return 0;
			}

			@Override
			public Timestamp getLastStamp(int accountId, int passportId) {
				return new Timestamp(0);
			}

			@Override
			public void insertPassport(int accountId, int passportId, int stamps, Timestamp lastStamp) {
			}

			@Override
			public void updatePassport(int accountId, int passportId, int stamps, boolean reset, Timestamp lastStamp) {
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		AtreianPassportService service = new AtreianPassportService(stubPassportsDAO);
		assertNotNull(service);
	}

	@Test
	void shugoSweepServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerShugoSweepDAO shugoSweepDAO = new com.aionemu.gameserver.dao.impl.PlayerShugoSweepDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		ShugoSweepService service = new ShugoSweepService(shugoSweepDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void eventWindowServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerEventsWindowDAO dao = new com.aionemu.gameserver.dao.impl.PlayerEventsWindowDAO(client);
		EventWindowService service = new EventWindowService(dao);
		assertNotNull(service);
	}

	@Test
	void lunaShopServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerLunaShopDAO lunaShopDAO = new com.aionemu.gameserver.dao.impl.PlayerLunaShopDAO(client);
		PlayerWardrobeDAO wardrobeDAO = new com.aionemu.gameserver.dao.impl.PlayerWardrobeDAO(client);
		LunaShopService service = new LunaShopService(lunaShopDAO, wardrobeDAO);
		assertNotNull(service);
	}

	@Test
	void atreianBestiaryServiceConstructsWithInjectedDao() {
		PlayerABDAO stubBestiaryDAO = new PlayerABDAO() {
			@Override
			public PlayerABList load(Player player) {
				return null;
			}

			@Override
			public boolean store(int playerObjId, int id, int killCount, int level, int levelUpable) {
				return true;
			}

			@Override
			public boolean delete(int playerObjId, int slot) {
				return true;
			}

			@Override
			public int getKillCountById(int playerObjId, int id) {
				return 0;
			}

			@Override
			public int getLevelById(int playerObjId, int id) {
				return 0;
			}

			@Override
			public int getClaimRewardById(int playerObjId, int id) {
				return 0;
			}

			@Override
			public boolean supports(String databaseName, int majorVersion, int minorVersion) {
				return true;
			}
		};

		AtreianBestiaryService service = new AtreianBestiaryService(stubBestiaryDAO);
		assertNotNull(service);
	}

	@Test
	void legionServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		LegionDAO legionDAO = new com.aionemu.gameserver.dao.impl.LegionDAO(client);
		LegionMemberDAO legionMemberDAO = new com.aionemu.gameserver.dao.impl.LegionMemberDAO(client);
		InventoryDAO inventoryDAO = new com.aionemu.gameserver.dao.impl.InventoryDAO(client);
		ItemStoneListDAO itemStoneListDAO = new com.aionemu.gameserver.dao.impl.ItemStoneListDAO(client);
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		LegionService service = new LegionService(legionDAO, legionMemberDAO, inventoryDAO, itemStoneListDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void playerServiceDaosConstructWithInjectedImplsAndBind() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerServiceDaos daos = new PlayerServiceDaos(
			new com.aionemu.gameserver.dao.impl.PlayerDAO(client),
			new com.aionemu.gameserver.dao.impl.OldNamesDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerAppearanceDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerSkillListDAO(client),
			new com.aionemu.gameserver.dao.impl.InventoryDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerStigmasEquippedDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerEquipmentSettingDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerSettingsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerQuestListDAO(client),
			new com.aionemu.gameserver.dao.impl.AbyssRankDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerPunishmentsDAO(client),
			new com.aionemu.gameserver.dao.impl.HousesDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerRegisteredItemsDAO(client),
			new com.aionemu.gameserver.dao.impl.ItemStoneListDAO(client),
			new com.aionemu.gameserver.dao.impl.MailDAO(client),
			new com.aionemu.gameserver.dao.impl.PortalCooldownsDAO(client),
			new com.aionemu.gameserver.dao.impl.CraftCooldownsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerNpcFactionsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerLunaShopDAO(client),
			new com.aionemu.gameserver.dao.impl.EventItemsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerMacrossesDAO(client),
			new com.aionemu.gameserver.dao.impl.FriendListDAO(client),
			new com.aionemu.gameserver.dao.impl.BlockListDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerTitleListDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerCreativityPointsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerEventsWindowDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerAtreianBestiaryDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerWardrobeDAO(client),
			new com.aionemu.gameserver.dao.impl.F2pDAO(client),
			new com.aionemu.gameserver.dao.impl.MotionDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerVarsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerRecipesDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerSkillSkinListDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerEffectsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerCooldownsDAO(client),
			new com.aionemu.gameserver.dao.impl.ItemCooldownsDAO(client),
			new com.aionemu.gameserver.dao.impl.HouseObjectCooldownsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerBindPointDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerLifeStatsDAO(client),
			new com.aionemu.gameserver.dao.impl.PlayerEmotionListDAO(client));
		PlayerService.bindDaos(daos);
		assertSame(daos, PlayerService.daos());
		assertNotNull(daos.playerDAO());
		assertNotNull(daos.playerEmotionListDAO());
	}

	private static JdbcClient createNoAccessJdbcClient() {
		DataSource noAccessDataSource = (DataSource) java.lang.reflect.Proxy.newProxyInstance(
			ServiceConstructorInjectionTest.class.getClassLoader(),
			new Class<?>[]{DataSource.class},
			(proxy, method, args) -> null
		);
		return JdbcClient.create(noAccessDataSource);
	}

	@Test
	void growthEnergyConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		GrowthEnergy service = new GrowthEnergy(playerDAO);
		assertNotNull(service);
		assertSame(playerDAO, service.playerDAO());
	}

	@Test
	void petServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerPetsDAO playerPetsDAO = new com.aionemu.gameserver.dao.impl.PlayerPetsDAO(client);
		PetService service = new PetService(playerPetsDAO);
		assertNotNull(service);
		assertSame(playerPetsDAO, service.playerPetsDAO());
	}

	@Test
	void playerTransferServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDAO playerDAO = new com.aionemu.gameserver.dao.impl.PlayerDAO(client);
		LegionMemberDAO legionMemberDAO = new com.aionemu.gameserver.dao.impl.LegionMemberDAO(client);
		String previousRemoveSkillList = PlayerTransferConfig.REMOVE_SKILL_LIST;
		try {
			PlayerTransferConfig.REMOVE_SKILL_LIST = "*";
			PlayerTransferService service = new PlayerTransferService(playerDAO, legionMemberDAO);
			assertNotNull(service);
		} finally {
			PlayerTransferConfig.REMOVE_SKILL_LIST = previousRemoveSkillList;
		}
	}

	@Test
	void servicesFailFastOnNullDaoInjection() {
		assertThrows(NullPointerException.class, () -> new TownService(null));
		assertThrows(NullPointerException.class, () -> new AnnouncementService(null));
		assertThrows(NullPointerException.class, () -> new AbyssLandingService(null));
		assertThrows(NullPointerException.class, () -> new AbyssLandingSpecialService(null));
		assertThrows(NullPointerException.class, () -> new ExchangeService(null));
		assertThrows(NullPointerException.class, () -> new PetitionService(null));
		assertThrows(NullPointerException.class, () -> new BrokerService(null, null));
		assertThrows(NullPointerException.class, () -> new MailService(null, null, null, null));
		assertThrows(NullPointerException.class, () -> new PunishmentService(null));
		assertThrows(NullPointerException.class, () -> new RenameService(null, null, null));
		assertThrows(NullPointerException.class, () -> new SurveyService(null));
		assertThrows(NullPointerException.class, () -> new HTMLService(null));
		assertThrows(NullPointerException.class, () -> new DatabaseCleaningService(null));
		assertThrows(NullPointerException.class, () -> new AccountService(null, null, null, null, null));
		assertThrows(NullPointerException.class, () -> new SocialService(null, null, null));
		assertThrows(NullPointerException.class, () -> new LimitedQuestService(null));
		assertThrows(NullPointerException.class, () -> new PeriodicSaveService(null, null, null, null, null, null, null));
		assertThrows(NullPointerException.class, () -> new WebshopService(null));
		assertThrows(NullPointerException.class, () -> new ChallengeTaskService(null, null, null, null));
		assertThrows(NullPointerException.class, () -> new BaseService(null));
		assertThrows(NullPointerException.class, () -> new SiegeService(null));
		assertThrows(NullPointerException.class, () -> new HousingBidService(null, null, null));
		assertThrows(NullPointerException.class, () -> new HousingService(null));
		assertThrows(NullPointerException.class, () -> new ArmsfusionService(null));
		assertThrows(NullPointerException.class, () -> new AtreianPassportService(null));
		assertThrows(NullPointerException.class, () -> new ShugoSweepService(null, null));
		assertThrows(NullPointerException.class, () -> new EventWindowService(null));
		assertThrows(NullPointerException.class, () -> new LunaShopService(null, null));
		assertThrows(NullPointerException.class, () -> new AtreianBestiaryService(null));
		assertThrows(NullPointerException.class, () -> new LegionService(null, null, null, null, null));
		assertThrows(NullPointerException.class, () -> new GrowthEnergy(null));
		assertThrows(NullPointerException.class, () -> new PetService(null));
		assertThrows(NullPointerException.class, () -> new PlayerTransferService(null, null));
		assertThrows(NullPointerException.class, () -> new PlayerServiceDaos(
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null));
	}
}
