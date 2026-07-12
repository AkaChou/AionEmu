package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.configs.schedule.SiegeSchedule;
import com.aionemu.gameserver.configs.schedule.SiegeSchedule.Fortress;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;
import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_ARTIFACT_INFO3;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORTRESS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORTRESS_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INFLUENCE_RATIO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RIFT_ANNOUNCE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHIELD_EFFECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SIEGE_LOCATION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.siegeservice.ArtifactSiege;
import com.aionemu.gameserver.services.siegeservice.FortressSiege;
import com.aionemu.gameserver.services.siegeservice.Siege;
import com.aionemu.gameserver.services.siegeservice.SiegeAutoRace;
import com.aionemu.gameserver.services.siegeservice.SiegeException;
import com.aionemu.gameserver.services.siegeservice.SiegeStartRunnable;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldType;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 攻城战服务，管理要塞/神器攻城、刷怪、计划与状态广播。
 * Siege war service managing fortress/artifact sieges, spawns, schedules, and status broadcasts.
 */
@Slf4j(topic = "SIEGE_LOG")

public class SiegeService {

	private static final String SIEGE_LOCATION_STATUS_BROADCAST_SCHEDULE = "0 0 * ? * *";
	private static volatile ObjectProvider<SiegeService> instanceProvider;
	private static final SiegeService instance = new SiegeService();
	private final ConcurrentMap<Integer, Siege<?>> activeSieges = new ConcurrentHashMap<Integer, Siege<?>>();
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private SiegeSchedule siegeSchedule;
	private Map<Integer, ArtifactLocation> artifacts;
	private Map<Integer, FortressLocation> fortresses;
	private Map<Integer, SiegeLocation> locations;

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static SiegeService getInstance() {
		ObjectProvider<SiegeService> provider = instanceProvider;
		if (provider == null) {
			return instance;
		}
		return provider.getIfAvailable(() -> instance);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SiegeService> instanceProvider) {
		SiegeService.instanceProvider = instanceProvider;
	}

	/**
	 * 从数据与数据库加载要塞/神器地点。
	 * Loads fortress/artifact locations from data and database.
	 */
	public void initSiegeLocations() {
		if (SiegeConfig.SIEGE_ENABLED) {
			if (siegeSchedule != null) {
				log.error(I18n.get("log.e4c1be66641b"));
				return;
			}
			artifacts = DataManager.SIEGE_LOCATION_DATA.getArtifacts();
			fortresses = DataManager.SIEGE_LOCATION_DATA.getFortress();
			locations = DataManager.SIEGE_LOCATION_DATA.getSiegeLocations();
			DAOManager.getDAO(SiegeDAO.class).loadSiegeLocations(locations);
			log.info(I18n.get("log.536ce47b586f", locations.size()));
		} else {
			artifacts = Collections.emptyMap();
			fortresses = Collections.emptyMap();
			locations = Collections.emptyMap();
			log.info(I18n.get("log.9929433ec536"));
		}
	}

