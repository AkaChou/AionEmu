package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import java.util.concurrent.ConcurrentHashMap;
import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.model.templates.spawns.SpawnType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_ACQUIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OWNER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MARK_FRIENDLIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 房屋服务：加载/生成自定义房屋与工作室、管理所有权与外观，并在登录时同步房屋状态。
 * Housing service: loads/spawns custom houses and studios, manages ownership and appearance, and syncs house state on login.
 */
@Slf4j
public class HousingService {

	private static volatile ObjectProvider<HousingService> instanceProvider;
	/** 地图 ID 到该图房屋 / Map id to houses on that map */
	private static final Map<Integer, List<House>> housesByMapId = new HashMap<Integer, List<House>>();
	/** 自定义（地产）房屋缓存。 / Custom (estate) house cache. */
	private final Map<Integer, House> customHouses;
	/** 工作室缓存（玩家对象 ID → 工作室） / Studio cache (player objectId → studio) */
	private final Map<Integer, House> studios;

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final HousingService instance = new HousingService();
	}

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static HousingService getInstance() {
		ObjectProvider<HousingService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<HousingService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 从数据库加载自定义房屋与工作室。
	 * Loads custom houses and studios from the database.
	 */
	public HousingService() {
		log.info(I18n.get("log.f983a3184f1a"));
		customHouses = new ConcurrentHashMap<>(
				DAOManager.getDAO(HousesDAO.class).loadHouses(DataManager.HOUSE_DATA.getLands(), false));
		studios = new ConcurrentHashMap<>(
				DAOManager.getDAO(HousesDAO.class).loadHouses(DataManager.HOUSE_DATA.getLands(), true));
		log.info(I18n.get("log.2e7366eec746"));
	}

	/**
	 * 在指定世界/实例中生成房屋；若无地产数据则尝试生成玩家工作室。
	 * Spawns houses in the given world/instance; if no land data, tries to spawn a player studio.
	 *
	 * 世界 ID / world id
	 * instance id
	 * @param registeredId 注册玩家 ID（工作室场景） / registered player id (studio case)
	 */
	public void spawnHouses(int worldId, int instanceId, int registeredId) {
		Set<HousingLand> lands = DataManager.HOUSE_DATA.getLandsForWorldId(worldId);
		if (lands == null) {
			if (registeredId > 0) {
				House studio;
				synchronized (studios) {
					studio = studios.get(registeredId);
				}
				if (studio == null) {
					return;
				}
				HouseAddress addr = studio.getAddress();
				if (addr.getMapId() != worldId) {
					return;
				}
				VisibleObject existing = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(studio.getObjectId());
				WorldPosition position = null;
				if (existing != null) {
					position = existing.getPosition();
				}
				if (position == null) {
					position = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(addr.getMapId(), addr.getX(), addr.getY(),
							addr.getZ(), (byte) 0, instanceId);
					studio.setPosition(position);
				}
				if (!position.isSpawned()) {
					SpawnEngine.bringIntoWorld(studio);
				}
				studio.spawn(instanceId);
				Player enteredPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(registeredId);
				if (enteredPlayer != null) {
					enteredPlayer.setHouseRegistry(studio.getRegistry());
				}
			}
			return;
		}

		int spawnedCounter = 0;
		for (HousingLand land : lands) {
			Building defaultBuilding = land.getDefaultBuilding();
			if (defaultBuilding.getType() == BuildingType.PERSONAL_INS) {
				continue;
			}
			for (HouseAddress address : land.getAddresses()) {
				if (address.getMapId() != worldId) {
					continue;
				}
				House customHouse = customHouses.get(address.getId());
				if (customHouse == null) {
					customHouse = new House(defaultBuilding, address, instanceId);
					customHouse.setPersistentState(PersistentState.NEW);
				}
				customHouse.spawn(instanceId);
				spawnedCounter++;
				List<House> housesForMap = housesByMapId.get(worldId);
				if (housesForMap == null) {
					housesForMap = new ArrayList<House>();
					housesByMapId.put(worldId, housesForMap);
				}
				housesForMap.add(customHouse);
			}
		}
		if (spawnedCounter > 0) {
			log.info(I18n.get("log.76ad8f8cf6ea", worldId, instanceId, spawnedCounter));
		}
	}

	/**
	 * 查询玩家拥有的房屋（工作室优先）。
	 * Lists houses owned by the player (studio first if present).
	 *
	 * player object id
	 * house list
	 */
	public List<House> searchPlayerHouses(int playerObjId) {
		List<House> houses = new ArrayList<House>();
		synchronized (studios) {
			if (studios.containsKey(playerObjId)) {
				houses.add(studios.get(playerObjId));
				return houses;
			}
		}
		for (House house : customHouses.values()) {
			if (house.getOwnerId() == playerObjId) {
				houses.add(house);
			}
		}
		return houses;
	}

	/**
	 * 获取玩家当前房屋地址 ID（工作室或有效自定义房屋）。
	 * Returns the player's current house address id (studio or active custom house).
	 *
	 * player id
	 *
	 * @param playerId @return 地址 ID，无则为 0 / address id, or 0
	 */
	public int getPlayerAddress(int playerId) {
		synchronized (studios) {
			if (studios.containsKey(playerId)) {
				return studios.get(playerId).getAddress().getId();
			}
		}
		for (House house : customHouses.values()) {
			if (house.getStatus() == HouseStatus.INACTIVE) {
				continue;
			}
			if (house.getOwnerId() == playerId
					&& (house.getStatus() == HouseStatus.ACTIVE || house.getStatus() == HouseStatus.SELL_WAIT)) {
				return house.getAddress().getId();
			}
		}
		return 0;
	}

	/**
	 * 重置房屋自定义装饰外观。
	 * Resets custom decorative parts of the house.
	 *
	 * @param house 目标房屋 / target house
	 */
	public void resetAppearance(House house) {
		List<HouseDecoration> customParts = house.getRegistry().getCustomParts();
		for (HouseDecoration deco : customParts) {
			deco.setPersistentState(PersistentState.DELETED);
		}
		for (HouseDecoration deco : customParts) {
			house.getRegistry().removeCustomPart(deco.getObjectId());
		}
	}

	/**
	 * 按名称查找自定义房屋。
	 * Finds a custom house by name.
	 *
	 * house name
	 *
	 * @param houseName @return 房屋，未找到则为 null / house, or null
	 */
	public House getHouseByName(String houseName) {
		for (House house : customHouses.values()) {
			if (house.getName().equals(houseName)) {
				return house;
			}
		}
		return null;
	}

	/**
	 * 按地址 ID 查找自定义房屋。
	 * Finds a custom house by address id.
	 *
	 * address id
	 *
	 * @param address @return 房屋，未找到则为 null / house, or null
	 */
	public House getHouseByAddress(int address) {
		for (House house : customHouses.values()) {
			if (house.getAddress().getId() == address) {
				return house;
			}
		}
		return null;
	}

	/**
	 * 激活玩家已购但处于未激活状态的房屋。
	 * Activates a house the player bought that is still inactive.
	 *
	 * player id
	 *
	 * @param playerId @return 激活后的房屋，不存在则为 null / activated house, or null
	 */
	public House activateBoughtHouse(int playerId) {
		for (House house : customHouses.values()) {
			if (house.getOwnerId() == playerId && house.getStatus() == HouseStatus.INACTIVE) {
				house.revokeOwner();
				house.setOwnerId(playerId);
				house.setFeePaid(true);
				house.setNextPay(null);
				house.setSellStarted(null);
				house.reloadHouseRegistry();
				house.save();
				return house;
			}
		}
		return null;
	}

	/**
	 * 获取玩家工作室。
	 * Returns the player's studio house.
	 *
	 * player id
	 *
	 * @param playerId @return 工作室，不存在则为 null / studio, or null
	 */
	public House getPlayerStudio(int playerId) {
		synchronized (studios) {
			if (studios.containsKey(playerId))
				return studios.get(playerId);
		}
		return null;
	}

	/**
	 * 移除玩家工作室缓存。
	 * Removes the player's studio from cache.
	 *
	 * player id
	 */
	public void removeStudio(int playerId) {
		if (playerId != 0) {
			synchronized (studios) {
				studios.remove(playerId);
			}
		}
	}

	/**
	 * 为玩家注册（创建）工作室。
	 * Registers (creates) a studio for the player.
	 *
	 * @param player 玩家 / player
	 */
	public void registerPlayerStudio(Player player) {
		createStudio(player);
	}

	/**
	 * 扣费后重新创建玩家工作室。
	 * Recreates the player's studio after charging the land fee.
	 *
	 * @param player 玩家 / player
	 */
	public void recreatePlayerStudio(Player player) {
		HousingLand land = DataManager.HOUSE_DATA.getLand(329001);
		final long fee = land.getSaleOptions().getGoldPrice();
		if (player.getInventory().getKinah() < fee) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NOT_ENOUGH_MONEY);
			return;
		}
		createStudio(player);
		player.getInventory().decreaseKinah(fee);
	}

	/**
	 * 创建并激活玩家工作室。
	 * Creates and activates a player studio.
	 *
	 * @param player 玩家 / player
	 */
	private void createStudio(Player player) {
		if (!searchPlayerHouses(player.getObjectId()).isEmpty()) { // should not happen
			return;
		}
		HousingLand land = DataManager.HOUSE_DATA.getLand(player.getRace() == Race.ELYOS ? 329001 : 339001);
		House studio = new House(land.getDefaultBuilding(), land.getAddresses().get(0), 0);
		studio.setOwnerId(player.getObjectId());
		synchronized (studios) {
			studios.put(player.getObjectId(), studio);
		}
		studio.setStatus(HouseStatus.ACTIVE);
		studio.setAcquiredTime(new Timestamp(System.currentTimeMillis()));
		studio.setFeePaid(true);
		studio.setNextPay(null);
		studio.setPersistentState(PersistentState.NEW);
		player.setBuildingOwnerState(PlayerHouseOwnerFlags.HOUSE_OWNER.getId());
		PacketSendUtility.sendPacket(player,
				new SM_HOUSE_ACQUIRE(player.getObjectId(), studio.getAddress().getId(), true));
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_INS_OWN_SUCCESS);
		PacketSendUtility.sendPacket(player, new SM_HOUSE_OWNER_INFO(player, studio));
	}

	/**
	 * 切换房屋建筑模板并刷新外观与物件。
	 * Switches the house building template and refreshes appearance/objects.
	 *
	 * current house
	 * @param newBuildingId 新建筑模板 ID / new building template id
	 */
	public void switchHouseBuilding(House currentHouse, int newBuildingId) {
		Building otherBuilding = DataManager.HOUSE_BUILDING_DATA.getBuilding(newBuildingId);
		currentHouse.setBuilding(otherBuilding);
		// currentHouse.getRegistry().despawnObjects(false);
		currentHouse.getRegistry().save();
		currentHouse.reloadHouseRegistry(); // load new defaults
		DAOManager.getDAO(HousesDAO.class).storeHouse(currentHouse);
		HouseController controller = ((HouseController) currentHouse.getController());
		controller.broadcastAppearance();
		controller.spawnObjects();
	}

	/**
	 * 汇总所有已按地图登记的自定义房屋。
	 * Collects all custom houses registered by map.
	 *
	 * @return 自定义房屋列表 / custom house list
	 */
	public List<House> getCustomHouses() {
		List<House> houses = new ArrayList<House>();
		for (List<House> mapHouses : housesByMapId.values()) {
			houses.addAll(mapHouses);
		}
		return houses;
	}

	/**
	 * 实例销毁时清理工作室生成点并保存。
	 * Clears studio spawn points and saves when an instance is destroyed.
	 *
	 * @param ownerId 所有者玩家 ID / owner player id
	 */
	public void onInstanceDestroy(int ownerId) {
		House studio;
		synchronized (studios) {
			studio = studios.get(ownerId);
		}
		if (studio != null) {
			studio.setSpawn(SpawnType.MANAGER, null);
			studio.setSpawn(SpawnType.TELEPORT, null);
			studio.setSpawn(SpawnType.SIGN, null);
			studio.save();
		}
	}

	/**
	 * 玩家登录时同步房屋所有者状态与相关数据包。
	 * On login, syncs house-owner flags and related packets.
	 *
	 * logging-in player
	 */
	public void onPlayerLogin(Player player) {
		House activeHouse = null;
		byte buildingState = PlayerHouseOwnerFlags.BUY_STUDIO_ALLOWED.getId();
		for (House house : player.getHouses()) {
			if (house.getStatus() == HouseStatus.ACTIVE || house.getStatus() == HouseStatus.SELL_WAIT) {
				activeHouse = house;
			}
		}
		if (activeHouse == null) {
			QuestState qs;
			qs = player.getQuestStateList().getQuestState(player.getRace() == Race.ELYOS ? 18802 : 28802);
			if (qs != null && qs.getStatus().equals(QuestStatus.COMPLETE)) {
				buildingState |= PlayerHouseOwnerFlags.BIDDING_ALLOWED.getId();
			}
		} else {
			if (activeHouse.getStatus() == HouseStatus.SELL_WAIT) {
				buildingState = PlayerHouseOwnerFlags.SELLING_HOUSE.getId();
			} else {
				buildingState = PlayerHouseOwnerFlags.HOUSE_OWNER.getId();
			}
		}
		player.setBuildingOwnerState(buildingState);
		PacketSendUtility.sendPacket(player, new SM_HOUSE_OWNER_INFO(player, activeHouse));
		if (!player.getFriendList().getIsFriendListSent()) {
			PacketSendUtility.sendPacket(player, new SM_FRIEND_LIST());
		}
		PacketSendUtility.sendPacket(player, new SM_MARK_FRIENDLIST());
	}
}
