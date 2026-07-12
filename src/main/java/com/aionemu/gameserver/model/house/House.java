package com.aionemu.gameserver.model.house;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.dao.HouseScriptsDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.SummonedHouseNpc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.model.templates.housing.Sale;
import com.aionemu.gameserver.model.templates.spawns.HouseSpawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnType;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 房屋模型。
 * House model.
 */
@Slf4j

public class House extends VisibleObject {
	private HousingLand land;
	private HouseAddress address;
	private Building building;
	private String name;
	private int playerObjectId;
	private Timestamp acquiredTime;
	private int permissions;
	private HouseStatus status;
	private boolean feePaid = true;
	private Timestamp nextPay;
	private Timestamp sellStarted;
	private Map<SpawnType, Npc> spawns = new HashMap<SpawnType, Npc>(3);
	private HouseRegistry houseRegistry;
	private byte houseOwnerInfoFlags = PlayerHouseOwnerFlags.SINGLE_HOUSE.getId();
	private PlayerScripts playerScripts;
	private PersistentState persistentState;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
	private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
	private ByteArrayOutputStream signNoticeStream;
	public static final int NOTICE_LENGTH = 130;

	public House(Building building, HouseAddress address, int instanceId) {
		this(GameWorldBootstrapServices.idFactory().nextId(), building, address, instanceId);
	}

	public House(int objectId, Building building, HouseAddress address, int instanceId) {
		super(objectId, new HouseController(), null, null, null);
		((HouseController) getController()).setOwner(this);
		this.address = address;
		this.building = building;
		this.name = "HOUSE_" + address.getId();
		setKnownlist(new PlayerAwareKnownList(this));
		setPersistentState(PersistentState.UPDATED);
		getRegistry();
	}

	/** 返回 controller / Returns the controller */
	@Override
	public HouseController getController() {
		return (HouseController) super.getController();
	}

	private void putDefaultParts() {
		for (PartType partType : PartType.values()) {
			Integer partId = building.getDefaultPartId(partType);
			if (partId == null) {
				continue;
			}
			for (int line = partType.getStartLineNr(); line <= partType.getEndLineNr(); line++) {
				int floor = partType.getEndLineNr() - line;
				HouseDecoration decor = new HouseDecoration(0, partId, floor);
				getRegistry().putDefaultPart(decor, floor);
			}
		}
	}

