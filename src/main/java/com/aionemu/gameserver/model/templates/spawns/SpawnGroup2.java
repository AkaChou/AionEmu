package com.aionemu.gameserver.model.templates.spawns;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.anoha.AnohaStateType;
import com.aionemu.gameserver.model.beritra.BeritraStateType;
import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;
import com.aionemu.gameserver.model.iu.IuStateType;
import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.legiondominion.LegionDominionModType;
import com.aionemu.gameserver.model.legiondominion.LegionDominionRace;
import com.aionemu.gameserver.model.moltenus.MoltenusStateType;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;
import com.aionemu.gameserver.model.rvr.RvrStateType;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.svs.SvsStateType;
import com.aionemu.gameserver.model.templates.spawns.agentspawns.AgentSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.anohaspawns.AnohaSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.beritraspawns.BeritraSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.conquestspawns.ConquestSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns.DynamicRiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns.IdianDepthsSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.instanceriftspawns.InstanceRiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.iuspawns.IuSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspawns.LandingSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspecialspawns.LandingSpecialSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.legiondominionspawns.LegionDominionSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.moltenusspawns.MoltenusSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns.NightmareCircusSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.outpostspawns.OutpostSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.riftspawns.RiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.rvrspawns.RvrSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.svsspawns.SvsSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns.TowerOfEternitySpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns.ZorshivDredgionSpawnTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;
import com.aionemu.gameserver.model.vortex.VortexStateType;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;

/**
 * 刷新点队伍2模板（静态数据/XML）。
 * XML template.
 */
@Slf4j

public class SpawnGroup2 extends AbstractLockManager {

	private int worldId;
	private int npcId;
	private int pool;
	private byte difficultId;
	private int spawnPage;
	private int spawnPageEnd;
	private boolean spawnPageRestricted;
	private int initialDelay;
	private TemporarySpawn temporarySpawn;
	private int respawnTime;
	private SpawnHandlerType handlerType;
	private List<SpawnTemplate> spots = new ArrayList<SpawnTemplate>();
	private HashMap<Integer, HashMap<SpawnTemplate, Boolean>> poolUsedTemplates;