	@SuppressWarnings("deprecation")
	/**
	 * 初始化攻城：刷和平 NPC、注册计划并启动独立神器攻城。
	 * Initializes sieges: peace NPCs, schedules, and standalone artifact sieges.
	 */
	public void initSieges() {
		if (!SiegeConfig.SIEGE_ENABLED) {
			return;
		}
		log.info(I18n.get("log.5fc5ca1430d6"));

		for (Integer i : getSiegeLocations().keySet()) {
			deSpawnNpcs(i);
		}
		for (FortressLocation f : getFortresses().values()) {
			spawnNpcs(f.getLocationId(), f.getRace(), SiegeModType.PEACE);
		}
		for (ArtifactLocation a : getStandaloneArtifacts().values()) {
			spawnNpcs(a.getLocationId(), a.getRace(), SiegeModType.PEACE);
		}
		reloadSchedule();
		for (ArtifactLocation artifact : artifacts.values()) {
			if (artifact.isStandAlone()) {
				log.debug("Starting siege of artifact #" + artifact.getLocationId());
				startSiege(artifact.getLocationId());
			} else {
				log.debug("Artifact #" + artifact.getLocationId() + " siege was not started, it belongs to fortress");
			}
		}
		updateFortressNextState();
		GameCronServices.cronService().schedule(new Runnable() {
			@Override
			public void run() {
				updateFortressNextState();
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					public void visit(Player player) {
						for (FortressLocation fortress : getFortresses().values()) {
							PacketSendUtility.sendPacket(player, new SM_FORTRESS_INFO(fortress.getLocationId(), false));
						}
						PacketSendUtility.sendPacket(player, new SM_FORTRESS_STATUS());
						for (FortressLocation fortress : getFortresses().values()) {
							PacketSendUtility.sendPacket(player, new SM_FORTRESS_INFO(fortress.getLocationId(), true));
						}
					}
				});
			}
		}, SIEGE_LOCATION_STATUS_BROADCAST_SCHEDULE);
	}

	/**
	 * 重载攻城 Cron 计划（先取消旧任务）。
	 * Reloads the siege cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		SiegeSchedule newSchedule = SiegeConfig.SIEGE_ENABLED ? SiegeSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		siegeSchedule = newSchedule;
		if (siegeSchedule != null) {
			for (Fortress fortress : siegeSchedule.getFortressesList()) {
				for (String siegeTime : fortress.getSiegeTimes()) {
					Runnable task = new SiegeStartRunnable(fortress.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, siegeTime);
				}
			}
		}
	}

	/**
	 * 检查并启动指定地点攻城（含自动种族逻辑）。
	 * Checks and starts siege for the location (including auto-race logic).
	 *
	 * location id
	 */
	public void checkSiegeStart(int locationId) {
		if (SiegeConfig.SIEGE_AUTO_RACE && SiegeAutoRace.isAutoSiege(locationId)) {
			SiegeAutoRace.AutoSiegeRace(locationId);
		} else {
			startSiege(locationId);
		}
	}

	/**
	 * 启动指定地点的攻城战。
	 * Starts the siege for the given location id.
	 *
	 * siege location id
	 */
	public void startSiege(final int siegeLocationId) {
		Siege<?> siege = newSiege(siegeLocationId);
		if (activeSieges.putIfAbsent(siegeLocationId, siege) != null) {
			return;
		}
		siege.startSiege();
		if (siege.isEndless()) {
			return;
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopSiege(siegeLocationId);
			}
		}, siege.getSiegeLocation().getSiegeDuration() * 1000);
	}

	/**
	 * 停止指定地点的攻城战。
	 * Stops the siege for the given location id.
	 *
	 * siege location id
	 */
	public void stopSiege(int siegeLocationId) {
		log.debug("Stopping siege of siege location: " + siegeLocationId);
		Siege<?> siege = activeSieges.remove(siegeLocationId);
		if (siege == null) {
			log.debug("Siege of siege location " + siegeLocationId + " is not in progress, it was captured earlier?");
			return;
		}
		if (siege.isFinished()) {
			return;
		}
		siege.stopSiege();
	}

	protected void updateFortressNextState() {
		Calendar currentHourPlus1 = Calendar.getInstance();
		currentHourPlus1.set(Calendar.MINUTE, 0);
		currentHourPlus1.set(Calendar.SECOND, 0);
		currentHourPlus1.set(Calendar.MILLISECOND, 0);
		currentHourPlus1.add(Calendar.HOUR, 1);
		Map<Runnable, JobDetail> siegeStartRunables = GameCronServices.cronService().getRunnables();
		siegeStartRunables = Maps.filterKeys(siegeStartRunables, new Predicate<Runnable>() {
			@Override
			public boolean apply(Runnable runnable) {
				return (runnable instanceof SiegeStartRunnable);
			}
		});
		Map<Integer, List<Trigger>> siegeIdToStartTriggers = Maps.newHashMap();
		for (Map.Entry<Runnable, JobDetail> entry : siegeStartRunables.entrySet()) {
			SiegeStartRunnable fssr = (SiegeStartRunnable) entry.getKey();
			List<Trigger> storage = siegeIdToStartTriggers.get(fssr.getLocationId());
			if (storage == null) {
				storage = Lists.newArrayList();
				siegeIdToStartTriggers.put(fssr.getLocationId(), storage);
			}
			storage.addAll(GameCronServices.cronService().getJobTriggers(entry.getValue()));
		}
		for (Map.Entry<Integer, List<Trigger>> entry : siegeIdToStartTriggers.entrySet()) {
			List<Date> nextFireDates = Lists.newArrayListWithCapacity(entry.getValue().size());
			for (Trigger trigger : entry.getValue()) {
				nextFireDates.add(trigger.getNextFireTime());
			}
			Collections.sort(nextFireDates);
			Date nextSiegeDate = nextFireDates.get(0);
			Calendar siegeStartHour = Calendar.getInstance();
			siegeStartHour.setTime(nextSiegeDate);
			siegeStartHour.set(Calendar.MINUTE, 0);
			siegeStartHour.set(Calendar.SECOND, 0);
			siegeStartHour.set(Calendar.MILLISECOND, 0);
			SiegeLocation fortress = getSiegeLocation(entry.getKey());
			Calendar siegeCalendar = Calendar.getInstance();
			siegeCalendar.set(Calendar.MINUTE, 0);
			siegeCalendar.set(Calendar.SECOND, 0);
			siegeCalendar.set(Calendar.MILLISECOND, 0);
			siegeCalendar.add(Calendar.HOUR, 0);
			siegeCalendar.add(Calendar.SECOND, getRemainingSiegeTimeInSeconds(fortress.getLocationId()));
			if (SiegeConfig.SIEGE_AUTO_RACE && SiegeAutoRace.isAutoSiege(fortress.getLocationId())) {
				siegeStartHour.add(Calendar.HOUR, 1);
			}
			if (currentHourPlus1.getTimeInMillis() == siegeStartHour.getTimeInMillis()
					|| siegeCalendar.getTimeInMillis() > currentHourPlus1.getTimeInMillis()) {
				fortress.setNextState(1);
			} else {
				fortress.setNextState(0);
			}
		}
	}

	/**
	 * 获取距本小时结束的剩余秒数。
	 * Returns seconds remaining until the end of the current hour.
	 *
	 * @return 剩余秒数 / remaining seconds
	 */
	public int getSecondsBeforeHourEnd() {
		Calendar c = Calendar.getInstance();
		int minutesAsSeconds = c.get(Calendar.MINUTE) * 60;
		int seconds = c.get(Calendar.SECOND);
		return 3600 - (minutesAsSeconds + seconds);
	}

	/**
	 * 获取指定攻城剩余时间（秒）。
	 * Returns remaining siege time in seconds for the location.
	 *
	 * siege location id
	 *
	 * @param siegeLocationId 剩余秒数 / remaining seconds
	 */
	public int getRemainingSiegeTimeInSeconds(int siegeLocationId) {
		Siege<?> siege = getSiege(siegeLocationId);
		if (siege == null || siege.isFinished()) {
			return 0;
		}
		if (!siege.isStarted()) {
			return siege.getSiegeLocation().getSiegeDuration();
		}
		if (siege.getSiegeLocation().getSiegeDuration() == -1) {
			return -1;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, siege.getSiegeLocation().getSiegeDuration());
		int result = (int) ((calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000);
		return result > 0 ? result : 0;
	}

	/**
	 * 按地点对象获取进行中的攻城实例。
	 * Returns the active siege instance for the location object.
	 *
	 * @param loc 攻城地点 / siege location
	 * siege instance
	 */
	public Siege<?> getSiege(SiegeLocation loc) {
		return activeSieges.get(loc.getLocationId());
	}

	/**
	 * 按 ID 获取进行中的攻城实例。
	 * Returns the active siege instance by location id.
	 *
	 * siege location id
	 * siege instance
	 */
	public Siege<?> getSiege(Integer siegeLocationId) {
		return activeSieges.get(siegeLocationId);
	}

	/**
	 * 判断指定要塞是否处于攻城中。
	 * Checks whether a siege is in progress for the fortress id.
	 *
	 * fortress id
	 *
	 * @param fortressId
	 * @return 是否攻城中 / whether in progress
	 */
	public boolean isSiegeInProgress(int fortressId) {
		return activeSieges.containsKey(fortressId);
	}

	/**
	 * 获取全部要塞地点。
	 * Returns all fortress locations.
	 *
	 * fortresses map
	 */
	public Map<Integer, FortressLocation> getFortresses() {
		return fortresses;
	}

	/**
	 * 按 ID 获取要塞地点。
	 * Returns the fortress location by id.
	 *
	 * fortress id
	 * fortress
	 */
	public FortressLocation getFortress(int fortressId) {
		return fortresses.get(fortressId);
	}

	/**
	 * 获取全部神器地点。
	 * Returns all artifact locations.
	 *
	 * artifacts map
	 */
	public Map<Integer, ArtifactLocation> getArtifacts() {
		return artifacts;
	}

	/**
	 * 按 ID 获取神器地点。
	 * Returns the artifact location by id.
	 *
	 * @param id 神器 ID / artifact id
	 * artifact
	 */
	public ArtifactLocation getArtifact(int id) {
		return getArtifacts().get(id);
	}

	/**
	 * 获取独立（非隶属要塞）神器地点。
	 * Returns standalone (non-fortress-bound) artifact locations.
	 *
	 * @return 独立神器映射 / standalone artifacts map
	 */
	public Map<Integer, ArtifactLocation> getStandaloneArtifacts() {
		return Maps.filterValues(artifacts, new Predicate<ArtifactLocation>() {
			@Override
			public boolean apply(ArtifactLocation input) {
				return input != null && input.isStandAlone();
			}
		});
	}

	/**
	 * 获取隶属要塞的神器地点。
	 * Returns fortress-bound artifact locations.
	 *
	 * @return 要塞神器映射 / fortress artifacts map
	 */
	public Map<Integer, ArtifactLocation> getFortressArtifacts() {
		return Maps.filterValues(artifacts, new Predicate<ArtifactLocation>() {
			@Override
			public boolean apply(ArtifactLocation input) {
				return input != null && input.getOwningFortress() != null;
			}
		});
	}

	/**
	 * 获取全部攻城地点。
	 * Returns all siege locations.
	 *
	 * locations map
	 */
	public Map<Integer, SiegeLocation> getSiegeLocations() {
		return locations;
	}

	/**
	 * 按 ID 获取攻城地点。
	 * Returns the siege location by id.
	 *
	 * location id
	 * location
	 */
	public SiegeLocation getSiegeLocation(int locationId) {
		return locations.get(locationId);
	}

	/**
	 * 获取指定世界内的攻城地点。
	 * Returns siege locations within the given world.
	 *
	 * 世界 ID / world id
	 * locations map
	 */
	public Map<Integer, SiegeLocation> getSiegeLocations(int worldId) {
		Map<Integer, SiegeLocation> mapLocations = new HashMap<Integer, SiegeLocation>();
		for (SiegeLocation location : getSiegeLocations().values()) {
			if (location.getWorldId() == worldId) {
				mapLocations.put(location.getLocationId(), location);
			}
		}
		return mapLocations;
	}

	protected Siege<?> newSiege(int siegeLocationId) {
		if (fortresses.containsKey(siegeLocationId)) {
			return new FortressSiege(fortresses.get(siegeLocationId));
		}
		if (artifacts.containsKey(siegeLocationId)) {
			return new ArtifactSiege(artifacts.get(siegeLocationId));
		}
		throw new SiegeException("Unknown siege handler for siege location: " + siegeLocationId);
	}

	/**
	 * 清理指定军团在攻城地点上的占领关联。
	 * Cleans legion ownership links from siege locations.
	 *
	 * legion id
	 */
	public void cleanLegionId(int legionId) {
		for (SiegeLocation loc : this.getSiegeLocations().values()) {
			if (loc.getLegionId() == legionId) {
				loc.setLegionId(0);
				break;
			}
		}
	}

	/**
	 * 按种族与模式刷出攻城 NPC。
	 * Spawns siege NPCs for race and siege mode type.
	 *
	 * location id
	 * 阵营 / race
	 * @param type 攻城模式 / siege mode type
	 */
	public void spawnNpcs(int siegeLocationId, SiegeRace race, SiegeModType type) {
		List<SpawnGroup2> siegeSpawns = DataManager.SPAWNS_DATA2.getSiegeSpawnsByLocId(siegeLocationId);
		for (SpawnGroup2 group : siegeSpawns) {
			for (SpawnTemplate template : group.getSpawnTemplates()) {
				SiegeSpawnTemplate siegetemplate = (SiegeSpawnTemplate) template;
				if (siegetemplate.getSiegeRace().equals(race) && siegetemplate.getSiegeModType().equals(type)) {
					SpawnEngine.spawnObject(siegetemplate, 1);
				}
			}
		}
	}

	/**
	 * 删除指定地点的攻城 NPC。
	 * Despawns siege NPCs for the location.
	 *
	 * location id
	 */
	public void deSpawnNpcs(int siegeLocationId) {
		Collection<SiegeNpc> siegeNpcs = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getLocalSiegeNpcs(siegeLocationId);
		for (SiegeNpc npc : new ArrayList<SiegeNpc>(siegeNpcs)) {
			npc.getController().onDelete();
		}
	}

	/**
	 * 判断 NPC 是否属于进行中的攻城。
	 * Checks whether the NPC belongs to an active siege.
	 *
	 * @param npc NPC
	 *
	 * @param npc
	 * @return 是否属于活动攻城 / whether in active siege
	 */
	public boolean isSiegeNpcInActiveSiege(Npc npc) {
		if ((npc instanceof SiegeNpc)) {
			FortressLocation fort = getFortress(((SiegeNpc) npc).getSiegeId());
			if (fort != null) {
				if (fort.isVulnerable()) {
					return true;
				}
				if (fort.getNextState() == 1) {
					return npc.getSpawn().getRespawnTime() >= getSecondsBeforeHourEnd();
				}
			}
		}
		return false;
	}

	/**
	 * 向全服广播攻城状态更新。
	 * Broadcasts siege status update to all players.
	 */
	public void broadcastUpdate() {
		broadcast(new SM_SIEGE_LOCATION_INFO(), null);
	}

	/**
	 * 广播单个地点的攻城状态更新。
	 * Broadcasts siege status update for a single location.
	 *
	 * location
	 */
	public void broadcastUpdate(SiegeLocation loc) {
		GameRuntimeServices.influence().recalculateInfluence();
		broadcast(new SM_SIEGE_LOCATION_INFO(loc), new SM_INFLUENCE_RATIO());
	}

	/**
	 * 向全服发送两份攻城相关数据包。
	 * Sends two siege-related packets to all players.
	 *
	 * packet 1
	 * packet 2
	 */
	public void broadcast(final AionServerPacket pkt1, final AionServerPacket pkt2) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			public void visit(Player player) {
				if (pkt1 != null) {
					PacketSendUtility.sendPacket(player, pkt1);
				}
				if (pkt2 != null) {
					PacketSendUtility.sendPacket(player, pkt2);
				}
			}
		});
	}

	/**
	 * 广播带名称的攻城状态更新。
	 * Broadcasts siege status update with a display name.
	 *
	 * location
	 * name description id
	 */
	public void broadcastUpdate(SiegeLocation loc, DescriptionId nameId) {
		SM_SIEGE_LOCATION_INFO pkt = new SM_SIEGE_LOCATION_INFO(loc);
		SM_SYSTEM_MESSAGE info = loc.getLegionId() == 0
				? new SM_SYSTEM_MESSAGE(1404542, loc.getRace().getDescriptionId(), nameId)
				: new SM_SYSTEM_MESSAGE(1301038,
						GameCoreGameplayServices.legionService().getLegion(loc.getLegionId()).getLegionName(), nameId);
		broadcast(pkt, info, loc.getRace());
	}

	private void broadcast(final AionServerPacket pkt, final AionServerPacket info, final SiegeRace race) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			public void visit(Player player) {
				if (player.getRace().getRaceId() == race.getRaceId()) {
					PacketSendUtility.sendPacket(player, info);
				}
				PacketSendUtility.sendPacket(player, pkt);
			}
		});
	}

	private void broadcast(final SM_RIFT_ANNOUNCE rift, final SM_SYSTEM_MESSAGE info) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, rift);
				if (info != null && player.getWorldType().equals(WorldType.BALAUREA)
						|| info != null && player.getWorldType().equals(WorldType.PANESTERRA)) {
					PacketSendUtility.sendPacket(player, info);
				}
			}
		});
	}

	/**
	 * 玩家登录时下发攻城相关信息。
	 * Sends siege-related info when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onPlayerLogin(Player player) {
		if (SiegeConfig.SIEGE_ENABLED) {
			PacketSendUtility.sendPacket(player, new SM_INFLUENCE_RATIO());
			PacketSendUtility.sendPacket(player, new SM_SIEGE_LOCATION_INFO());
		}
	}

	/**
	 * 玩家进入攻城世界时同步地点状态。
	 * Syncs location status when a player enters a siege world.
	 *
	 * @param player 玩家 / player
	 */
	public void onEnterSiegeWorld(Player player) {
		Map<Integer, SiegeLocation> worldLocations = new HashMap<Integer, SiegeLocation>();
		Map<Integer, ArtifactLocation> worldArtifacts = new HashMap<Integer, ArtifactLocation>();
		for (SiegeLocation location : getSiegeLocations().values()) {
			if (location.getWorldId() == player.getWorldId()) {
				worldLocations.put(location.getLocationId(), location);
			}
		}
		for (ArtifactLocation artifact : getArtifacts().values()) {
			if (artifact.getWorldId() == player.getWorldId()) {
				worldArtifacts.put(artifact.getLocationId(), artifact);
			}
		}
		PacketSendUtility.sendPacket(player, new SM_SHIELD_EFFECT(worldLocations.values()));
		PacketSendUtility.sendPacket(player, new SM_ABYSS_ARTIFACT_INFO3(worldArtifacts.values()));
	}

	/**
	 * 天气变化时的攻城侧处理钩子。
	 * Hook for siege-side handling when weather changes.
	 *
	 * @param entry 天气条目 / weather entry
	 */
	public void onWeatherChanged(WeatherEntry entry) {
	}

	/**
	 * 将地点 ID 映射为要塞 ID。
	 * Maps a location id to its fortress id.
	 *
	 * location id
	 * fortress id
	 */
	public int getFortressId(int locId) {
		switch (locId) {
		case 49:
		case 61:
			return 1011;
		case 36:
		case 54:
			return 1131;
		case 37:
		case 55:
			return 1132;
		case 39:
		case 56:
			return 1141;
		case 45:
		case 57:
		case 72:
		case 75:
			return 1221;
		case 46:
		case 58:
		case 73:
		case 76:
			return 1231;
		case 47:
		case 59:
		case 74:
		case 77:
			return 1241;
		// 4.7
		case 102:
			return 7011;
		case 103:
			return 10111;
		case 104:
			return 10211;
		case 105:
			return 10311;
		case 106:
			return 10411;
		}
		return 0;
	}
}