	/** 返回着陆 / Returns the land */
	public HousingLand getLand() {
		if (land == null) {
			for (HousingLand housingland : DataManager.HOUSE_DATA.getLands()) {
				for (HouseAddress houseAddress : housingland.getAddresses()) {
					if (this.getAddress().getId() == houseAddress.getId()) {
						this.land = housingland;
						break;
					}
				}
			}
		}
		return this.land;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 返回 address / Returns the address */
	public HouseAddress getAddress() {
		return address;
	}

	/** 返回建筑 / Returns the building*/
	public Building getBuilding() {
		return building;
	}

	/** 设置 building / Sets the building */
	public void setBuilding(Building building) {
		this.building = building;
	}

	/** 生成。 / Spawn. */
	public synchronized void spawn(int instanceId) {
		playerScripts = DAOManager.getDAO(HouseScriptsDAO.class).getPlayerScripts(getObjectId());
		if (playerObjectId > 0 && status == HouseStatus.ACTIVE || status == HouseStatus.SELL_WAIT) {
			DAOManager.getDAO(PlayerRegisteredItemsDAO.class).loadRegistry(playerObjectId);
		}
		fixBuildingStates();
		if (getPosition() == null || !getPosition().isSpawned()) {
			WorldPosition position = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(address.getMapId(), address.getX(),
					address.getY(), address.getZ(), (byte) 0, instanceId);
			this.setPosition(position);
			SpawnEngine.bringIntoWorld(this);
		}
		List<HouseSpawn> templates = DataManager.HOUSE_NPCS_DATA.getSpawnsByAddress(getAddress().getId());
		if (templates == null) {
			Collection<ZoneInstance> zones = GameWorldBootstrapServices.zoneService()
					.getZoneInstancesByWorldId(getAddress().getMapId()).values();
			String msg = null;
			for (ZoneInstance zone : zones) {
				if (zone.getZoneTemplate().getZoneType() != ZoneClassName.SUB
						|| zone.getZoneTemplate().getPriority() > 20) {
					continue;
				}
				if (zone.isInsideCordinate(getAddress().getX(), getAddress().getY(), getAddress().getZ())) {
					msg = "zone=" + zone.getZoneTemplate().getXmlName();
					break;
				}
			}
			if (msg == null) {
				msg = "address=" + this.getAddress().getId() + "; map=" + this.getAddress().getMapId();
			}
			msg += "; x=" + getAddress().getX() + ", y=" + getAddress().getY() + ", z=" + getAddress().getZ();
			log.warn(I18n.get("log.eecb48ab5c9d", msg));
			return;
		}
		int creatorId = getAddress().getId();
		String masterName = StringUtils.EMPTY;
		if (playerObjectId != 0) {
			ArrayList<Integer> players = new ArrayList<Integer>(1);
			players.add(playerObjectId);
			Map<Integer, String> playerNames = DAOManager.getDAO(PlayerDAO.class).getPlayerNames(players);
			if (playerNames.containsKey(playerObjectId)) {
				masterName = playerNames.get(playerObjectId);
			} else {
				this.revokeOwner();
			}
		}
		for (HouseSpawn spawn : templates) {
			SpawnTemplate t = null;
			if (spawn.getType() == SpawnType.MANAGER && spawns.get(SpawnType.MANAGER) == null) {
				t = SpawnEngine.addNewSingleTimeSpawn(getAddress().getMapId(), getLand().getManagerNpcId(),
						spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getH());
				SummonedHouseNpc npc = VisibleObjectSpawner.spawnHouseNpc(t, getPosition().getInstanceId(), this,
						masterName);
				spawns.put(SpawnType.MANAGER, npc);
			} else if (spawn.getType() == SpawnType.TELEPORT && spawns.get(SpawnType.TELEPORT) == null) {
				t = SpawnEngine.addNewSingleTimeSpawn(getAddress().getMapId(), getLand().getTeleportNpcId(),
						spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getH());
				SummonedHouseNpc npc = VisibleObjectSpawner.spawnHouseNpc(t, getPosition().getInstanceId(), this,
						masterName);
				spawns.put(SpawnType.TELEPORT, npc);
			} else if (spawn.getType() == SpawnType.SIGN && spawns.get(SpawnType.SIGN) == null) {
				t = SpawnEngine.addNewSingleTimeSpawn(getAddress().getMapId(), getCurrentSignNpcId(), spawn.getX(),
						spawn.getY(), spawn.getZ(), spawn.getH(), creatorId, StringUtils.EMPTY);
				spawns.put(SpawnType.SIGN, (Npc) SpawnEngine.spawnObject(t, getPosition().getInstanceId()));
			}
		}
	}

	/** 返回 visibility distance / Returns the visibility distance */
	@Override
	public float getVisibilityDistance() {
		return HousingConfig.VISIBILITY_DISTANCE;
	}

	/** 返回 max z visible distance / Returns the max z visible distance */
	@Override
	public float getMaxZVisibleDistance() {
		return HousingConfig.VISIBILITY_DISTANCE;
	}

	/** 返回所有者 ID / Returns the owner id */
	public int getOwnerId() {
		return playerObjectId;
	}

	/** 设置 owner id / Sets the owner id */
	public void setOwnerId(int playerObjectId) {
		if (this.playerObjectId != playerObjectId) {
			writeLock.lock();
			try {
				if (playerObjectId == 0) {
					signNoticeStream = null;
				} else {
					if (signNoticeStream == null) {
						signNoticeStream = new ByteArrayOutputStream(NOTICE_LENGTH);
					}
					signNoticeStream.reset();
					signNoticeStream.write(new byte[] { 0, 0 }, 0, 2);
				}
				this.playerObjectId = playerObjectId;
			} finally {
				writeLock.unlock();
			}
		}
		fixBuildingStates();
	}

	/** 返回 acquired time / Returns the acquired time */
	public Timestamp getAcquiredTime() {
		return acquiredTime;
	}

	/** 设置 acquired time / Sets the acquired time */
	public void setAcquiredTime(Timestamp acquiredTime) {
		this.acquiredTime = acquiredTime;
	}

	/** 返回 permissions / Returns the permissions */
	public int getPermissions() {
		if (playerObjectId == 0) {
			setDoorState(
					status == HouseStatus.SELL_WAIT ? HousePermissions.DOOR_OPENED_ALL : HousePermissions.DOOR_CLOSED);
			setNoticeState(HousePermissions.NOT_SET);
		} else {
			if (permissions == 0) {
				setNoticeState(HousePermissions.SHOW_OWNER);
				if (getBuilding().getType() == BuildingType.PERSONAL_FIELD) {
					setDoorState(HousePermissions.DOOR_CLOSED);
				}
			}
		}
		return permissions;
	}

	/** 设置 permissions / Sets the permissions */
	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}