	public SpawnGroup2(int worldId, Spawn spawn) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			SpawnTemplate spawnTemplate = new SpawnTemplate(this, template);
			if (spawn.isEventSpawn()) {
				spawnTemplate.setEventTemplate(spawn.getEventTemplate());
			}
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, Race race) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			BaseSpawnTemplate spawnTemplate = new BaseSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setBaseRace(race);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, Race race, int miss) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			OutpostSpawnTemplate spawnTemplate = new OutpostSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setOutpostRace(race);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int siegeId, SiegeRace race, SiegeModType mod) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			SiegeSpawnTemplate spawnTemplate = new SiegeSpawnTemplate(this, template);
			spawnTemplate.setSiegeId(siegeId);
			spawnTemplate.setSiegeRace(race);
			spawnTemplate.setSiegeModType(mod);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int legionDominionId, LegionDominionRace race,
			LegionDominionModType mod) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			LegionDominionSpawnTemplate spawnTemplate = new LegionDominionSpawnTemplate(this, template);
			spawnTemplate.setLegionDominionId(legionDominionId);
			spawnTemplate.setLegionDominionRace(race);
			spawnTemplate.setLegionDominionModType(mod);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			RiftSpawnTemplate spawnTemplate = new RiftSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, VortexStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			VortexSpawnTemplate spawnTemplate = new VortexSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, BeritraStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			BeritraSpawnTemplate spawnTemplate = new BeritraSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setBStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, AgentStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			AgentSpawnTemplate spawnTemplate = new AgentSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setAStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, AnohaStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			AnohaSpawnTemplate spawnTemplate = new AnohaSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setCStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, ConquestStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			ConquestSpawnTemplate spawnTemplate = new ConquestSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setOStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, SvsStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			SvsSpawnTemplate spawnTemplate = new SvsSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setPStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, RvrStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			RvrSpawnTemplate spawnTemplate = new RvrSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setRStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, IuStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			IuSpawnTemplate spawnTemplate = new IuSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setIUStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, DynamicRiftStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			DynamicRiftSpawnTemplate spawnTemplate = new DynamicRiftSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setDStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, InstanceRiftStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			InstanceRiftSpawnTemplate spawnTemplate = new InstanceRiftSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setEStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, NightmareCircusStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			NightmareCircusSpawnTemplate spawnTemplate = new NightmareCircusSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setNStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, IdianDepthsStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			IdianDepthsSpawnTemplate spawnTemplate = new IdianDepthsSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setIStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, ZorshivDredgionStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			ZorshivDredgionSpawnTemplate spawnTemplate = new ZorshivDredgionSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setZStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, MoltenusStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			MoltenusSpawnTemplate spawnTemplate = new MoltenusSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setMStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int landingId, LandingStateType state) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			LandingSpawnTemplate spawnTemplate = new LandingSpawnTemplate(this, template);
			spawnTemplate.setId(landingId);
			spawnTemplate.setEStateType(state);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, LandingSpecialStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			LandingSpecialSpawnTemplate spawnTemplate = new LandingSpecialSpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setFStateType(type);
			spots.add(spawnTemplate);
		}
	}

	public SpawnGroup2(int worldId, Spawn spawn, int id, TowerOfEternityStateType type) {
		this.worldId = worldId;
		initializing(spawn);
		for (SpawnSpotTemplate template : spawn.getSpawnSpotTemplates()) {
			TowerOfEternitySpawnTemplate spawnTemplate = new TowerOfEternitySpawnTemplate(this, template);
			spawnTemplate.setId(id);
			spawnTemplate.setTStateType(type);
			spots.add(spawnTemplate);
		}
	}

	private void initializing(Spawn spawn) {
		temporarySpawn = spawn.getTemporarySpawn();
		respawnTime = spawn.getRespawnTime();
		pool = spawn.getPool();
		npcId = spawn.getNpcId();
		handlerType = spawn.getSpawnHandlerType();
		difficultId = spawn.getDifficultId();
		spawnPage = spawn.getSpawnPage();
		spawnPageEnd = spawn.getSpawnPageEnd();
		spawnPageRestricted = spawn.hasSpawnPage();
		initialDelay = spawn.getInitialDelay();
		if (hasPool()) {
			poolUsedTemplates = new HashMap<Integer, HashMap<SpawnTemplate, Boolean>>();
		}
	}

	public SpawnGroup2(int worldId, int npcId) {
		this.worldId = worldId;
		this.npcId = npcId;
	}

	/** 返回刷新模板 / Returns the spawn templates*/
	public List<SpawnTemplate> getSpawnTemplates() {
		return spots;
	}

	/** 添加刷新点模板。 / Adds spawn template. */
	public void addSpawnTemplate(SpawnTemplate spawnTemplate) {
		spots.add(spawnTemplate);
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 获取临时刷新 / Gets the temporary spawn */
	public TemporarySpawn geTemporarySpawn() {
		return temporarySpawn;
	}

	/** 返回刷新池大小 / Returns the pool */
	public int getPool() {
		return pool;
	}

	/**
	 * @return 是否使用对象池。 / Whether pool
	  */
	public boolean hasPool() {
		return pool > 0;
	}

	/** 返回重生时间 / Returns the respawn time */
	public int getRespawnTime() {
		return respawnTime;
	}

	/** 设置重生时间 / Sets the respawn time */
	public void setRespawnTime(int respawnTime) {
		this.respawnTime = respawnTime;
	}

	/**
	 * @return 是否为临时刷新 / whether temporary spawn
	 */
	public boolean isTemporarySpawn() {
		return temporarySpawn != null;
	}

	/** 获取处理器类型。 / Returns the handler type. */
	public SpawnHandlerType getHandlerType() {
		return handlerType;
	}

	/** 返回随机刷新模板 / Returns a random spawn template */
	public SpawnTemplate getRndTemplate(int instanceId) {
		final List<SpawnTemplate> allTemplates = spots;
		List<SpawnTemplate> templates = new ArrayList<SpawnTemplate>();
		super.readLock();
		try {
			for (SpawnTemplate template : allTemplates) {
				//if (!isTemplateUsed(instanceId, template)) {
					templates.add(template);
				//}
			}
			if (templates.size() == 0) {
				log.warn(I18n.get("log.05d6e85f725b", npcId, worldId));
				return null;
			}
		} finally {
			super.readUnlock();
		}
		SpawnTemplate spawnTemplate = templates.get(Rnd.get(0, templates.size() - 1));
		setTemplateUse(instanceId, spawnTemplate, true);
		return spawnTemplate;
	}

	/** 设置模板使用 / Sets the template use*/
	public void setTemplateUse(int instanceId, SpawnTemplate template, boolean isUsed) {
		super.writeLock();
		try {
			HashMap<SpawnTemplate, Boolean> states = poolUsedTemplates.get(instanceId);
			if (states == null) {
				states = new HashMap<SpawnTemplate, Boolean>();
				poolUsedTemplates.put(instanceId, states);
			}
			states.put(template, isUsed);
		} finally {
			super.writeUnlock();
		}
	}

	/**
	 * @return 模板是否已被使用 / whether template used
	 */
	public boolean isTemplateUsed(int instanceId, SpawnTemplate template) {
		super.readLock();
		try {
			HashMap<SpawnTemplate, Boolean> states = poolUsedTemplates.get(instanceId);
			if (states == null)
				return false;
			Boolean state = states.get(template);
			if (state == null)
				return false;
			return state;
		} finally {
			super.readUnlock();
		}
	}

	/** 重置模板 / Reset templates*/
	public void resetTemplates(int instanceId) {
		HashMap<SpawnTemplate, Boolean> states = poolUsedTemplates.get(instanceId);
		if (states == null)
			return;
		super.writeLock();
		try {
			for (SpawnTemplate template : states.keySet()) {
				states.put(template, false);
			}
		} finally {
			super.writeUnlock();
		}
	}

	/** 返回难度 ID / Returns the difficult id */
	public byte getDifficultId() {
		return difficultId;
	}

	/** 返回出生页起点。 / Returns the first matching spawn page. */
	public int getSpawnPage() {
		return spawnPage;
	}

	/** 是否声明了出生页限制。 / Whether a spawn page restriction is declared. */
	public boolean hasSpawnPage() {
		return spawnPageRestricted;
	}

	/** 返回出生页终点（含）。 / Returns the last matching spawn page, inclusive. */
	public int getSpawnPageEnd() {
		return spawnPageEnd;
	}

	/** 返回首次出生延迟（秒）。 / Returns the initial spawn delay in seconds. */
	public int getInitialDelay() {
		return initialDelay;
	}
}
