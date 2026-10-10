package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.controllers.FlyController;
import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.CraftCooldownsDAO;
import com.aionemu.gameserver.dao.EventItemsDAO;
import com.aionemu.gameserver.dao.F2pDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.HouseObjectCooldownsDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemCooldownsDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.MotionDAO;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerBindPointDAO;
import com.aionemu.gameserver.dao.PlayerCooldownsDAO;
import com.aionemu.gameserver.dao.PlayerCreativityPointsDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerEffectsDAO;
import com.aionemu.gameserver.dao.PlayerEmotionListDAO;
import com.aionemu.gameserver.dao.PlayerEquipmentSettingDAO;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.dao.PlayerLifeStatsDAO;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerMacrossesDAO;
import com.aionemu.gameserver.dao.PlayerNpcFactionsDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.dao.PlayerSettingsDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dao.PlayerVarsDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dao.PortalCooldownsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData;
import com.aionemu.gameserver.dataholders.PlayerInitialData.LocationData;
import com.aionemu.gameserver.dataholders.PlayerInitialData.PlayerCreationData;
import com.aionemu.gameserver.dataholders.PlayerInitialData.PlayerCreationData.ItemType;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.model.stats.calc.functions.PlayerStatFunctions;
import com.aionemu.gameserver.model.stats.listeners.TitleChangeListener;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.collections.cachemap.CacheMap;
import com.aionemu.gameserver.utils.collections.cachemap.CacheMapFactory;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.KnownList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 玩家核心服务，处理角色创建、删除、宏与持久化。
 * Core player service handling character create/delete, macros and persistence.
 */
public class PlayerService {
	private static final CacheMap<Integer, Player> playerCache = CacheMapFactory.createSoftCacheMap("Player", "player");

	/**
	 * Spring 绑定的持久层依赖聚合 / Spring-bound persistence dependency aggregate.
	 */
	private static volatile PlayerServiceDaos boundDaos;

	/**
	 * 由 Spring 配置类在启动期绑定持久层依赖聚合。
	 * Binds the persistence dependency aggregate from the Spring configuration at startup.
	 *
	 * @param daos 依赖聚合 / dependency aggregate
	 */
	public static void bindDaos(PlayerServiceDaos daos) {
		boundDaos = Objects.requireNonNull(daos, "daos must not be null");
	}

	/**
	 * 返回持久层依赖聚合；未绑定（离线/单测环境）时按 safeDao 兜底构建并缓存。
	 * Returns the persistence dependency aggregate; falls back to safeDao construction when unbound (offline/unit tests).
	 *
	 * @return 依赖聚合 / dependency aggregate
	 */
	public static PlayerServiceDaos daos() {
		PlayerServiceDaos current = boundDaos;
		if (current == null) {
			synchronized (PlayerService.class) {
				current = boundDaos;
				if (current == null) {
					boundDaos = current = buildOfflineDaos();
				}
			}
		}
		return current;
	}

