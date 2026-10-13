package com.aionemu.gameserver.services;

import com.aionemu.gameserver.persistence.dao.impl.WebRewardDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.VeteranRewardDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.TownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.SurveyDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.SpecialLandingDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.SiegeLocationDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ServerVariableDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PortalCooldownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerWardrobeDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerTransformDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerTitleDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerStigmasEquippedDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerSkillSkinDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerSkillDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerShugoSweepDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerSettingDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerRegisteredItemDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerRecipeDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerQuestDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerPunishmentDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerPetDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerPassportDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerPasskeyDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerNpcFactionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerMotionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerMinionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerMacrosseDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerLunaShopDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerLifeStatDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerEventsWindowDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerEquipmentSettingDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerEmotionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerEffectDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerCpDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerCooldownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerBindPointDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerAtreianBestiaryDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PlayerAppearanceDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.PetitionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.OutpostLocationDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.OldNameDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.MailDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.LimitedQuestCounterDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.LegionMemberDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.LegionDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.LadderPlayerDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ItemStoneDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ItemCooldownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.InventoryDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.HouseScriptDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.HouseObjectCooldownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.HouseDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.HouseBidDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.GuideDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.FriendDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.F2paccountDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.EventItemDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.CraftCooldownDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.CompetitionRankingDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.ChallengeTaskDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.BrokerDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.BlockDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.BaseLocationDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.AnnouncementDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.AbyssRankDaoImpl;
import com.aionemu.gameserver.persistence.dao.impl.AbyssLandingDaoImpl;
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

