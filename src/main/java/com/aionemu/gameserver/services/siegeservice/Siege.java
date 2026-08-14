package com.aionemu.gameserver.services.siegeservice;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.npc.AbyssNpcType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SIEGE_LOCATION_STATE;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.world.World;
/**
 * 攻城实例基类，管理攻城生命周期、BOSS 与广播。
 * Siege instance base managing siege lifecycle, boss and broadcasts.
 */
@Slf4j

public abstract class Siege<SL extends SiegeLocation> {
	private final SiegeBossDeathListener siegeBossDeathListener = new SiegeBossDeathListener(this);
	private final SiegeBossDoAddDamageListener siegeBossDoAddDamageListener = new SiegeBossDoAddDamageListener(this);
	private final AtomicBoolean finished = new AtomicBoolean();
	private final SiegeCounter siegeCounter = new SiegeCounter();
	private final SL siegeLocation;
	private boolean bossKilled;
	private SiegeNpc boss, flag;
	private Date startTime;
	private boolean started;

	public Siege(SL siegeLocation) {
		this.siegeLocation = siegeLocation;
	}

	/**
	 * 开始攻城。
	 * Starts the siege.
	 */
	public final void startSiege() {
		boolean doubleStart = false;
		synchronized (this) {
			if (started) {
				doubleStart = true;
			} else {
				startTime = new Date();
				started = true;
			}
		}
		if (doubleStart) {
			log.error(I18n.get("log.877d6e5ee3ef", siegeLocation.getLocationId()));
			return;
		}
		onSiegeStart();
		if (SiegeConfig.BALAUR_AUTO_ASSAULT) {
			GameCoreGameplayServices.balaurAssaultService().onSiegeStart(this);
		}
	}

	/**
	 * 按指定据点 ID 开始攻城。
	 * Starts the siege for the given location id.
	 *
	 * @param locationId 据点 ID / location id
	 */
	public final void startSiege(int locationId) {
		GameFeatureServices.siegeService().startSiege(locationId);
	}

	/**
	 * 停止攻城。
	 * Stops the siege.
	 */
	public final void stopSiege() {
		if (finished.compareAndSet(false, true)) {
			onSiegeFinish();
			if (SiegeConfig.BALAUR_AUTO_ASSAULT) {
				GameCoreGameplayServices.balaurAssaultService().onSiegeFinish(this);
			}
		} else {
			log.error(I18n.get("log.441230160aab", siegeLocation.getLocationId()));
		}
	}

	/**
	 * 返回攻城据点。
	 * Returns the siege location.
	 *
	 * @return 攻城据点 / siege location
	 */
	public SL getSiegeLocation() {
		return siegeLocation;
	}

	/**
	 * 返回攻城据点 ID。
	 * Returns the siege location id.
	 *
	 * @return 据点 ID / location id
	 */
	public int getSiegeLocationId() {
		return siegeLocation.getLocationId();
	}

	/**
	 * 返回攻城首领是否已被击杀。
	 * Returns whether the siege boss has been killed.
	 *
	 * @return 是否已击杀首领 / whether boss was killed
	 */
	public boolean isBossKilled() {
		return bossKilled;
	}

	/**
	 * setBossKilled 方法。
	 * setBossKilled method.
	 *
	 * @param bossKilled 是否击杀首领 / bossKilled
	 */
	public void setBossKilled(boolean bossKilled) {
		this.bossKilled = bossKilled;
	}

	/**
	 * 返回攻城首领 NPC。
	 * Returns the siege boss NPC.
	 *
	 * @return 首领 NPC / boss NPC
	 */
	public SiegeNpc getBoss() {
		return boss;
	}

	/**
	 * 设置攻城首领 NPC。
	 * Sets the siege boss NPC.
	 *
	 * @param boss 首领 NPC / boss NPC
	 */
	public void setBoss(SiegeNpc boss) {
		this.boss = boss;
	}

	/**
	 * 返回首领伤害监听器。
	 * Returns the boss damage listener.
	 *
	 * @return 伤害监听器 / damage listener
	 */
	public SiegeBossDoAddDamageListener getSiegeBossDoAddDamageListener() {
		return siegeBossDoAddDamageListener;
	}

	/**
	 * 返回首领死亡监听器。
	 * Returns the boss death listener.
	 *
	 * @return 死亡监听器 / death listener
	 */
	public SiegeBossDeathListener getSiegeBossDeathListener() {
		return siegeBossDeathListener;
	}