	/** 返回门状态 / Returns the door state*/
	public HousePermissions getDoorState() {
		return HousePermissions.getDoorState(getPermissions());
	}

	/** 设置 door state / Sets the door state */
	public void setDoorState(HousePermissions doorState) {
		permissions = HousePermissions.setDoorState(permissions, doorState);
		if (isSpawned()) {
			GameWorldServices.geoService().setHouseDoorState(getWorldId(), getInstanceId(), address.getId(),
					doorState.isDoorOpen());
		}
	}

	/** 返回 notice state / Returns the notice state */
	public HousePermissions getNoticeState() {
		return HousePermissions.getNoticeState(getPermissions());
	}

	/** 设置 notice state / Sets the notice state */
	public void setNoticeState(HousePermissions noticeState) {
		permissions = HousePermissions.setNoticeState(permissions, noticeState);
	}

	/** 获取状态。 / Returns the status. */
	public HouseStatus getStatus() {
		return status;
	}

	/** 设置状态。 / Sets the status. */
	public synchronized void setStatus(HouseStatus status) {
		if (this.status != status) {
			if (this.playerObjectId == 0 && status == HouseStatus.ACTIVE) {
				status = HouseStatus.NOSALE;
			}
			this.status = status;
			fixBuildingStates();
			if ((status != HouseStatus.INACTIVE || getSellStarted() != null) && spawns.get(SpawnType.SIGN) != null) {
				Npc sign = spawns.get(SpawnType.SIGN);
				int oldNpcId = sign.getNpcId();
				int newNpcId = getCurrentSignNpcId();
				if (newNpcId != oldNpcId) {
					SpawnTemplate t = sign.getSpawn();
					sign.setSpawn(null);
					sign.getController().onDelete();
					t = SpawnEngine.addNewSingleTimeSpawn(t.getWorldId(), newNpcId, t.getX(), t.getY(), t.getZ(),
							t.getHeading());
					sign = (Npc) SpawnEngine.spawnObject(t, this.getPosition().getInstanceId());
					spawns.put(SpawnType.SIGN, sign);
				}
			}
		}
	}

	/**
	 * @return Whether fee paid
	 */
	public boolean isFeePaid() {
		return feePaid;
	}

	/** 设置 fee paid / Sets the fee paid */
	public void setFeePaid(boolean feePaid) {
		this.feePaid = feePaid;
	}

	/** 返回 next pay / Returns the next pay */
	public Timestamp getNextPay() {
		return nextPay;
	}

	/** 设置 next pay / Sets the next pay */
	public void setNextPay(Timestamp nextPay) {
		this.nextPay = nextPay;
	}

	/** 返回 sell started / Returns the sell started */
	public Timestamp getSellStarted() {
		return sellStarted;
	}

