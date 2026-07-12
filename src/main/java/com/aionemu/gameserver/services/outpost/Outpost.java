package com.aionemu.gameserver.services.outpost;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.outpost.OutpostNpc;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.outpostspawns.OutpostSpawnTemplate;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.world.World;

/**
 * 前哨据点运行时对象，管理归属种族、旗帜/BOSS 与周期性袭击。
 * Runtime outpost object managing owning race, flag/boss and periodic assaults.
 *
 * @author Wnkrz
 * @param <OL> 前哨位置类型 / Outpost location type
 */
public class Outpost<OL extends OutpostLocation> {
	private Npc boss, flag;
	private boolean started;
	private final OL outpostLocation;
	private Future<?> startAssault, stopAssault;
	private List<Race> list = new ArrayList<Race>();
	private List<Npc> spawned = new ArrayList<Npc>();
	private List<Npc> attackers = new ArrayList<Npc>();
	private final AtomicBoolean finished = new AtomicBoolean();
	private final OutpostBossDeathListener baseBossDeathListener = new OutpostBossDeathListener(this);

	/**
	 * 以前哨位置模板构造运行时实例。
	 * Constructs a runtime instance from an outpost location template.
	 *
	 * Outpost location
	 */
	public Outpost(OL outpostLocation) {
		list.add(Race.ASMODIANS);
		list.add(Race.ELYOS);
		list.add(Race.NPC);
		this.outpostLocation = outpostLocation;
	}

	/**
	 * 启动前哨（生成守卫/旗帜）；重复调用无效。
	 * Starts the outpost (spawns guards/flag); no-op on double start.
	 */
	public final void start() {
		boolean doubleStart = false;
		synchronized (this) {
			if (started) {
				doubleStart = true;
			} else {
				started = true;
			}
		}
		if (doubleStart) {
			return;
		}
		spawn();
	}

	/**
	 * 停止前哨并清理生成物。
	 * Stops the outpost and despawns its entities.
	 */
	public final void stop() {
		if (finished.compareAndSet(false, true)) {
			despawn(getId());
		}
	}

	/**
	 * 读取该前哨的刷怪配置。
	 * Loads spawn groups for this outpost.
	 *
	 * @return 刷怪组列表 / Spawn group list
	 */
	private List<SpawnGroup2> getOutpostSpawns() {
		List<SpawnGroup2> spawns = DataManager.SPAWNS_DATA2.getOutpostSpawnsByLocId(getId());
		if (spawns == null) {
		}
		return spawns;
	}

	/**
	 * 按当前归属种族生成前哨单位（含旗帜）。
	 * Spawns outpost units for the current owning race (including flag).
	 */
	protected void spawn() {
		for (SpawnGroup2 group : getOutpostSpawns()) {
			for (SpawnTemplate spawn : group.getSpawnTemplates()) {
				final OutpostSpawnTemplate template = (OutpostSpawnTemplate) spawn;
				if (template.getOutpostRace().equals(getOutpostLocation().getRace())) {
					if (template.getHandlerType() == null) {
						Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
						NpcTemplate npcTemplate = npc.getObjectTemplate();
						if (npcTemplate.getNpcTemplateType().equals(NpcTemplateType.FLAG)) {
							setFlag(npc);
						}
						getSpawned().add(npc);
					}
				}
			}
		}
	}

	/**
	 * 是否仍有存活的袭击单位。
	 * Whether assault attackers are still alive.
	 *
	 * @return {@code true} 正在被袭击 / {@code true} if under attack
	 */
	public boolean isAttacked() {
		for (Npc attacker : getAttackers()) {
			if (!attacker.getLifeStats().isAlreadyDead()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 清理指定前哨的全部本地 NPC。
	 * Despawns all local NPCs of the given outpost.
	 *
	 * Outpost location id
	 */
	protected void despawn(int outpostLocationId) {
		setFlag(null);
		Collection<OutpostNpc> outpostNpcs = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getLocalOutpostNpcs(outpostLocationId);
		for (OutpostNpc npc : new ArrayList<OutpostNpc>(outpostNpcs)) {
			npc.getController().onDelete();
		}
	}

	/**
	 * 延迟调度下一次袭击。
	 * Schedules the next assault after a random delay.
	 */
	private void delayedAssault() {
		startAssault = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				chooseAttackersRace();
			}
		}, Rnd.get(120, 180) * 60000);
	}