	/**
	 * 离线兜底构建依赖聚合：DAOManager 已初始化时优先解析 Spring Bean，否则退回实现类无参构造。
	 * Builds the aggregate offline: resolves the Spring bean when DAOManager is initialized, else falls back to no-arg impl constructors.
	 */
	private static PlayerServiceDaos buildOfflineDaos() {
		return new PlayerServiceDaos(
			safeDao(PlayerDAO.class, com.aionemu.gameserver.dao.impl.PlayerDAO::new),
			safeDao(OldNamesDAO.class, com.aionemu.gameserver.dao.impl.OldNamesDAO::new),
			safeDao(PlayerAppearanceDAO.class, com.aionemu.gameserver.dao.impl.PlayerAppearanceDAO::new),
			safeDao(PlayerSkillListDAO.class, com.aionemu.gameserver.dao.impl.PlayerSkillListDAO::new),
			safeDao(InventoryDAO.class, com.aionemu.gameserver.dao.impl.InventoryDAO::new),
			safeDao(PlayerStigmasEquippedDAO.class, com.aionemu.gameserver.dao.impl.PlayerStigmasEquippedDAO::new),
			safeDao(PlayerEquipmentSettingDAO.class, com.aionemu.gameserver.dao.impl.PlayerEquipmentSettingDAO::new),
			safeDao(PlayerSettingsDAO.class, com.aionemu.gameserver.dao.impl.PlayerSettingsDAO::new),
			safeDao(PlayerQuestListDAO.class, com.aionemu.gameserver.dao.impl.PlayerQuestListDAO::new),
			safeDao(AbyssRankDAO.class, com.aionemu.gameserver.dao.impl.AbyssRankDAO::new),
			safeDao(PlayerPunishmentsDAO.class, com.aionemu.gameserver.dao.impl.PlayerPunishmentsDAO::new),
			safeDao(HousesDAO.class, com.aionemu.gameserver.dao.impl.HousesDAO::new),
			safeDao(PlayerRegisteredItemsDAO.class, com.aionemu.gameserver.dao.impl.PlayerRegisteredItemsDAO::new),
			safeDao(ItemStoneListDAO.class, com.aionemu.gameserver.dao.impl.ItemStoneListDAO::new),
			safeDao(MailDAO.class, com.aionemu.gameserver.dao.impl.MailDAO::new),
			safeDao(PortalCooldownsDAO.class, com.aionemu.gameserver.dao.impl.PortalCooldownsDAO::new),
			safeDao(CraftCooldownsDAO.class, com.aionemu.gameserver.dao.impl.CraftCooldownsDAO::new),
			safeDao(PlayerNpcFactionsDAO.class, com.aionemu.gameserver.dao.impl.PlayerNpcFactionsDAO::new),
			safeDao(PlayerLunaShopDAO.class, com.aionemu.gameserver.dao.impl.PlayerLunaShopDAO::new),
			safeDao(EventItemsDAO.class, com.aionemu.gameserver.dao.impl.EventItemsDAO::new),
			safeDao(PlayerMacrossesDAO.class, com.aionemu.gameserver.dao.impl.PlayerMacrossesDAO::new),
			safeDao(FriendListDAO.class, com.aionemu.gameserver.dao.impl.FriendListDAO::new),
			safeDao(BlockListDAO.class, com.aionemu.gameserver.dao.impl.BlockListDAO::new),
			safeDao(PlayerTitleListDAO.class, com.aionemu.gameserver.dao.impl.PlayerTitleListDAO::new),
			safeDao(PlayerCreativityPointsDAO.class, com.aionemu.gameserver.dao.impl.PlayerCreativityPointsDAO::new),
			safeDao(PlayerEventsWindowDAO.class, com.aionemu.gameserver.dao.impl.PlayerEventsWindowDAO::new),
			safeDao(PlayerABDAO.class, com.aionemu.gameserver.dao.impl.PlayerAtreianBestiaryDAO::new),
			safeDao(PlayerWardrobeDAO.class, com.aionemu.gameserver.dao.impl.PlayerWardrobeDAO::new),
			safeDao(F2pDAO.class, com.aionemu.gameserver.dao.impl.F2pDAO::new),
			safeDao(MotionDAO.class, com.aionemu.gameserver.dao.impl.MotionDAO::new),
			safeDao(PlayerVarsDAO.class, com.aionemu.gameserver.dao.impl.PlayerVarsDAO::new),
			safeDao(PlayerRecipesDAO.class, com.aionemu.gameserver.dao.impl.PlayerRecipesDAO::new),
			safeDao(PlayerSkillSkinListDAO.class, com.aionemu.gameserver.dao.impl.PlayerSkillSkinListDAO::new),
			safeDao(PlayerEffectsDAO.class, com.aionemu.gameserver.dao.impl.PlayerEffectsDAO::new),
			safeDao(PlayerCooldownsDAO.class, com.aionemu.gameserver.dao.impl.PlayerCooldownsDAO::new),
			safeDao(ItemCooldownsDAO.class, com.aionemu.gameserver.dao.impl.ItemCooldownsDAO::new),
			safeDao(HouseObjectCooldownsDAO.class, com.aionemu.gameserver.dao.impl.HouseObjectCooldownsDAO::new),
			safeDao(PlayerBindPointDAO.class, com.aionemu.gameserver.dao.impl.PlayerBindPointDAO::new),
			safeDao(PlayerLifeStatsDAO.class, com.aionemu.gameserver.dao.impl.PlayerLifeStatsDAO::new),
			safeDao(PlayerEmotionListDAO.class, com.aionemu.gameserver.dao.impl.PlayerEmotionListDAO::new),
			safeDao(com.aionemu.gameserver.dao.PlayerPetsDAO.class, com.aionemu.gameserver.dao.impl.PlayerPetsDAO::new),
			safeDao(com.aionemu.gameserver.dao.PlayerMinionsDAO.class, com.aionemu.gameserver.dao.impl.PlayerMinionsDAO::new),
			safeDao(com.aionemu.gameserver.dao.PlayerShugoSweepDAO.class, com.aionemu.gameserver.dao.impl.PlayerShugoSweepDAO::new),
			safeDao(com.aionemu.gameserver.dao.HouseScriptsDAO.class, com.aionemu.gameserver.dao.impl.HouseScriptsDAO::new),
			safeDao(com.aionemu.gameserver.dao.PlayerPasskeyDAO.class, com.aionemu.gameserver.dao.impl.PlayerPasskeyDAO::new),
			safeDao(com.aionemu.gameserver.dao.PlayerTransformDAO.class, com.aionemu.gameserver.dao.impl.PlayerTransfoDAO::new));
	}