	/** 设置 sell started / Sets the sell started */
	public void setSellStarted(Timestamp sellStarted) {
		this.sellStarted = sellStarted;
	}

	/**
	 * @return 是否处于宽限期。 / Whether in grace period
	  */
	public boolean isInGracePeriod() {
		return playerObjectId > 0 && GameHousingServices.housingService().searchPlayerHouses(playerObjectId).size() == 2
				&& (status == HouseStatus.ACTIVE || status == HouseStatus.SELL_WAIT) && sellStarted != null
				&& sellStarted.getTime() <= GameHousingServices.housingBidService().getAuctionStartTime();
	}

	/** 返回 butler / Returns the butler */
	public synchronized Npc getButler() {
		return spawns.get(SpawnType.MANAGER);
	}

	/** 获取玩家种族。 / Returns the player race. */
	public Race getPlayerRace() {
		if (getButler() == null) {
			return Race.NONE;
		}
		if (getButler().getTribe() == TribeClass.GENERAL) {
			return Race.ELYOS;
		}
		return Race.ASMODIANS;
	}

	/** 获取传送。 / Returns the teleport. */
	public synchronized Npc getTeleport() {
		return spawns.get(SpawnType.TELEPORT);
	}

	/** 返回 current sign / Returns the current sign */
	public synchronized Npc getCurrentSign() {
		return spawns.get(SpawnType.SIGN);
	}

	/** 设置刷新点。 / Sets the spawn. */
	public synchronized void setSpawn(SpawnType type, Npc npc) {
		if (npc == null) {
			npc = spawns.remove(type);
			if (npc != null) {
				npc.getController().onDelete();
			}
		} else {
			spawns.put(type, npc);
		}
	}

	/** 返回 current sign npc id / Returns the current sign npc id */
	public int getCurrentSignNpcId() {
		int npcId = getLand().getWaitingSignNpcId();
		if (status == HouseStatus.NOSALE) {
			npcId = getLand().getNosaleSignNpcId();
		} else if (status == HouseStatus.SELL_WAIT) {
			if (GameHousingServices.housingBidService().isBiddingAllowed()) {
				npcId = getLand().getSaleSignNpcId();
			}
		} else if (playerObjectId != 0) {
			if (status == HouseStatus.ACTIVE) {
				npcId = getLand().getHomeSignNpcId();
			}
		}
		return npcId;
	}

	/** 撤销所有者 / revoke Owner. */
	public synchronized boolean revokeOwner() {
		if (playerObjectId == 0) {
			return false;
		}
		getRegistry().despawnObjects();
		if (this.getBuilding().getType() == BuildingType.PERSONAL_INS) {
			GameHousingServices.housingService().removeStudio(playerObjectId);
			DAOManager.getDAO(HousesDAO.class).deleteHouse(playerObjectId);
			return true;
		}
		houseRegistry = null;
		acquiredTime = null;
		sellStarted = null;
		nextPay = null;
		feePaid = true;
		Building defaultBuilding = getLand().getDefaultBuilding();
		setOwnerId(0);
		if (defaultBuilding != building) {
			GameHousingServices.housingService().switchHouseBuilding(this, defaultBuilding.getId());
		}
		setStatus(HouseStatus.NOSALE);
		save();
		return true;
	}

	/** 返回 registry / Returns the registry */
	public HouseRegistry getRegistry() {
		if (houseRegistry == null) {
			houseRegistry = new HouseRegistry(this);
			putDefaultParts();
		}
		return houseRegistry;
	}

	/** Reload 房屋 registry / Reload house registry */
	public synchronized void reloadHouseRegistry() {
		houseRegistry = null;
		getRegistry();
		if (playerObjectId != 0) {
			DAOManager.getDAO(PlayerRegisteredItemsDAO.class).loadRegistry(playerObjectId);
		}
	}

	/** 返回 render part / Returns the render part */
	public HouseDecoration getRenderPart(PartType partType, int floor) {
		return getRegistry().getRenderPart(partType, floor);
	}