	/**
	 * 选择非归属种族作为袭击方并生成。
	 * Picks a non-owning race as attackers and spawns them.
	 */
	protected void chooseAttackersRace() {
		AtomicBoolean next = new AtomicBoolean(Math.random() < 0.5);
		for (Race race : list) {
			if (!race.equals(getRace())) {
				if (next.compareAndSet(true, false)) {
					continue;
				}
				spawnAttackers(race);
			}
		}
	}

	/**
	 * 生成指定种族的袭击单位；区域非活跃时可能直接占领或改期。
	 * Spawns attackers of the given race; may capture or reschedule if region inactive.
	 *
	 * @param race 袭击种族 / Attacking race
	 */
	public void spawnAttackers(Race race) {
		if (getFlag() == null) {
		} else if (!getFlag().getPosition().getMapRegion().isMapRegionActive()) {
			if (Math.random() < 0.5) {
				GameLocationBootstrapServices.outpostService().capture(getId(), race);
				GameLocationBootstrapServices.outpostService().captureArtifact(getId(), race);
			} else {
				delayedAssault();
			}
			return;
		}
		if (!isAttacked()) {
			despawnAttackers();
			for (SpawnGroup2 group : getOutpostSpawns()) {
				for (SpawnTemplate spawn : group.getSpawnTemplates()) {
					final OutpostSpawnTemplate template = (OutpostSpawnTemplate) spawn;
					if (template.getOutpostRace().equals(race)) {
						if (template.getHandlerType() != null
								&& template.getHandlerType().equals(SpawnHandlerType.SLAYER)) {
							Npc npc = (Npc) SpawnEngine.spawnObject(template, 1);
							getAttackers().add(npc);
						}
					}
				}
			}
			if (getAttackers().isEmpty()) {
			} else {
				stopAssault = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnAttackers();
						delayedAssault();
					}
				}, 5 * 60000);
			}
		}
	}

	/**
	 * 删除全部袭击单位。
	 * Despawns all attackers.
	 */
	protected void despawnAttackers() {
		for (Npc attacker : new ArrayList<Npc>(getAttackers())) {
			attacker.getController().onDelete();
		}
		getAttackers().clear();
	}

	/**
	 * 获取旗帜 NPC。
	 * Returns the flag NPC.
	 *
	 * Flag NPC
	 */
	public Npc getFlag() {
		return flag;
	}

	/**
	 * 设置旗帜 NPC。
	 * Sets the flag NPC.
	 *
	 * Flag NPC
	 */
	public void setFlag(Npc flag) {
		this.flag = flag;
	}

	/**
	 * 获取 BOSS NPC。
	 * Returns the boss NPC.
	 *
	 * Boss NPC
	 */
	public Npc getBoss() {
		return boss;
	}

	/**
	 * 设置 BOSS NPC。
	 * Sets the boss NPC.
	 *
	 * Boss NPC
	 */
	public void setBoss(Npc boss) {
		this.boss = boss;
	}

	/**
	 * 获取 BOSS 死亡监听器。
	 * Returns the boss death listener.
	 *
	 * Death listener
	 */
	public OutpostBossDeathListener getOutpostBossDeathListener() {
		return baseBossDeathListener;
	}

	/**
	 * 前哨是否已结束。
	 * Whether the outpost is finished.
	 *
	 * @return {@code true} if finished。
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 获取前哨位置模板。
	 * Returns the outpost location template.
	 *
	 * Location template
	 */
	public OL getOutpostLocation() {
		return outpostLocation;
	}

	/**
	 * 获取前哨 ID。
	 * Returns the outpost id.
	 *
	 * Outpost id
	 */
	public int getId() {
		return outpostLocation.getId();
	}

	/**
	 * 获取当前归属种族。
	 * Returns the owning race.
	 *
	 * @return 阵营 / Race
	 */
	public Race getRace() {
		return outpostLocation.getRace();
	}

	/**
	 * 设置归属种族。
	 * Sets the owning race.
	 *
	 * @param race 阵营 / Race
	 */
	public void setRace(Race race) {
		outpostLocation.setRace(race);
	}

	/**
	 * 获取袭击单位列表。
	 * Returns the attacker list.
	 *
	 * Attackers
	 */
	public List<Npc> getAttackers() {
		return attackers;
	}

	/**
	 * 获取已生成单位列表。
	 * Returns the spawned unit list.
	 *
	 * @return 已生成单位 / Spawned units
	 */
	public List<Npc> getSpawned() {
		return spawned;
	}
}
