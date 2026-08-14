package com.aionemu.gameserver.spawnengine;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai.RetailNpcPartyEngine;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.List;
import java.util.function.Function;

import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
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
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * NPC 刷怪引擎入口：按模板创建可见对象并刷入世界。
 * NPC spawn engine entry: creates visible objects from templates and brings them into the world.
 * <p>
 * 当前实现为临时方案，后续可能替换。
 * Current implementation is temporal and may be replaced.
 *
 * @author Luno
 * @author ATracer
 * @author Source
 * @author Wakizashi
 * @author xTz
 * @author nrg
 */
@Slf4j
public class SpawnEngine {

	/**
	 * 根据刷怪模板创建并刷出 VisibleObject。
	 * Creates and spawns a VisibleObject from the given spawn template.
	 *
	 * @param spawn 刷怪模板 / spawn template
	 * @param instanceIndex 实例索引 / instance index
	 * @return 已创建并刷出的可见对象 / created and spawned visible object
	 */
	public static VisibleObject spawnObject(SpawnTemplate spawn, int instanceIndex) {
		final VisibleObject visObj = getSpawnedObject(spawn, instanceIndex);
		if (spawn.isEventSpawn()) {
			spawn.getEventTemplate().addSpawnedObject(visObj);
		}
		spawn.setVisibleObject(visObj);
		spawn.addVisibleObject(visObj);
		return visObj;
	}

