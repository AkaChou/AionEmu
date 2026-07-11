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
	 * 开始攻城。
	 * Starts the siege.
	 *
	 * locationId
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
	 * getSiegeLocation 方法。
	 * getSiegeLocation method.
	 * result
	 */
	public SL getSiegeLocation() {
		return siegeLocation;
	}

	/**
	 * getSiegeLocationId 方法。
	 * getSiegeLocationId method.
	 * result
	 */
	public int getSiegeLocationId() {
		return siegeLocation.getLocationId();
	}

	/**
	 * isBossKilled 方法。
	 * isBossKilled method.
	 * result
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
	 * getBoss 方法。
	 * getBoss method.
	 * result
	 */
	public SiegeNpc getBoss() {
		return boss;
	}

	/**
	 * setBoss 方法。
	 * setBoss method.
	 *
	 * boss
	 */
	public void setBoss(SiegeNpc boss) {
		this.boss = boss;
	}

	/**
	 * getSiegeBossDoAddDamageListener 方法。
	 * getSiegeBossDoAddDamageListener method.
	 * result
	 */
	public SiegeBossDoAddDamageListener getSiegeBossDoAddDamageListener() {
		return siegeBossDoAddDamageListener;
	}

	/**
	 * getSiegeBossDeathListener 方法。
	 * getSiegeBossDeathListener method.
	 * result
	 */
	public SiegeBossDeathListener getSiegeBossDeathListener() {
		return siegeBossDeathListener;
	}

	/**
	 * getSiegeCounter 方法。
	 * getSiegeCounter method.
	 * result
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
	 * attacker
	 * damage
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
	 * isStarted 方法。
	 * isStarted method.
	 * result
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * isFinished 方法。
	 * isFinished method.
	 * result
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * getStartTime 方法。
	 * getStartTime method.
	 * result
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
	 * locationId
	 * 阵营 / race
	 * type
	 */
	protected void spawnNpcs(int locationId, SiegeRace race, SiegeModType type) {
		GameFeatureServices.siegeService().spawnNpcs(locationId, race, type);
	}

	/**
	 * 移除 NPC。
	 * Despawns NPCs.
	 *
	 * locationId
	 */
	protected void deSpawnNpcs(int locationId) {
		GameFeatureServices.siegeService().deSpawnNpcs(locationId);
	}

	/**
	 * 广播攻城状态。
	 * Broadcasts siege state.
	 *
	 * location
	 */
	protected void broadcastState(SiegeLocation location) {
		GameFeatureServices.siegeService().broadcast(new SM_SIEGE_LOCATION_STATE(location), null);
	}

	/**
	 * 广播攻城更新。
	 * Broadcasts siege update.
	 *
	 * location
	 */
	protected void broadcastUpdate(SiegeLocation location) {
		GameFeatureServices.siegeService().broadcastUpdate(location);
	}

	/**
	 * 广播攻城更新。
	 * Broadcasts siege update.
	 *
	 * location
	 * nameId
	 */
	protected void broadcastUpdate(SiegeLocation location, int nameId) {
		GameFeatureServices.siegeService().broadcastUpdate(location, new DescriptionId(nameId));
	}
}