	/** 返回 default part / Returns the default part */
	public HouseDecoration getDefaultPart(PartType partType, int floor) {
		return getRegistry().getDefaultPartByType(partType, floor);
	}

	/** 返回 player scripts / Returns the player scripts */
	public PlayerScripts getPlayerScripts() {
		return playerScripts;
	}

	/** 获取房屋类型。 / Returns the house type. */
	public HouseType getHouseType() {
		return HouseType.fromValue(getBuilding().getSize());
	}

	/** 保存。 / Save. */
	public synchronized void save() {
		DAOManager.getDAO(HousesDAO.class).storeHouse(this);
		if (houseRegistry != null) {
			this.houseRegistry.save();
		}
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		this.persistentState = persistentState;
	}

	/** 返回 house owner info flags / Returns the house owner info flags */
	public byte getHouseOwnerInfoFlags() {
		return houseOwnerInfoFlags;
	}

	/**
	 * @param status 是否处于指定房屋状态。 / Whether in housing status
	  */
	public boolean isInHousingStatus(PlayerHouseOwnerFlags status) {
		return (houseOwnerInfoFlags & status.getId()) != 0;
	}

	/** 修复建筑状态 / fix Building States. */
	public void fixBuildingStates() {
		houseOwnerInfoFlags = PlayerHouseOwnerFlags.SINGLE_HOUSE.getId();
		if (playerObjectId != 0) {
			houseOwnerInfoFlags |= PlayerHouseOwnerFlags.HAS_OWNER.getId();
			if (status == HouseStatus.ACTIVE) {
				houseOwnerInfoFlags |= PlayerHouseOwnerFlags.BIDDING_ALLOWED.getId();
				houseOwnerInfoFlags &= ~PlayerHouseOwnerFlags.SINGLE_HOUSE.getId();
			}
		} else if (status == HouseStatus.SELL_WAIT) {
			houseOwnerInfoFlags = PlayerHouseOwnerFlags.SELLING_HOUSE.getId();
		}
	}

	/** 返回 sign notice / Returns the sign notice */
	public byte[] getSignNotice() {
		byte[] notice;
		readLock.lock();
		if (signNoticeStream == null) {
			notice = new byte[0];
		} else {
			notice = signNoticeStream.toByteArray();
		}
		readLock.unlock();
		return notice;
	}

	/** 设置 sign notice / Sets the sign notice */
	public void setSignNotice(byte[] noticeStream) {
		writeLock.lock();
		if (signNoticeStream == null) {
			signNoticeStream = new ByteArrayOutputStream(NOTICE_LENGTH);
		}
		signNoticeStream.reset();
		try {
			signNoticeStream.write(noticeStream, 0, Math.min(noticeStream.length, NOTICE_LENGTH));
		} finally {
			writeLock.unlock();
		}
	}

	/** 返回 level restrict / Returns the level restrict */
	public int getLevelRestrict() {
		return land != null ? land.getSaleOptions().getMinLevel() : 10;
	}

	/** 返回 default auction price / Returns the default auction price */
	public final long getDefaultAuctionPrice() {
		Sale saleOptions = getLand().getSaleOptions();
		switch (getHouseType()) {
		case HOUSE:
			if (HousingConfig.HOUSE_MIN_BID > 0) {
				return HousingConfig.HOUSE_MIN_BID;
			}
			break;
		case MANSION:
			if (HousingConfig.MANSION_MIN_BID > 0) {
				return HousingConfig.MANSION_MIN_BID;
			}
			break;
		case ESTATE:
			if (HousingConfig.ESTATE_MIN_BID > 0) {
				return HousingConfig.ESTATE_MIN_BID;
			}
			break;
		case PALACE:
			if (HousingConfig.PALACE_MIN_BID > 0) {
				return HousingConfig.PALACE_MIN_BID;
			}
			break;
		default:
			break;
		}
		return saleOptions.getGoldPrice();
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return name;
	}
}