	/**
	 * 按模板类型分派到具体刷怪实现。
	 * Dispatches to a concrete spawner by template type.
	 *
	 * @param spawn 刷怪模板 / spawn template
	 * @param instanceIndex 实例索引 / instance index
	 * @return 刷出的可见对象 / spawned visible object
	 */
	private static VisibleObject getSpawnedObject(SpawnTemplate spawn, int instanceIndex) {
		int objectId = spawn.getNpcId();
		if (objectId > 400000 && objectId < 499999) {
			return VisibleObjectSpawner.spawnGatherable(spawn, instanceIndex);
		} else if (spawn instanceof SiegeSpawnTemplate) {
			return VisibleObjectSpawner.spawnSiegeNpc((SiegeSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof BaseSpawnTemplate) {
			return VisibleObjectSpawner.spawnBaseNpc((BaseSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof OutpostSpawnTemplate) {
			return VisibleObjectSpawner.spawnOutpostNpc((OutpostSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof RiftSpawnTemplate) {
			return VisibleObjectSpawner.spawnRiftNpc((RiftSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof VortexSpawnTemplate) {
			return VisibleObjectSpawner.spawnInvasionNpc((VortexSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof BeritraSpawnTemplate) {
			return VisibleObjectSpawner.spawnBeritraNpc((BeritraSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof AgentSpawnTemplate) {
			return VisibleObjectSpawner.spawnAgentNpc((AgentSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof AnohaSpawnTemplate) {
			return VisibleObjectSpawner.spawnAnohaNpc((AnohaSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof ConquestSpawnTemplate) {
			return VisibleObjectSpawner.spawnConquestNpc((ConquestSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof SvsSpawnTemplate) {
			return VisibleObjectSpawner.spawnSvsNpc((SvsSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof RvrSpawnTemplate) {
			return VisibleObjectSpawner.spawnRvrNpc((RvrSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof IuSpawnTemplate) {
			return VisibleObjectSpawner.spawnIuNpc((IuSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof MoltenusSpawnTemplate) {
			return VisibleObjectSpawner.spawnMoltenusNpc((MoltenusSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof DynamicRiftSpawnTemplate) {
			return VisibleObjectSpawner.spawnDynamicRiftNpc((DynamicRiftSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof InstanceRiftSpawnTemplate) {
			return VisibleObjectSpawner.spawnInstanceRiftNpc((InstanceRiftSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof NightmareCircusSpawnTemplate) {
			return VisibleObjectSpawner.spawnNightmareCircusNpc((NightmareCircusSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof IdianDepthsSpawnTemplate) {
			return VisibleObjectSpawner.spawnIdianDepthsNpc((IdianDepthsSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof ZorshivDredgionSpawnTemplate) {
			return VisibleObjectSpawner.spawnZorshivDredgionNpc((ZorshivDredgionSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof LandingSpawnTemplate) {
			return VisibleObjectSpawner.spawnLandingNpc((LandingSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof LandingSpecialSpawnTemplate) {
			return VisibleObjectSpawner.spawnLandingSpecialNpc((LandingSpecialSpawnTemplate) spawn, instanceIndex);
		} else if (spawn instanceof TowerOfEternitySpawnTemplate) {
			return VisibleObjectSpawner.spawnTowerOfEternityNpc((TowerOfEternitySpawnTemplate) spawn, instanceIndex);
		} else {
			return VisibleObjectSpawner.spawnNpc(spawn, instanceIndex);
		}
	}

	/**
	 * 创建基础刷怪模板（无重生、无主人）。
	 * Creates a basic spawn template (no respawn, no master).
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @return 刷怪模板 / spawn template
	 */
	static SpawnTemplate createSpawnTemplate(int worldId, int npcId, float x, float y, float z, byte heading) {
		return new SpawnTemplate(new SpawnGroup2(worldId, npcId), x, y, z, heading, 0, null, 0, 0);
	}

	/**
	 * 创建带创建者与主人名的刷怪模板（如代码侧攻城刷怪）。
	 * Creates a spawn template with creator and master name (e.g. code-side siege spawns).
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @param creatorId 创建者 ID / creator id
	 * @param masterName 主人名 / master name
	 * @return 刷怪模板 / spawn template
	 */
	static SpawnTemplate createSpawnTemplate(int worldId, int npcId, float x, float y, float z, byte heading,
			int creatorId, String masterName) {
		SpawnTemplate template = createSpawnTemplate(worldId, npcId, x, y, z, heading);
		template.setCreatorId(creatorId);
		template.setMasterName(masterName);
		return template;
	}

	/**
	 * 添加攻城刷怪模板（非 static_data，如 CustomBalaurAssault）。
	 * Adds a siege spawn template from code rather than static_data (e.g. CustomBalaurAssault).
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param siegeId 攻城 ID / siege id
	 * @param race 攻城种族 / siege race
	 * @param mod 攻城模式类型 / siege mod type
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @return 攻城刷怪模板 / siege spawn template
	 */
	public static SiegeSpawnTemplate addNewSiegeSpawn(int worldId, int npcId, int siegeId, SiegeRace race,
			SiegeModType mod, float x, float y, float z, byte heading) {
		SiegeSpawnTemplate spawnTemplate = new SiegeSpawnTemplate(new SpawnGroup2(worldId, npcId), x, y, z, heading, 0,
				null, 0, 0);
		spawnTemplate.setSiegeId(siegeId);
		spawnTemplate.setSiegeRace(race);
		spawnTemplate.setSiegeModType(mod);
		return spawnTemplate;
	}

	/**
	 * 添加可配置重生时间的刷怪模板；死亡后是否删除由 respawnTime 决定。
	 * Adds a spawn template with configurable respawn; death cleanup depends on respawnTime.
	 * <p>
	 * 通过本方法创建的刷怪不会被 //save_spawn 持久化。
	 * Spawns created this way are not saved by //save_spawn.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @param respawnTime 重生时间（秒，0 表示不重生） / respawn time in seconds (0 = no respawn)
	 * @return 刷怪模板 / spawn template
	 */
	public static SpawnTemplate addNewSpawn(int worldId, int npcId, float x, float y, float z, byte heading,
			int respawnTime) {
		SpawnTemplate spawnTemplate = createSpawnTemplate(worldId, npcId, x, y, z, heading);
		spawnTemplate.setRespawnTime(respawnTime);
		return spawnTemplate;
	}

	/**
	 * 创建一次性、无重生的刷怪模板。
	 * Creates a non-permanent spawn template with no respawn.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @return 刷怪模板 / spawn template
	 */
	public static SpawnTemplate addNewSingleTimeSpawn(int worldId, int npcId, float x, float y, float z, byte heading) {
		return addNewSpawn(worldId, npcId, x, y, z, heading, 0);
	}

	/**
	 * 创建带创建者与主人名的一次性刷怪模板。
	 * Creates a single-time spawn template with creator and master name.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC 模板 ID / npc template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / heading
	 * @param creatorId 创建者 ID / creator id
	 * @param masterName 主人名 / master name
	 * @return 刷怪模板 / spawn template
	 */
	public static SpawnTemplate addNewSingleTimeSpawn(int worldId, int npcId, float x, float y, float z, byte heading,
			int creatorId, String masterName) {
		SpawnTemplate template = addNewSpawn(worldId, npcId, x, y, z, heading, 0);
		template.setCreatorId(creatorId);
		template.setMasterName(masterName);
		return template;
	}

	/**
	 * 将可见对象按刷怪模板坐标刷入指定实例。
	 * Brings a visible object into the world using spawn template coordinates.
	 *
	 * @param visibleObject 可见对象 / the visible object
	 * @param spawn 刷怪模板 / spawn template
	 * @param instanceIndex 实例索引 / instance index
	 */
	static void bringIntoWorld(VisibleObject visibleObject, SpawnTemplate spawn, int instanceIndex) {
		float z = projectedSpawnZ(visibleObject, spawn, npc -> GameWorldServices.pathService()
				.projectGroundPoint(npc, spawn.getX(), spawn.getY(), spawn.getZ()));
		bringIntoWorld(visibleObject, spawn.getWorldId(), instanceIndex, spawn.getX(), spawn.getY(), z,
				spawn.getHeading());
	}

	static float projectedSpawnZ(VisibleObject visibleObject, SpawnTemplate spawn, Function<Npc, float[]> projector) {
		if (!(visibleObject instanceof Npc npc) || spawn.canFly()) {
			return spawn.getZ();
		}
		float[] point = projector.apply(npc);
		if (point != null) {
			return point[2];
		}
		// PATH 节点容差外的出生点（如出生 Z 悬空于树冠上方）：用地形高度兜底，避免出生即悬空
		float terrainZ = GameWorldServices.geoService().getTerrainZ(npc.getWorldId(), spawn.getX(), spawn.getY());
		return Float.isNaN(terrainZ) ? spawn.getZ() : terrainZ;
	}

	/**
	 * 将可见对象登记、定位并刷入世界。
	 * Stores, positions and spawns a visible object into the world.
	 *
	 * @param visibleObject 可见对象 / the visible object
	 * @param worldId 世界 ID / world id
	 * @param instanceIndex 实例索引 / instance index
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / heading
	 */
	public static void bringIntoWorld(VisibleObject visibleObject, int worldId, int instanceIndex, float x, float y,
			float z, byte h) {
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		world.storeObject(visibleObject);
		world.setPosition(visibleObject, worldId, instanceIndex, x, y, z, h);
		world.spawn(visibleObject);
	}

	/**
	 * 将已有位置的可见对象登记并刷入世界。
	 * Stores and spawns a visible object that already has a position.
	 *
	 * @param visibleObject 可见对象 / the visible object
	 * @throws IllegalArgumentException 位置为空时 / when position is null
	 */
	public static void bringIntoWorld(VisibleObject visibleObject) {
		if (visibleObject.getPosition() == null)
			throw new IllegalArgumentException("Position is null");
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		world.storeObject(visibleObject);
		world.spawn(visibleObject);
	}

	/**
	 * 从模板刷出所有非副本世界的 NPC。
	 * Spawns all NPCs from templates for non-instance world maps.
	 */
	public static void spawnAll() {
		if (!DeveloperConfig.SPAWN_ENABLE) {
			log.info(I18n.get("log.6bd421074785"));
			return;
		}
		for (WorldMapTemplate worldMapTemplate : DataManager.WORLD_MAPS_DATA) {
			if (worldMapTemplate.isInstance()) {
				continue;
			}
			spawnBasedOnTemplate(worldMapTemplate);
		}
		DataManager.SPAWNS_DATA2.clearTemplates();
		printWorldSpawnStats();
	}

	/**
	 * 刷出指定世界地图（非副本）的全部刷怪。
	 * Spawns all objects for the given non-instance world map.
	 *
	 * @param worldId 世界 ID / world id
	 */
	public static void spawnWorldMap(int worldId) {
		WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
		if (template != null && !template.isInstance()) {
			spawnBasedOnTemplate(template);
		}
	}

	/**
	 * 按世界模板刷出所有双子实例。
	 * Spawns all twin instances defined by the world map template.
	 *
	 * @param worldMapTemplate 世界地图模板 / world map template
	 */
	private static void spawnBasedOnTemplate(WorldMapTemplate worldMapTemplate) {
		int maxTwin = worldMapTemplate.getTwinCount();
		final int mapId = worldMapTemplate.getMapId();
		int numberToSpawn = maxTwin > 0 ? maxTwin : 1;

		for (int instanceId = 1; instanceId <= numberToSpawn; instanceId++) {
			spawnInstance(mapId, instanceId, 0);
		}
	}

	/**
	 * 刷出指定世界实例（难度 0）。
	 * Spawns the given world instance with difficulty 0.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 * @param difficultId 难度 ID / difficulty id
	 */
	public static void spawnInstance(int worldId, int instanceId, int difficultId) {
		spawnInstance(worldId, instanceId, difficultId, 0);
	}

	/**
	 * 刷出指定世界实例的门、NPC、静态物与房屋。
	 * Spawns doors, NPCs, static objects and houses for the world instance.
	 *
	 * @param worldId 世界 ID / world id
	 * @param instanceId 实例 ID / instance id
	 * @param difficultId 难度 ID / difficulty id
	 * @param ownerId 房屋所有者 ID / house owner id
	 */
	public static void spawnInstance(int worldId, int instanceId, int difficultId, int ownerId) {
		List<SpawnGroup2> worldSpawns = DataManager.SPAWNS_DATA2.getSpawnsByWorldId(worldId);
		WorldMapTemplate worldTemplate = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
		StaticDoorSpawnManager.spawnTemplate(worldId, instanceId);
		int spawnedCounter = 0;
		if (worldSpawns != null) {
			for (SpawnGroup2 spawn : worldSpawns) {
				int difficult = spawn.getDifficultId();
				if (difficult != 0 && difficult != difficultId) {
					continue;
				}

				// 副本中禁用临时生成，TemporarySpawnEngine / Disable temporary spawns in instances, TemporarySpawnEngine
				// 不支持移除生成 / doesn't support removing spawns
				if (spawn.isTemporarySpawn() && !worldTemplate.isInstance()) {
					TemporarySpawnEngine.addSpawnGroup(spawn, instanceId);
					continue;
				}

				if (spawn.getHandlerType() != null) {
					switch (spawn.getHandlerType()) {
					case RIFT:
					case VOLATILE_RIFT:
						RiftManager.addRiftSpawnTemplate(spawn);
						break;
					case STATIC:
						StaticObjectSpawnManager.spawnTemplate(spawn, instanceId);
					default:
						break;
					}
				} else if (spawn.hasPool() && checkPool(spawn)) {
					for (int i = 0; i < spawn.getPool(); i++) {
						SpawnTemplate template = spawn.getRndTemplate(instanceId);
						if (template == null)
							break;
						spawnObject(template, instanceId);
						spawnedCounter++;
					}
				} else {
					for (SpawnTemplate template : spawn.getSpawnTemplates()) {
						spawnObject(template, instanceId);
						spawnedCounter++;
					}
				}
			}
			WalkerFormator.organizeAndSpawn(worldId, instanceId);
		}
		log.info(I18n.get("log.1a270a579228", worldId, instanceId, spawnedCounter));
		GameHousingServices.housingService().spawnHouses(worldId, instanceId, ownerId);
		var instance = GameWorldBootstrapServices.world().getWorldMap(worldId).getWorldMapInstanceById(instanceId);
		RetailNpcPartyEngine.initialize(instance);
		RetailConditionSpawnEngine.initialize(instance);
	}

	/**
	 * 校验池大小是否不超过可用模板数。
	 * Validates that pool size does not exceed available templates.
	 *
	 * @param spawn 刷怪组 / the spawn group
	 * @return 池大小有效则为 true / true if valid
	 */
	private static boolean checkPool(SpawnGroup2 spawn) {
		if (spawn.getSpawnTemplates().size() < spawn.getPool()) {
			log.warn(I18n.get("log.05d6e85f725b", spawn.getNpcId(), spawn.getWorldId()));
			return false;
		}
		return true;
	}

	/**
	 * 统计并打印世界中 NPC 与采集物数量。
	 * Counts and logs NPC and gatherable totals in the world.
	 */
	public static void printWorldSpawnStats() {
		StatsCollector visitor = new StatsCollector();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllObjects(visitor);
		log.info(I18n.get("log.6931cfbae670", visitor.getNpcCount()));
		log.info(I18n.get("log.a6e250bd76ce", visitor.getGatherableCount()));
	}

	/**
	 * 世界对象统计访问者。
	 * Visitor that tallies NPCs and gatherables.
	 */
	static class StatsCollector implements Visitor<VisibleObject> {

		/**
		 * NPC 计数。
		 * NPC count.
		 */
		int npcCount;

		/**
		 * 采集物计数。
		 * Gatherable count.
		 */
		int gatherableCount;

		@Override
		public void visit(VisibleObject object) {
			if (object instanceof Npc) {
				npcCount++;
			} else if (object instanceof Gatherable) {
				gatherableCount++;
			}
		}

		/**
		 * NPC 数量 / npc count
		 */
		public int getNpcCount() {
			return npcCount;
		}

		/**
		 * @return 采集物数量 / gatherable count
		 */
		public int getGatherableCount() {
			return gatherableCount;
		}
	}
}