import com.aionemu.gameserver.persistence.dao.AbyssLandingDao;
import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.SpecialLandingDao;
import com.aionemu.gameserver.persistence.dao.AnnouncementDao;
import com.aionemu.gameserver.persistence.dao.BaseLocationDao;
import com.aionemu.gameserver.persistence.dao.BlockDao;
import com.aionemu.gameserver.persistence.dao.BrokerDao;
import com.aionemu.gameserver.persistence.dao.ChallengeTaskDao;
import com.aionemu.gameserver.persistence.dao.FriendDao;
import com.aionemu.gameserver.persistence.dao.GuideDao;
import com.aionemu.gameserver.persistence.dao.HouseBidDao;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.dao.LadderPlayerDao;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.dao.LegionMemberDao;
import com.aionemu.gameserver.persistence.dao.LimitedQuestCounterDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.dao.OutpostLocationDao;
import com.aionemu.gameserver.persistence.dao.PetitionDao;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.persistence.dao.PlayerAppearanceDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerEventsWindowDao;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerPassportDao;
import com.aionemu.gameserver.persistence.dao.PlayerPetDao;
import com.aionemu.gameserver.persistence.dao.PlayerPunishmentDao;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.dao.PlayerStigmasEquippedDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.dao.WebRewardDao;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.persistence.dao.SiegeLocationDao;
import com.aionemu.gameserver.persistence.dao.CompetitionRankingDao;
import com.aionemu.gameserver.persistence.dao.SurveyDao;
import com.aionemu.gameserver.persistence.dao.TownDao;
import com.aionemu.gameserver.persistence.dao.VeteranRewardDao;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.siegeservice.ArtifactSiege;
import com.aionemu.gameserver.services.siegeservice.FortressSiege;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.player.PlayerServiceDaos;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.configs.main.CleaningConfig;
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

		TownDao stubTownDAO = new TownDaoImpl() {
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
		AnnouncementDao stubAnnouncementsDAO = new AnnouncementDaoImpl() {
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
		};

		AnnouncementService announcementService = new AnnouncementService(stubAnnouncementsDAO);
		assertNotNull(announcementService.getAnnouncements());
		assertEquals(0, announcementService.getAnnouncements().size());
	}

	@Test
	void abyssLandingServiceConstructsWithInjectedDao() {
		AbyssLandingDao stubAbyssLandingDAO = new AbyssLandingDaoImpl() {
			@Override
			public void loadLandingLocations(Map<Integer, LandingLocation> locations) {
				return;
			}

			@Override
			public void store(LandingLocation location) {
			}

			@Override
			public boolean updateLandingLocation(LandingLocation location) {
				return true;
			}
		};

		AbyssLandingService service = new AbyssLandingService(stubAbyssLandingDAO,
			new AbyssLandingSpecialService(new SpecialLandingDaoImpl()));
		assertNotNull(service);
	}

	@Test
	void abyssLandingSpecialServiceConstructsWithInjectedDao() {
		SpecialLandingDao stubAbyssSpecialLandingDAO = new SpecialLandingDaoImpl() {
			@Override
			public void loadLandingSpecialLocations(Map<Integer, LandingSpecialLocation> locations) {
				return;
			}

			@Override
			public void store(LandingSpecialLocation location) {
			}

			@Override
			public boolean updateLandingSpecialLocation(LandingSpecialLocation location) {
				return true;
			}
		};

		AbyssLandingSpecialService service = new AbyssLandingSpecialService(stubAbyssSpecialLandingDAO);
		assertNotNull(service);
	}

	@Test
	void abyssRankUpdateServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		AbyssRankDao abyssRankDAO = new AbyssRankDaoImpl();
		ServerVariableDao serverVariablesDAO = new ServerVariableDaoImpl();

		AbyssRankUpdateService service = new AbyssRankUpdateService(abyssRankDAO, serverVariablesDAO,
				new org.objenesis.ObjenesisStd().newInstance(AbyssRankingCache.class));
		assertNotNull(service);
	}

	@Test
	void abyssRankCleaningServiceConstructsWithInjectedDao() {
		boolean previousEnable = CleaningConfig.ABYSS_CLEANING_ENABLE;
		try {
			CleaningConfig.ABYSS_CLEANING_ENABLE = false;
			JdbcClient client = createNoAccessJdbcClient();
			AbyssRankDao abyssRankDAO = new AbyssRankDaoImpl();

			AbyssRankCleaningService service = new AbyssRankCleaningService(abyssRankDAO,
				new org.objenesis.ObjenesisStd().newInstance(AbyssRankingCache.class));
			assertNotNull(service);
		} finally {
			CleaningConfig.ABYSS_CLEANING_ENABLE = previousEnable;
		}
	}

	@Test
	void abyssRankingCacheConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		AbyssRankDao abyssRankDAO = new AbyssRankDaoImpl();

		AbyssRankingCache service = new AbyssRankingCache(abyssRankDAO,
				new org.objenesis.ObjenesisStd().newInstance(LegionService.class));
		assertNotNull(service);
	}

	@Test
	void exchangeServiceConstructsWithInjectedDao() {
		InventoryDao concreteStubDao = new InventoryDaoImpl() {
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
		};

		ExchangeService service = new ExchangeService(concreteStubDao);
		assertNotNull(service);
	}

	@Test
	void petitionServiceConstructsWithInjectedDaoAndLoadsPetitions() {
		Petition stubPetition = new Petition(501);
		PetitionDao stubPetitionDAO = new PetitionDaoImpl() {
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
		};

		PetitionService service = new PetitionService(stubPetitionDAO);
		assertEquals(1, service.getRegisteredPetitions().size());
	}

	@Test
	void brokerServiceRequiresRunningContainer() {
		BrokerDao stubBrokerDAO = new BrokerDaoImpl(new ItemStoneDaoImpl()) {
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
		};

		InventoryDao stubInventoryDAO = new InventoryDaoImpl();

		// BrokerService 在构造期登记过期物品任务，容器未启动即应快速失败。
		// BrokerService schedules the expired-items task during construction, so it must fail fast outside the container.
		assertThrows(IllegalStateException.class, () -> new BrokerService(stubBrokerDAO, stubInventoryDAO));
	}

	@Test
	void mailServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();

		MailDao mailDAO = new MailDaoImpl(new ItemStoneDaoImpl());
		InventoryDao inventoryDAO = new InventoryDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		AbyssRankDao abyssRankDAO = new AbyssRankDaoImpl();

		MailService mailService = new MailService(mailDAO, inventoryDAO, playerDAO, abyssRankDAO);
		assertNotNull(mailService);
	}

	@Test
	void punishmentServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerPunishmentDao dao = new PlayerPunishmentDaoImpl();
		PunishmentService service = new PunishmentService(dao);
		assertNotNull(service);
	}

	@Test
	void renameServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		OldNameDao oldNamesDAO = new OldNameDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		LegionDao legionDAO = new LegionDaoImpl(null, null, null, null, null, null, new org.springframework.transaction.support.TransactionTemplate());
		RenameService service = new RenameService(oldNamesDAO, playerDAO, legionDAO);
		assertNotNull(service);
	}

	@Test
	void surveyServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SurveyDao dao = new SurveyDaoImpl();
		SurveyService service = new SurveyService(dao);
		assertNotNull(service);
	}

	@Test
	void htmlServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		GuideDao dao = new GuideDaoImpl();
		HTMLService service = new HTMLService(dao);
		assertNotNull(service);
	}

	@Test
	void databaseCleaningServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDao dao = new PlayerDaoImpl();
		DatabaseCleaningService service = new DatabaseCleaningService(dao);
		assertNotNull(service);
	}

	@Test
	void accountServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		InventoryDao inventoryDAO = new InventoryDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		PlayerAppearanceDao playerAppearanceDAO = new PlayerAppearanceDaoImpl();
		PlayerPunishmentDao playerPunishmentsDAO = new PlayerPunishmentDaoImpl();
		LegionMemberDao legionMemberDAO = new LegionMemberDaoImpl();

		AccountService service = new AccountService(inventoryDAO, playerDAO, playerAppearanceDAO, playerPunishmentsDAO, legionMemberDAO);
		assertNotNull(service);
	}

	@Test
	void socialServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDao playerDAO = new PlayerDaoImpl();
		BlockDao blockListDAO = new BlockDaoImpl(new PlayerDaoImpl());
		FriendDao friendListDAO = new FriendDaoImpl(new PlayerDaoImpl());

		SocialService service = new SocialService(blockListDAO, friendListDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void limitedQuestServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		LimitedQuestCounterDao dao = new LimitedQuestCounterDaoImpl();
		LimitedQuestService service = new LimitedQuestService(dao);
		assertNotNull(service);
	}

	@Test
	void periodicSaveServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		AbyssRankDao abyssRankDAO = new AbyssRankDaoImpl();
		PlayerSkillDao playerSkillListDAO = new PlayerSkillDaoImpl();
		PlayerQuestDao playerQuestListDAO = new PlayerQuestDaoImpl(new org.springframework.transaction.support.TransactionTemplate());
		PlayerDao playerDAO = new PlayerDaoImpl();
		PlayerStigmasEquippedDao playerStigmasEquippedDAO = new PlayerStigmasEquippedDaoImpl();
		InventoryDao inventoryDAO = new InventoryDaoImpl();
		ItemStoneDao itemStoneListDAO = new ItemStoneDaoImpl();

		PeriodicSaveService service = new PeriodicSaveService(abyssRankDAO, playerSkillListDAO, playerQuestListDAO,
				playerDAO, playerStigmasEquippedDAO, inventoryDAO, itemStoneListDAO,
				new org.objenesis.ObjenesisStd().newInstance(LegionService.class));
		assertNotNull(service);
	}

	@Test
	void webshopServiceRequiresRunningContainer() {
		// WebshopService 构造期 load() 需要静态数据服务，容器未启动即应快速失败。
		// WebshopService.load() needs static-data services during construction, so it must fail fast outside the container.
		WebRewardDao dao = new WebRewardDaoImpl();
		assertThrows(IllegalStateException.class, () -> new WebshopService(dao));
	}

	@Test
	void challengeTaskServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		ChallengeTaskDao challengeTasksDAO = new ChallengeTaskDaoImpl();
		TownDao townDAO = new TownDaoImpl();
		LegionMemberDao legionMemberDAO = new LegionMemberDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		ChallengeTaskService service = new ChallengeTaskService(challengeTasksDAO, townDAO, legionMemberDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void baseServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		BaseLocationDao dao = new BaseLocationDaoImpl();
		BaseService service = new BaseService(dao);
		assertNotNull(service);
	}

	@Test
	void siegeServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SiegeLocationDao dao = new SiegeLocationDaoImpl();
		SiegeService service = new SiegeService(dao, new org.objenesis.ObjenesisStd().newInstance(LegionService.class));
		assertNotNull(service);
	}

	@Test
	void housingBidServiceConstructsWithInjectedDaos() throws Exception {
		JdbcClient client = createNoAccessJdbcClient();
		HouseBidDao houseBidsDAO = new HouseBidDaoImpl();
		ServerVariableDao serverVariablesDAO = new ServerVariableDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		HousingBidService service = new HousingBidService(houseBidsDAO, serverVariablesDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void housingServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		HouseDao dao = new HouseDaoImpl();
		HousingService service = new HousingService(dao);
		assertNotNull(service);
	}

	@Test
	void armsfusionServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		InventoryDao dao = new InventoryDaoImpl();
		ArmsfusionService service = new ArmsfusionService(dao);
		assertNotNull(service);
	}

	@Test
	void atreianPassportServiceConstructsWithInjectedDao() {
		PlayerPassportDao stubPassportsDAO = new PlayerPassportDaoImpl() {
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
		};

		AtreianPassportService service = new AtreianPassportService(stubPassportsDAO);
		assertNotNull(service);
	}

	@Test
	void shugoSweepServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerShugoSweepDao shugoSweepDAO = new PlayerShugoSweepDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		ShugoSweepService service = new ShugoSweepService(shugoSweepDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void eventWindowServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerEventsWindowDao dao = new PlayerEventsWindowDaoImpl();
		EventWindowService service = new EventWindowService(dao);
		assertNotNull(service);
	}

	@Test
	void lunaShopServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerLunaShopDao lunaShopDAO = new PlayerLunaShopDaoImpl();
		PlayerWardrobeDao wardrobeDAO = new PlayerWardrobeDaoImpl();
		LunaShopService service = new LunaShopService(lunaShopDAO, wardrobeDAO);
		assertNotNull(service);
	}

	@Test
	void atreianBestiaryServiceConstructsWithInjectedDao() {
		PlayerAtreianBestiaryDao stubBestiaryDAO = new PlayerAtreianBestiaryDaoImpl() {
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
		};

		AtreianBestiaryService service = new AtreianBestiaryService(stubBestiaryDAO);
		assertNotNull(service);
	}

	@Test
	void legionServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		LegionDao legionDAO = new LegionDaoImpl(null, null, null, null, null, null, new org.springframework.transaction.support.TransactionTemplate());
		LegionMemberDao legionMemberDAO = new LegionMemberDaoImpl();
		InventoryDao inventoryDAO = new InventoryDaoImpl();
		ItemStoneDao itemStoneListDAO = new ItemStoneDaoImpl();
		PlayerDao playerDAO = new PlayerDaoImpl();
		LegionService service = new LegionService(legionDAO, legionMemberDAO, inventoryDAO, itemStoneListDAO, playerDAO);
		assertNotNull(service);
	}

	@Test
	void playerServiceDaosConstructWithInjectedImplsAndBind() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerServiceDaos daos = new PlayerServiceDaos(
			new PlayerDaoImpl(),
			new OldNameDaoImpl(),
			new PlayerAppearanceDaoImpl(),
			new PlayerSkillDaoImpl(),
			new InventoryDaoImpl(),
			new PlayerStigmasEquippedDaoImpl(),
			new PlayerEquipmentSettingDaoImpl(),
			new PlayerSettingDaoImpl(),
			new PlayerQuestDaoImpl(new org.springframework.transaction.support.TransactionTemplate()),
			new AbyssRankDaoImpl(),
			new PlayerPunishmentDaoImpl(),
			new HouseDaoImpl(),
			new PlayerRegisteredItemDaoImpl(),
			new ItemStoneDaoImpl(),
			new MailDaoImpl(new ItemStoneDaoImpl()),
			new PortalCooldownDaoImpl(),
			new CraftCooldownDaoImpl(),
			new PlayerNpcFactionDaoImpl(),
			new PlayerLunaShopDaoImpl(),
			new EventItemDaoImpl(),
			new PlayerMacrosseDaoImpl(),
			new FriendDaoImpl(new PlayerDaoImpl()),
			new BlockDaoImpl(new PlayerDaoImpl()),
			new PlayerTitleDaoImpl(),
			new PlayerCpDaoImpl(),
			new PlayerEventsWindowDaoImpl(),
			new PlayerAtreianBestiaryDaoImpl(),
			new PlayerWardrobeDaoImpl(),
			new F2paccountDaoImpl(),
			new PlayerMotionDaoImpl(),
			com.aionemu.gameserver.persistence.dao.PlayerVarDao.class.cast(java.lang.reflect.Proxy.newProxyInstance(
				ServiceConstructorInjectionTest.class.getClassLoader(),
				new Class<?>[]{com.aionemu.gameserver.persistence.dao.PlayerVarDao.class},
				(proxy, method, args) -> null)),
			new PlayerRecipeDaoImpl(),
			new PlayerSkillSkinDaoImpl(),
			new PlayerEffectDaoImpl(),
			new PlayerCooldownDaoImpl(),
			new ItemCooldownDaoImpl(),
			new HouseObjectCooldownDaoImpl(),
			new PlayerBindPointDaoImpl(),
			new PlayerLifeStatDaoImpl(),
			new PlayerEmotionDaoImpl(),
			new PlayerPetDaoImpl(),
			new PlayerMinionDaoImpl(),
			new PlayerShugoSweepDaoImpl(),
			new HouseScriptDaoImpl(),
			new PlayerPasskeyDaoImpl(),
			new PlayerTransformDaoImpl());
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
		PlayerDao playerDAO = new PlayerDaoImpl();
		GrowthEnergy service = new GrowthEnergy(playerDAO);
		assertNotNull(service);
		assertSame(playerDAO, service.playerDAO());
	}

	@Test
	void petServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerPetDao playerPetsDAO = new PlayerPetDaoImpl();
		PetService service = new PetService(playerPetsDAO);
		assertNotNull(service);
		assertSame(playerPetsDAO, service.playerPetsDAO());
	}

	@Test
	void playerTransferServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDao playerDAO = new PlayerDaoImpl();
		LegionMemberDao legionMemberDAO = new LegionMemberDaoImpl();
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
	void veteranRewardsServiceConstructsWithInjectedDaos() {
		try (com.aionemu.commons.services.ServiceContext.Scope ignored =
				com.aionemu.commons.services.ServiceContext.use("veteran-rewards-injection-test-" + System.nanoTime())) {
			com.aionemu.commons.services.CronService cronService =
					com.aionemu.commons.services.CronService.initSingleton(TestRunnableRunner.class);
			try {
				JdbcClient client = createNoAccessJdbcClient();
				VeteranRewardDao veteranRewardsDAO = new VeteranRewardDaoImpl();
				PlayerDao playerDAO = new PlayerDaoImpl();
				VeteranRewardsService service = new VeteranRewardsService(veteranRewardsDAO, playerDAO);
				assertNotNull(service);
			} finally {
				cronService.shutdown();
			}
		}
	}

	@Test
	void seasonRankingServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		CompetitionRankingDao seasonRankingDAO = new CompetitionRankingDaoImpl();
		SeasonRankingService service = new SeasonRankingService(seasonRankingDAO);
		assertNotNull(service);
	}

	@Test
	void territoryServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		LegionDao legionDAO = new LegionDaoImpl(null, null, null, null, null, null, new org.springframework.transaction.support.TransactionTemplate());
		TerritoryService service = new TerritoryService(legionDAO, new org.objenesis.ObjenesisStd().newInstance(LegionService.class));
		assertNotNull(service);
	}

	@Test
	void rewardServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		WebRewardDao dao = new WebRewardDaoImpl();
		RewardService service = new RewardService(dao);
		assertNotNull(service);
	}

	@Test
	void ladderServiceRequiresRunningContainer() {
		// LadderService 在构造期就要向线程池登记周期任务，容器未启动即应快速失败。
		// LadderService schedules a periodic task during construction, so it must fail fast outside the container.
		LadderPlayerDao ladderDAO = new LadderPlayerDaoImpl();
		assertThrows(IllegalStateException.class, () -> new LadderService(ladderDAO));
	}

	@Test
	void seasonRankingUpdateServiceConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		CompetitionRankingDao seasonRankingDAO = new CompetitionRankingDaoImpl();
		SeasonRankingUpdateService service = new SeasonRankingUpdateService(seasonRankingDAO);
		assertNotNull(service);
	}

	/** 供 cron 依赖服务离线构造使用的最简执行器。 / Minimal runner for offline construction of cron-dependent services. */
	public static final class TestRunnableRunner extends com.aionemu.commons.services.cron.RunnableRunner {
		@Override
		public void executeRunnable(Runnable r) {
			r.run();
		}

		@Override
		public void executeLongRunningRunnable(Runnable r) {
			r.run();
		}
	}

	@Test
	void systemMailServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		PlayerDao playerDAO = new PlayerDaoImpl();
		InventoryDao inventoryDAO = new InventoryDaoImpl();
		MailDao mailDAO = new MailDaoImpl(new ItemStoneDaoImpl());
		SystemMailService service = new SystemMailService(playerDAO, inventoryDAO, mailDAO);
		assertNotNull(service);
	}

	@Test
	void outpostServiceConstructsWithInjectedDaos() {
		JdbcClient client = createNoAccessJdbcClient();
		OutpostLocationDao outpostDAO = new OutpostLocationDaoImpl();
		SiegeLocationDao siegeDAO = new SiegeLocationDaoImpl();
		OutpostService service = new OutpostService(outpostDAO, siegeDAO);
		assertNotNull(service);
	}

	@Test
	void fortressSiegeConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SiegeLocationDao siegeDAO = new SiegeLocationDaoImpl();
		FortressLocation location = new org.objenesis.ObjenesisStd().newInstance(FortressLocation.class);
		FortressSiege siege = new FortressSiege(location, siegeDAO);
		assertNotNull(siege);
	}

	@Test
	void artifactSiegeConstructsWithInjectedDao() {
		JdbcClient client = createNoAccessJdbcClient();
		SiegeLocationDao siegeDAO = new SiegeLocationDaoImpl();
		ArtifactLocation location = new org.objenesis.ObjenesisStd().newInstance(ArtifactLocation.class);
		ArtifactSiege siege = new ArtifactSiege(location, siegeDAO);
		assertNotNull(siege);
	}

	@Test
	void servicesFailFastOnNullDaoInjection() {
		assertThrows(NullPointerException.class, () -> new TownService(null));
		assertThrows(NullPointerException.class, () -> new AnnouncementService(null));
		assertThrows(NullPointerException.class,
			() -> new AbyssLandingService(null, new AbyssLandingSpecialService(new SpecialLandingDaoImpl())));
		assertThrows(NullPointerException.class, () -> new AbyssLandingSpecialService(null));
		assertThrows(NullPointerException.class, () -> new AbyssRankUpdateService(null, null, null));
		assertThrows(NullPointerException.class, () -> new AbyssRankCleaningService(null, null));
		assertThrows(NullPointerException.class, () -> new AbyssRankingCache(null, null));
		assertThrows(NullPointerException.class, () -> new ExchangeService(null));
		assertThrows(NullPointerException.class, () -> new PetitionService(null));
		assertThrows(NullPointerException.class, () -> new BrokerService(null, null));
		assertThrows(NullPointerException.class, () -> new MailService(null, null, null, null));
		assertThrows(NullPointerException.class, () -> new SystemMailService(null, null, null));
		assertThrows(NullPointerException.class, () -> new OutpostService(null, null));
		assertThrows(NullPointerException.class, () -> new FortressSiege(null, null));
		assertThrows(NullPointerException.class, () -> new ArtifactSiege(null, null));
		assertThrows(NullPointerException.class, () -> new PunishmentService(null));
		assertThrows(NullPointerException.class, () -> new RenameService(null, null, null));
		assertThrows(NullPointerException.class, () -> new SurveyService(null));
		assertThrows(NullPointerException.class, () -> new HTMLService(null));
		assertThrows(NullPointerException.class, () -> new DatabaseCleaningService(null));
		assertThrows(NullPointerException.class, () -> new AccountService(null, null, null, null, null));
		assertThrows(NullPointerException.class, () -> new SocialService(null, null, null));
		assertThrows(NullPointerException.class, () -> new LimitedQuestService(null));
		assertThrows(NullPointerException.class, () -> new PeriodicSaveService(null, null, null, null, null, null, null, null));
		assertThrows(NullPointerException.class, () -> new WebshopService(null));
		assertThrows(NullPointerException.class, () -> new ChallengeTaskService(null, null, null, null));
		assertThrows(NullPointerException.class, () -> new BaseService(null));
		assertThrows(NullPointerException.class, () -> new SiegeService(null, null));
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
		assertThrows(NullPointerException.class, () -> new VeteranRewardsService(null, null));
		assertThrows(NullPointerException.class, () -> new SeasonRankingService(null));
		assertThrows(NullPointerException.class, () -> new TerritoryService(null, null));
		assertThrows(NullPointerException.class, () -> new RewardService(null));
		assertThrows(NullPointerException.class, () -> new LadderService(null));
		assertThrows(NullPointerException.class, () -> new SeasonRankingUpdateService(null));
		assertThrows(NullPointerException.class, () -> new PlayerServiceDaos(
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null));
	}
}