	/**
	 * 返回攻城计数器。
	 * Returns the siege counter.
	 *
	 * @return 攻城计数器 / siege counter
	 */
	public SiegeCounter getSiegeCounter() {
		return siegeCounter;
	}

	protected abstract void onSiegeStart();

	protected abstract void onSiegeFinish();

	/**
	 * 累计 BOSS 伤害。
	 * Adds boss damage.
	 *
	 * @param attacker 攻击者 / attacker
	 * @param damage 伤害量 / damage
	 */
	public void addBossDamage(Creature attacker, int damage) {
		if (isFinished()) {
			return;
		}
		if (attacker == null) {
			return;
		}
		attacker = attacker.getMaster();
		getSiegeCounter().addDamage(attacker, damage);
	}

	public abstract boolean isEndless();

	public abstract void addAbyssPoints(Player player, int abysPoints);

	/**
	 * 返回攻城是否已开始。
	 * Returns whether the siege has started.
	 *
	 * @return 是否已开始 / whether started
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * 返回攻城是否已结束。
	 * Returns whether the siege has finished.
	 *
	 * @return 是否已结束 / whether finished
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 返回攻城开始时间。
	 * Returns the siege start time.
	 *
	 * @return 开始时间 / start time
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * 注册攻城 BOSS 监听。
	 * Registers siege boss listeners.
	 */
	protected void registerSiegeBossListeners() {
		EnhancedObject eo = (EnhancedObject) getBoss().getAggroList();
		eo.addCallback(getSiegeBossDoAddDamageListener());
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		eo = (EnhancedObject) ai;
		eo.addCallback(getSiegeBossDeathListener());
	}

	/**
	 * 注销攻城 BOSS 监听。
	 * Unregisters siege boss listeners.
	 */
	protected void unregisterSiegeBossListeners() {
		EnhancedObject eo = (EnhancedObject) getBoss().getAggroList();
		eo.removeCallback(getSiegeBossDoAddDamageListener());
		AbstractAI ai = (AbstractAI) getBoss().getAi2();
		eo = (EnhancedObject) ai;
		eo.removeCallback(getSiegeBossDeathListener());
	}

	/**
	 * 初始化攻城 BOSS。
	 * Initializes the siege boss.
	 */
	protected void initSiegeBoss() {
		SiegeNpc boss = null;
		Collection<SiegeNpc> npcs = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getLocalSiegeNpcs(getSiegeLocationId());
		for (SiegeNpc npc : npcs) {
			if (npc.getObjectTemplate().getAbyssNpcType().equals(AbyssNpcType.BOSS)) {
				if (boss != null) {
					throw new SiegeException("Found 2 siege bosses for siege " + getSiegeLocationId());
				}
				boss = npc;
			}
		}
		if (boss == null) {
			throw new SiegeException("Siege Boss not found for siege " + getSiegeLocationId());
		}
		setBoss(boss);
		registerSiegeBossListeners();
	}

	/**
	 * 刷出 NPC。
	 * Spawns NPCs.
	 *
	 * @param locationId 据点 ID / location id
	 * @param race 阵营 / race
	 * @param type 攻城模式 / siege mod type
	 */
	protected void spawnNpcs(int locationId, SiegeRace race, SiegeModType type) {
		GameFeatureServices.siegeService().spawnNpcs(locationId, race, type);
	}

	/**
	 * 移除 NPC。
	 * Despawns NPCs.
	 *
	 * @param locationId 据点 ID / location id
	 */
	protected void deSpawnNpcs(int locationId) {
		GameFeatureServices.siegeService().deSpawnNpcs(locationId);
	}

	/**
	 * 广播攻城状态。
	 * Broadcasts siege state.
	 *
	 * @param location 攻城据点 / siege location
	 */
	protected void broadcastState(SiegeLocation location) {
		GameFeatureServices.siegeService().broadcast(new SM_SIEGE_LOCATION_STATE(location), null);
	}

	/**
	 * 广播攻城更新。
	 * Broadcasts siege update.
	 *
	 * @param location 攻城据点 / siege location
	 */
	protected void broadcastUpdate(SiegeLocation location) {
		GameFeatureServices.siegeService().broadcastUpdate(location);
	}

	/**
	 * 广播攻城更新（带名称 ID）。
	 * Broadcasts siege update (with name id).
	 *
	 * @param location 攻城据点 / siege location
	 * @param nameId 名称 ID / name id
	 */
	protected void broadcastUpdate(SiegeLocation location, int nameId) {
		GameFeatureServices.siegeService().broadcastUpdate(location, new DescriptionId(nameId));
	}
}