	private static <T extends com.aionemu.commons.database.dao.DAO> T safeDao(Class<T> daoClass,
																			  java.util.function.Supplier<T> fallback) {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(daoClass);
			} catch (Exception ignored) {
			}
		}
		return fallback.get();
	}

	/**
	 * 检查角色名是否未被占用。
	 * Checks whether the player name is unused.
	 *
	 * @param name 角色名 / player name
	 * @return 是否可用 / whether free
	 */
	public static boolean isFreeName(String name) {
		return !daos().playerDAO().isNameUsed(name);
	}

	/**
	 * 检查角色名是否与旧名一致。
	 * Checks whether the name matches the previous one.
	 *
	 * @param name 角色名 / player name
	 * @return 是否一致 / whether equal
	 */
	public static boolean isOldName(String name) {
		return daos().oldNamesDAO().isOldName(name);
	}

	/**
	 * 持久化新建角色。
	 * Persists a newly created player.
	 *
	 * @param player      玩家 / player
	 * @param accountName 账号名 / account name
	 * @param accountId   账号 ID / account id
	 * @return 是否成功 / whether successful
	 */
	public static boolean storeNewPlayer(Player player, String accountName, int accountId) {
		return daos().playerDAO().saveNewPlayer(player.getCommonData(), accountId, accountName)
			&& daos().playerAppearanceDAO().store(player)
			&& daos().playerSkillListDAO().storeSkills(player)
			&& daos().inventoryDAO().store(player);
	}

	/**
	 * 持久化角色数据。
	 * Persists player data.
	 *
	 * @param player 玩家 / player
	 */
	public static void storePlayer(Player player) {
		daos().playerDAO().storePlayer(player);
		daos().playerSkillListDAO().storeSkills(player);
		daos().playerStigmasEquippedDAO().storeItems(player);
		daos().playerEquipmentSettingDAO().store(player);
		daos().playerSettingsDAO().saveSettings(player);
		daos().playerQuestListDAO().store(player);
		daos().abyssRankDAO().storeAbyssRank(player);
		daos().playerPunishmentsDAO().storePlayerPunishments(player, PunishmentType.PRISON);
		daos().playerPunishmentsDAO().storePlayerPunishments(player, PunishmentType.GATHER);
		daos().inventoryDAO().store(player);
		for (House house : player.getHouses()) {
			daos().housesDAO().storeHouse(house);
			if (house.getRegistry() != null
				&& house.getRegistry().getPersistentState() == PersistentState.UPDATE_REQUIRED) {
				daos().playerRegisteredItemsDAO().store(house.getRegistry(),
					player.getCommonData().getPlayerObjId());
			}
		}
		daos().itemStoneListDAO().save(player);
		daos().mailDAO().storeMailbox(player);
		daos().portalCooldownsDAO().storePortalCooldowns(player);
		daos().craftCooldownsDAO().storeCraftCooldowns(player);
		daos().playerNpcFactionsDAO().storeNpcFactions(player);
		daos().playerLunaShopDAO().store(player);
		daos().eventItemsDAO().loadItems(player);
	}

	/**
	 * 获取玩家对象。
	 * Gets the player object.
	 *
	 * @param playerObjId 角色对象 ID / player object id
	 * @param account     账号 / account
	 * @return 玩家对象 / player object
	 */
	public static Player getPlayer(int playerObjId, Account account) {
		Player player = playerCache.get(playerObjId);
		if (player != null) {
			return player;
		}
		PlayerAccountData playerAccountData = account.getPlayerAccountData(playerObjId);
		PlayerCommonData pcd = playerAccountData.getPlayerCommonData();
		PlayerAppearance appearance = playerAccountData.getAppereance();
		player = new Player(new PlayerController(), pcd, appearance, account);
		LegionMember legionMember = GameCoreGameplayServices.legionService().getLegionMember(player.getObjectId());
		if (legionMember != null) {
			player.setLegionMember(legionMember);
		}
		MacroList macroses = daos().playerMacrossesDAO().restoreMacrosses(playerObjId);
		player.setMacroList(macroses);
		player.setSkillList(daos().playerSkillListDAO().loadSkillList(playerObjId));
		player.setEquipedStigmaList(daos().playerStigmasEquippedDAO().loadItemsList(playerObjId));
		player.setKnownlist(new KnownList(player));
		player.setFriendList(daos().friendListDAO().load(player));
		player.setBlockList(daos().blockListDAO().load(player));
		player.setTitleList(daos().playerTitleListDAO().loadTitleList(playerObjId));
		player.setCP(daos().playerCreativityPointsDAO().loadCP(player));
		player.setEventWindow(daos().playerEventsWindowDAO().load(player));
		player.setAtreianBestiary(daos().playerABDAO().load(player));
		player.setWardrobe(daos().playerWardrobeDAO().load(player));
		daos().f2pDAO().loadF2pInfo(player);
		daos().playerSettingsDAO().loadSettings(player);
		daos().playerEquipmentSettingDAO().loadEquipmentSetting(player);
		daos().abyssRankDAO().loadAbyssRank(player);
		daos().playerNpcFactionsDAO().loadNpcFactions(player);
		daos().motionDAO().loadMotionList(player);
		player.setVars(daos().playerVarsDAO().load(player.getObjectId()));
		Equipment equipment = daos().inventoryDAO().loadEquipment(player);
		ItemService.loadItemStones(equipment.getEquippedItemsWithoutStigma());
		if (ItemService.ensureSkillEnhance(player, equipment.getEquippedItemsWithoutStigma())) {
			equipment.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		equipment.setOwner(player);
		player.setEquipment(equipment);
		player.setEffectController(new PlayerEffectController(player));
		player.setFlyController(new FlyController(player));
		PlayerStatFunctions.addPredefinedStatFunctions(player);
		player.setQuestStateList(daos().playerQuestListDAO().load(player));
		player.setRecipeList(daos().playerRecipesDAO().load(player.getObjectId()));
		player.setSkillSkinList(daos().playerSkillSkinListDAO().loadSkillSkinList(playerObjId));

		Storage accWarehouse = account.getAccountWarehouse();
		player.setStorage(accWarehouse, StorageType.ACCOUNT_WAREHOUSE);
		Storage inventory = daos().inventoryDAO().loadStorage(playerObjId, StorageType.CUBE);
		ItemService.loadItemStones(inventory.getItems());
		if (ItemService.ensureSkillEnhance(player, inventory.getItems())) {
			inventory.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		player.setStorage(inventory, StorageType.CUBE);

		for (int petBagId = StorageType.PET_BAG_MIN; petBagId <= StorageType.PET_BAG_MAX; petBagId++) {
			Storage petBag = daos().inventoryDAO().loadStorage(playerObjId,
				StorageType.getStorageTypeById(petBagId));
			ItemService.loadItemStones(petBag.getItems());
			if (ItemService.ensureSkillEnhance(player, petBag.getItems())) {
				petBag.setPersistentState(PersistentState.UPDATE_REQUIRED);
			}
			player.setStorage(petBag, StorageType.getStorageTypeById(petBagId));
		}

		for (int houseWhId = StorageType.HOUSE_WH_MIN; houseWhId <= StorageType.HOUSE_WH_MAX; houseWhId++) {
			StorageType whType = StorageType.getStorageTypeById(houseWhId);
			if (whType != null) {
				Storage cabinet = daos().inventoryDAO().loadStorage(playerObjId,
					StorageType.getStorageTypeById(houseWhId));
				ItemService.loadItemStones(cabinet.getItems());
				if (ItemService.ensureSkillEnhance(player, cabinet.getItems())) {
					cabinet.setPersistentState(PersistentState.UPDATE_REQUIRED);
				}
				player.setStorage(cabinet, StorageType.getStorageTypeById(houseWhId));
			}
		}

		Storage warehouse = daos().inventoryDAO().loadStorage(playerObjId,
			StorageType.REGULAR_WAREHOUSE);
		ItemService.loadItemStones(warehouse.getItems());
		if (ItemService.ensureSkillEnhance(player, warehouse.getItems())) {
			warehouse.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}

		player.setStorage(warehouse, StorageType.REGULAR_WAREHOUSE);

		HouseRegistry houseRegistry = null;
		for (House house : player.getHouses()) {
			if (house.getStatus() == HouseStatus.ACTIVE || house.getStatus() == HouseStatus.SELL_WAIT) {
				houseRegistry = house.getRegistry();
				break;
			}
		}
		player.setHouseRegistry(houseRegistry);
		player.getEquipment().onLoadApplyEquipmentStats();
		daos().playerPunishmentsDAO().loadPlayerPunishments(player, PunishmentType.PRISON);
		daos().playerPunishmentsDAO().loadPlayerPunishments(player, PunishmentType.GATHER);
		player.getController().updatePassiveStats();
		daos().playerEffectsDAO().loadPlayerEffects(player);
		daos().playerCooldownsDAO().loadPlayerCooldowns(player);
		daos().itemCooldownsDAO().loadItemCooldowns(player);
		daos().portalCooldownsDAO().loadPortalCooldowns(player);
		daos().houseObjectCooldownsDAO().loadHouseObjectCooldowns(player);
		daos().playerBindPointDAO().loadBindPoint(player);
		daos().craftCooldownsDAO().loadCraftCooldowns(player);
		daos().playerLunaShopDAO().load(player);
		if (player.getCommonData().getBonusTitleId() > 0) {
			TitleChangeListener.onBonusTitleChange(player.getGameStats(), player.getCommonData().getBonusTitleId(), true);
		}
		daos().playerLifeStatsDAO().loadPlayerLifeStat(player);
		daos().playerEmotionListDAO().loadEmotions(player);
		if (CacheConfig.CACHE_PLAYERS) {
			playerCache.put(playerObjId, player);
		}
		return player;
	}

	/**
	 * 创建新角色对象。
	 * Creates a new player object.
	 *
	 * @param playerCommonData 玩家公共数据 / player common data
	 * @param playerAppearance 角色外观 / player appearance
	 * @param account          账号 / account
	 * @return 新玩家对象 / new player object
	 */
	public static Player newPlayer(PlayerCommonData playerCommonData, PlayerAppearance playerAppearance,
								   Account account) {
		PlayerInitialData playerInitialData = DataManager.PLAYER_INITIAL_DATA;
		LocationData ld = playerInitialData.getSpawnLocation(playerCommonData.getRace());
		WorldPosition position = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(ld.getMapId(), ld.getX(), ld.getY(), ld.getZ(),
			ld.getHeading(), 0);
		playerCommonData.setPosition(position);
		Player newPlayer = new Player(new PlayerController(), playerCommonData, playerAppearance, account);
		newPlayer.setSkillList(new PlayerSkillList());
		SkillLearnService.addNewSkills(newPlayer);
		PlayerCreationData playerCreationData = playerInitialData
			.getPlayerCreationData(playerCommonData.getPlayerClass());
		Storage playerInventory = new PlayerStorage(StorageType.CUBE);
		Storage regularWarehouse = new PlayerStorage(StorageType.REGULAR_WAREHOUSE);
		Storage accountWarehouse = new PlayerStorage(StorageType.ACCOUNT_WAREHOUSE);
		Equipment equipment = new Equipment(newPlayer);
		if (playerCreationData != null) {
			List<ItemType> items = playerCreationData.getItems();
			for (ItemType itemType : items) {
				int itemId = itemType.getTemplate().getTemplateId();
				Item item = ItemFactory.newItem(itemId, itemType.getCount());
				if (item == null) {
					continue;
				}
				ItemTemplate itemTemplate = item.getItemTemplate();
				if ((itemTemplate.isArmor() || itemTemplate.isWeapon())
					&& !(equipment.isSlotEquipped(itemTemplate.getItemSlot()))) {
					item.setEquipped(true);
					ItemSlot itemSlot = ItemSlot.getSlotFor(itemTemplate.getItemSlot());
					item.setEquipmentSlot(itemSlot.getSlotIdMask());
					equipment.onLoadHandler(item);
				} else {
					playerInventory.onLoadHandler(item);
				}
			}
		}
		newPlayer.setStorage(playerInventory, StorageType.CUBE);
		newPlayer.setStorage(regularWarehouse, StorageType.REGULAR_WAREHOUSE);
		newPlayer.setStorage(accountWarehouse, StorageType.ACCOUNT_WAREHOUSE);
		newPlayer.setEquipment(equipment);
		newPlayer.setMailbox(new Mailbox(newPlayer));
		for (int petBagId = StorageType.PET_BAG_MIN; petBagId <= StorageType.PET_BAG_MAX; petBagId++) {
			Storage petBag = new PlayerStorage(StorageType.getStorageTypeById(petBagId));
			newPlayer.setStorage(petBag, StorageType.getStorageTypeById(petBagId));
		}
		for (int houseWhId = StorageType.HOUSE_WH_MIN; houseWhId <= StorageType.HOUSE_WH_MAX; houseWhId++) {
			StorageType whType = StorageType.getStorageTypeById(houseWhId);
			if (whType != null) {
				Storage cabinet = new PlayerStorage(whType);
				newPlayer.setStorage(cabinet, StorageType.getStorageTypeById(houseWhId));
			}
		}

		playerInventory.setPersistentState(PersistentState.UPDATE_REQUIRED);
		equipment.setPersistentState(PersistentState.UPDATE_REQUIRED);
		return newPlayer;
	}

	/**
	 * 取消角色删除。
	 * Cancels the scheduled player deletion.
	 *
	 * @param accData 玩家账号数据 / player account data
	 * @return 是否已取消 / whether canceled
	 */
	public static boolean cancelPlayerDeletion(PlayerAccountData accData) {
		if (accData.getDeletionDate() == null) {
			return true;
		}
		if (accData.getDeletionDate().getTime() > System.currentTimeMillis()) {
			accData.setDeletionDate(null);
			storeDeletionTime(accData);
			return true;
		}
		return false;
	}

	/**
	 * 删除角色。
	 * Deletes a player character.
	 *
	 * @param accData 玩家账号数据 / player account data
	 */
	public static void deletePlayer(PlayerAccountData accData) {
		if (accData.getDeletionDate() != null) {
			return;
		}
		accData.setDeletionDate(new Timestamp(System.currentTimeMillis() + getCharacterDeletionDelayMillis()));
		storeDeletionTime(accData);
	}

	static long getCharacterDeletionDelayMillis() {
		return TimeUnit.MINUTES.toMillis(Math.max(0, GSConfig.CHARACTER_DELETE_DELAY_MINUTES));
	}

	/**
	 * 从数据库删除角色。
	 * Deletes a player from the database.
	 *
	 * @param playerId 角色 ID / player id
	 */
	public static void deletePlayerFromDB(int playerId) {
		daos().inventoryDAO().deletePlayerItems(playerId);
		daos().playerDAO().deletePlayer(playerId);
	}

	private static void storeDeletionTime(PlayerAccountData accData) {
		daos().playerDAO().updateDeletionTime(accData.getPlayerCommonData().getPlayerObjId(),
			accData.getDeletionDate());
	}

	/**
	 * 记录创建时间。
	 * Stores creation time.
	 * objectId
	 * creationDate
	 */
	public static void storeCreationTime(int objectId, Timestamp creationDate) {
		daos().playerDAO().storeCreationTime(objectId, creationDate);
	}

	/**
	 * 添加宏。
	 * Adds a macro.
	 * 玩家 / player
	 * macroOrder
	 * macroXML
	 */
	public static void addMacro(Player player, int macroOrder, String macroXML) {
		if (player.getMacroList().addMacro(macroOrder, macroXML)) {
			daos().playerMacrossesDAO().addMacro(player.getObjectId(), macroOrder, macroXML);
		} else {
			daos().playerMacrossesDAO().updateMacro(player.getObjectId(), macroOrder, macroXML);
		}
	}

	/**
	 * 移除宏。
	 * Removes a macro.
	 * 玩家 / player
	 * macroOrder
	 */
	public static void removeMacro(Player player, int macroOrder) {
		if (player.getMacroList().removeMacro(macroOrder)) {
			daos().playerMacrossesDAO().deleteMacro(player.getObjectId(), macroOrder);
		}
	}

	/**
	 * getCachedPlayer 方法。
	 * getCachedPlayer method.
	 * playerObjectId
	 * result
	 */
	public static Player getCachedPlayer(int playerObjectId) {
		return playerCache.get(playerObjectId);
	}

	/**
	 * getPlayerName 方法。
	 * getPlayerName method.
	 * objectId
	 * result
	 */
	public static String getPlayerName(Integer objectId) {
		return getPlayerNames(Collections.singleton(objectId)).get(objectId);
	}

	/**
	 * getPlayerNames 方法。
	 * getPlayerNames method.
	 *
	 * @param playerObjIds 玩家对象 ID 列表 / playerObjIds
	 *                     result
	 */
	public static Map<Integer, String> getPlayerNames(Collection<Integer> playerObjIds) {
		if (GenericValidator.isBlankOrNull(playerObjIds)) {
			return Collections.emptyMap();
		}
		final Map<Integer, String> result = Maps.newHashMap();
		final Set<Integer> playerObjIdsCopy = Sets.newHashSet(playerObjIds);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(object -> {
			if (playerObjIdsCopy.contains(object.getObjectId())) {
				result.put(object.getObjectId(), object.getName());
				playerObjIdsCopy.remove(object.getObjectId());
			}
		});
		result.putAll(daos().playerDAO().getPlayerNames(playerObjIdsCopy));
		return result;
	}
}
