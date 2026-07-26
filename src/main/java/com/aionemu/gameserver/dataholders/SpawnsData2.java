package com.aionemu.gameserver.dataholders;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnMap;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.agentspawns.AgentSpawn;
import com.aionemu.gameserver.model.templates.spawns.anohaspawns.AnohaSpawn;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawn;
import com.aionemu.gameserver.model.templates.spawns.beritraspawns.BeritraSpawn;
import com.aionemu.gameserver.model.templates.spawns.conquestspawns.ConquestSpawn;
import com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns.DynamicRiftSpawn;
import com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns.IdianDepthsSpawn;
import com.aionemu.gameserver.model.templates.spawns.instanceriftspawns.InstanceRiftSpawn;
import com.aionemu.gameserver.model.templates.spawns.iuspawns.IuSpawn;
import com.aionemu.gameserver.model.templates.spawns.landingspawns.LandingSpawn;
import com.aionemu.gameserver.model.templates.spawns.landingspecialspawns.LandingSpecialSpawn;
import com.aionemu.gameserver.model.templates.spawns.legiondominionspawns.LegionDominionSpawn;
import com.aionemu.gameserver.model.templates.spawns.moltenusspawns.MoltenusSpawn;
import com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns.NightmareCircusSpawn;
import com.aionemu.gameserver.model.templates.spawns.outpostspawns.OutpostSpawn;
import com.aionemu.gameserver.model.templates.spawns.riftspawns.RiftSpawn;
import com.aionemu.gameserver.model.templates.spawns.rvrspawns.RvrSpawn;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawn;
import com.aionemu.gameserver.model.templates.spawns.svsspawns.SvsSpawn;
import com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns.TowerOfEternitySpawn;
import com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawn;
import com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns.ZorshivDredgionSpawn;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 刷怪数据容器：按地图与活动类型索引 {@link SpawnGroup2}，并支持管理员运行时保存。
 * Spawn data holder that indexes {@link SpawnGroup2} by map and event type, with admin runtime save support.
 */
@XmlRootElement(name = "spawns")
@XmlType(namespace = "", name = "SpawnsData2")
@XmlAccessorType(XmlAccessType.NONE)
@Slf4j
public class SpawnsData2 {

	@XmlElement(name = "spawn_map", type = SpawnMap.class)
	protected List<SpawnMap> templates;

	private IntObjectHashMap<Map<Integer, SimpleEntry<SpawnGroup2, Spawn>>> allSpawnMaps = new IntObjectHashMap<Map<Integer, SimpleEntry<SpawnGroup2, Spawn>>>();
	private IntObjectHashMap<LinkedHashMap<Spawn, SpawnGroup2>> allSpawnGroups = new IntObjectHashMap<LinkedHashMap<Spawn, SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> siegeSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> baseSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> vortexSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> riftSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> beritraSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> agentSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> anohaSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> rvrSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> svsSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> iuSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> dynamicRiftSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> instanceRiftSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> idianDepthsSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> nightmareCircusSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> legionDominionSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> zorshivDredgionSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> moltenusSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> conquestSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> landingSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> landingSpecialSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> towerOfEternitySpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<List<SpawnGroup2>> outpostSpawnMaps = new IntObjectHashMap<List<SpawnGroup2>>();
	private IntObjectHashMap<Spawn> customs = new IntObjectHashMap<Spawn>();

	private Map<Integer, SimpleEntry<SpawnGroup2, Spawn>> spawnIndexForWorld(int mapId) {
		Map<Integer, SimpleEntry<SpawnGroup2, Spawn>> worldSpawns = allSpawnMaps.get(mapId);
		if (worldSpawns == null) {
			worldSpawns = new LinkedHashMap<Integer, SimpleEntry<SpawnGroup2, Spawn>>();
			allSpawnMaps.put(mapId, worldSpawns);
		}
		return worldSpawns;
	}

	private void indexWorldSpawn(int mapId, Map<Integer, SimpleEntry<SpawnGroup2, Spawn>> worldSpawns,
			SpawnGroup2 spawnGroup, Spawn spawn) {
		LinkedHashMap<Spawn, SpawnGroup2> groups = allSpawnGroups.get(mapId);
		if (groups == null) {
			groups = new LinkedHashMap<>();
			allSpawnGroups.put(mapId, groups);
		}
		if (spawn.isCustom()) {
			groups.entrySet().removeIf(entry -> entry.getKey().getNpcId() == spawn.getNpcId());
		}
		groups.put(spawn, spawnGroup);
		worldSpawns.put(spawn.getNpcId(), new SimpleEntry<>(spawnGroup, spawn));
	}

	private List<SpawnGroup2> spawnGroupsFor(IntObjectHashMap<List<SpawnGroup2>> spawnMaps, int id) {
		List<SpawnGroup2> spawnGroups = spawnMaps.get(id);
		if (spawnGroups == null) {
			spawnGroups = new ArrayList<SpawnGroup2>();
			spawnMaps.put(id, spawnGroups);
		}
		return spawnGroups;
	}

	/**
	 * JAXB 反序列化完成后，将各类刷怪模板索引到运行时映射。
	 * After JAXB unmarshalling, indexes all spawn templates into runtime maps.
	 *
	 * @param u Unmarshaller
	 * parent object
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		if (templates != null) {
			for (SpawnMap spawnMap : templates) {
				int mapId = spawnMap.getMapId();
				Map<Integer, SimpleEntry<SpawnGroup2, Spawn>> worldSpawns = spawnIndexForWorld(mapId);
				for (Spawn spawn : spawnMap.getSpawns()) {
					if (spawn.isCustom()) {
						if (worldSpawns.containsKey(spawn.getNpcId())) {
							worldSpawns.remove(spawn.getNpcId());
						}
						customs.put(spawn.getNpcId(), spawn);
					} else if (customs.containsKey(spawn.getNpcId())) {
						continue;
					}
					SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn);
					indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
				}
				for (SiegeSpawn SiegeSpawn : spawnMap.getSiegeSpawns()) {
					int siegeId = SiegeSpawn.getSiegeId();
					List<SpawnGroup2> siegeSpawnGroups = spawnGroupsFor(siegeSpawnMaps, siegeId);
					for (SiegeSpawn.SiegeRaceTemplate race : SiegeSpawn.getSiegeRaceTemplates()) {
						for (SiegeSpawn.SiegeRaceTemplate.SiegeModTemplate mod : race.getSiegeModTemplates()) {
							if (mod == null || mod.getSpawns() == null) {
								continue;
							}
							for (Spawn spawn : mod.getSpawns()) {
								if (spawn.isCustom()) {
									if (worldSpawns.containsKey(spawn.getNpcId())) {
										worldSpawns.remove(spawn.getNpcId());
									}
									customs.put(spawn.getNpcId(), spawn);
								} else if (customs.containsKey(spawn.getNpcId())) {
									continue;
								}
								SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, siegeId, race.getSiegeRace(),
										mod.getSiegeModType());
								indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
								siegeSpawnGroups.add(spawnGroup);
							}
						}
					}
				}
				for (LegionDominionSpawn LegionDominionSpawn : spawnMap.getLegionDominionSpawns()) {
					int legionDominionId = LegionDominionSpawn.getLegionDominionId();
					List<SpawnGroup2> legionDominionSpawnGroups = spawnGroupsFor(legionDominionSpawnMaps,
							legionDominionId);
					for (LegionDominionSpawn.LegionDominionRaceTemplate race : LegionDominionSpawn
							.getLegionDominionRaceTemplates()) {
						for (LegionDominionSpawn.LegionDominionRaceTemplate.LegionDominionModTemplate mod : race
								.getLegionDominionModTemplates()) {
							if (mod == null || mod.getSpawns() == null) {
								continue;
							}
							for (Spawn spawn : mod.getSpawns()) {
								if (spawn.isCustom()) {
									if (worldSpawns.containsKey(spawn.getNpcId())) {
										worldSpawns.remove(spawn.getNpcId());
									}
									customs.put(spawn.getNpcId(), spawn);
								} else if (customs.containsKey(spawn.getNpcId())) {
									continue;
								}
								SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, legionDominionId,
										race.getLegionDominionRace(), mod.getLegionDominionModType());
								indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
								legionDominionSpawnGroups.add(spawnGroup);
							}
						}
					}
				}
				for (BaseSpawn BaseSpawn : spawnMap.getBaseSpawns()) {
					int baseId = BaseSpawn.getId();
					List<SpawnGroup2> baseSpawnGroups = spawnGroupsFor(baseSpawnMaps, baseId);
					for (BaseSpawn.SimpleRaceTemplate simpleRace : BaseSpawn.getBaseRaceTemplates()) {
						for (Spawn spawn : simpleRace.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, baseId, simpleRace.getBaseRace());
							indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
							baseSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (OutpostSpawn OutpostSpawn : spawnMap.getOutpostSpawns()) {
					int outpostId = OutpostSpawn.getId();
					List<SpawnGroup2> outpostSpawnGroups = spawnGroupsFor(outpostSpawnMaps, outpostId);
					for (OutpostSpawn.SimpleRaceTemplate simpleRace : OutpostSpawn.getOutpostRaceTemplates()) {
						for (Spawn spawn : simpleRace.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, outpostId, simpleRace.getBaseRace(),
									0);
							indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
							outpostSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (RiftSpawn rift : spawnMap.getRiftSpawns()) {
					int id = rift.getId();
					List<SpawnGroup2> riftSpawnGroups = spawnGroupsFor(riftSpawnMaps, id);
					for (Spawn spawn : rift.getSpawns()) {
						if (spawn.isCustom()) {
							if (worldSpawns.containsKey(spawn.getNpcId())) {
								worldSpawns.remove(spawn.getNpcId());
							}
							customs.put(spawn.getNpcId(), spawn);
						} else if (customs.containsKey(spawn.getNpcId())) {
							continue;
						}
						SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id);
						indexWorldSpawn(mapId, worldSpawns, spawnGroup, spawn);
						riftSpawnGroups.add(spawnGroup);
					}
				}
				for (VortexSpawn VortexSpawn : spawnMap.getVortexSpawns()) {
					int id = VortexSpawn.getId();
					List<SpawnGroup2> vortexSpawnGroups = spawnGroupsFor(vortexSpawnMaps, id);
					for (VortexSpawn.VortexStateTemplate type : VortexSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getStateType());
							vortexSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (BeritraSpawn BeritraSpawn : spawnMap.getBeritraSpawns()) {
					int id = BeritraSpawn.getId();
					List<SpawnGroup2> beritraSpawnGroups = spawnGroupsFor(beritraSpawnMaps, id);
					for (BeritraSpawn.BeritraStateTemplate type : BeritraSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getBeritraType());
							beritraSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (AgentSpawn AgentSpawn : spawnMap.getAgentSpawns()) {
					int id = AgentSpawn.getId();
					List<SpawnGroup2> agentSpawnGroups = spawnGroupsFor(agentSpawnMaps, id);
					for (AgentSpawn.AgentStateTemplate type : AgentSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getAgentType());
							agentSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (AnohaSpawn AnohaSpawn : spawnMap.getAnohaSpawns()) {
					int id = AnohaSpawn.getId();
					List<SpawnGroup2> anohaSpawnGroups = spawnGroupsFor(anohaSpawnMaps, id);
					for (AnohaSpawn.AnohaStateTemplate type : AnohaSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getAnohaType());
							anohaSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (ConquestSpawn ConquestSpawn : spawnMap.getConquestSpawns()) {
					int id = ConquestSpawn.getId();
					List<SpawnGroup2> conquestSpawnGroups = spawnGroupsFor(conquestSpawnMaps, id);
					for (ConquestSpawn.ConquestStateTemplate type : ConquestSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getConquestType());
							conquestSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (SvsSpawn SvsSpawn : spawnMap.getSvsSpawns()) {
					int id = SvsSpawn.getId();
					List<SpawnGroup2> svsSpawnGroups = spawnGroupsFor(svsSpawnMaps, id);
					for (SvsSpawn.SvsStateTemplate type : SvsSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getSvsType());
							svsSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (RvrSpawn RvrSpawn : spawnMap.getRvrSpawns()) {
					int id = RvrSpawn.getId();
					List<SpawnGroup2> rvrSpawnGroups = spawnGroupsFor(rvrSpawnMaps, id);
					for (RvrSpawn.RvrStateTemplate type : RvrSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getRvrType());
							rvrSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (IuSpawn IuSpawn : spawnMap.getIuSpawns()) {
					int id = IuSpawn.getId();
					List<SpawnGroup2> iuSpawnGroups = spawnGroupsFor(iuSpawnMaps, id);
					for (IuSpawn.IuStateTemplate type : IuSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getIuType());
							iuSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (DynamicRiftSpawn DynamicRiftSpawn : spawnMap.getDynamicRiftSpawns()) {
					int id = DynamicRiftSpawn.getId();
					List<SpawnGroup2> dynamicRiftSpawnGroups = spawnGroupsFor(dynamicRiftSpawnMaps, id);
					for (DynamicRiftSpawn.DynamicRiftStateTemplate type : DynamicRiftSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getDynamicRiftType());
							dynamicRiftSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (InstanceRiftSpawn InstanceRiftSpawn : spawnMap.getInstanceRiftSpawns()) {
					int id = InstanceRiftSpawn.getId();
					List<SpawnGroup2> instanceRiftSpawnGroups = spawnGroupsFor(instanceRiftSpawnMaps, id);
					for (InstanceRiftSpawn.InstanceRiftStateTemplate type : InstanceRiftSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getInstanceRiftType());
							instanceRiftSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (NightmareCircusSpawn NightmareCircusSpawn : spawnMap.getNightmareCircusSpawns()) {
					int id = NightmareCircusSpawn.getId();
					List<SpawnGroup2> nightmareCircusSpawnGroups = spawnGroupsFor(nightmareCircusSpawnMaps, id);
					for (NightmareCircusSpawn.NightmareCircusStateTemplate type : NightmareCircusSpawn
							.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getNightmareCircusType());
							nightmareCircusSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (IdianDepthsSpawn IdianDepthsSpawn : spawnMap.getIdianDepthsSpawns()) {
					int id = IdianDepthsSpawn.getId();
					List<SpawnGroup2> idianDepthsSpawnGroups = spawnGroupsFor(idianDepthsSpawnMaps, id);
					for (IdianDepthsSpawn.IdianDepthsStateTemplate type : IdianDepthsSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getIdianDepthsType());
							idianDepthsSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (ZorshivDredgionSpawn ZorshivDredgionSpawn : spawnMap.getZorshivDredgionSpawns()) {
					int id = ZorshivDredgionSpawn.getId();
					List<SpawnGroup2> zorshivDredgionSpawnGroups = spawnGroupsFor(zorshivDredgionSpawnMaps, id);
					for (ZorshivDredgionSpawn.ZorshivDredgionStateTemplate type : ZorshivDredgionSpawn
							.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getZorshivDredgionType());
							zorshivDredgionSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (MoltenusSpawn MoltenusSpawn : spawnMap.getMoltenusSpawns()) {
					int id = MoltenusSpawn.getId();
					List<SpawnGroup2> moltenusSpawnGroups = spawnGroupsFor(moltenusSpawnMaps, id);
					for (MoltenusSpawn.MoltenusStateTemplate type : MoltenusSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getMoltenusType());
							moltenusSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (LandingSpawn LandingSpawn : spawnMap.getLandingSpawns()) {
					int id = LandingSpawn.getId();
					List<SpawnGroup2> landingSpawnGroups = spawnGroupsFor(landingSpawnMaps, id);
					for (LandingSpawn.LandingStateTemplate type : LandingSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getLandingType());
							landingSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (LandingSpecialSpawn LandingSpecialSpawn : spawnMap.getLandingSpecialSpawns()) {
					int id = LandingSpecialSpawn.getId();
					List<SpawnGroup2> landingSpecialSpawnGroups = spawnGroupsFor(landingSpecialSpawnMaps, id);
					for (LandingSpecialSpawn.LandingSpStateTemplate type : LandingSpecialSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getLandingSpecialType());
							landingSpecialSpawnGroups.add(spawnGroup);
						}
					}
				}
				for (TowerOfEternitySpawn TowerOfEternitySpawn : spawnMap.getTowerOfEternitySpawns()) {
					int id = TowerOfEternitySpawn.getId();
					List<SpawnGroup2> towerOfEternitySpawnGroups = spawnGroupsFor(towerOfEternitySpawnMaps, id);
					for (TowerOfEternitySpawn.TowerOfEternityStateTemplate type : TowerOfEternitySpawn
							.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (worldSpawns.containsKey(spawn.getNpcId())) {
									worldSpawns.remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId())) {
								continue;
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getTowerOfEternityType());
							towerOfEternitySpawnGroups.add(spawnGroup);
						}
					}
				}
				customs.clear();
			}
		}
	}

	/**
	 * 释放 JAXB 原始模板列表以降低内存占用。
	 * Clears the raw JAXB template list to reduce memory use.
	 */
	public void clearTemplates() {
		if (templates != null) {
			templates.clear();
			templates = null;
		}
	}

	/**
	 * 从目录加载全部刷怪 XML（使用目录内 spawns.xsd）。
	 * Loads all spawn XML files from a directory using its spawns.xsd schema.
	 *
	 * @param directory 刷怪数据目录 / spawn data directory
	 * @return 已索引的刷怪数据 / indexed spawn data
	 * on load or validation failure。
	 */
	public static SpawnsData2 load(File directory) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new File(directory, "spawns.xsd"));
		return load(directory, schema);
	}

	/**
	 * 使用指定 Schema 从目录加载刷怪 XML 并完成索引。
	 * Loads spawn XML from a directory with the given schema and builds indexes.
	 *
	 * @param directory 刷怪数据目录 / spawn data directory
	 * @param schema XSD Schema
	 * @return 已索引的刷怪数据 / indexed spawn data
	 * on load failure
	 */
	static SpawnsData2 load(File directory, Schema schema) throws Exception {
		JAXBContext context = JAXBContext.newInstance(SpawnsData2.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(schema);
		List<SpawnMap> maps = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(directory.toPath())) {
			for (Path path : paths.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".xml"))
					.filter(p -> !p.getFileName().toString().startsWith("new"))
					.sorted().toList()) {
				SpawnsData2 data = (SpawnsData2) unmarshaller.unmarshal(path.toFile());
				if (data.templates != null) {
					maps.addAll(data.templates);
				}
			}
		}
		SpawnsData2 data = new SpawnsData2();
		data.templates = maps;
		data.afterUnmarshal(null, null);
		data.clearTemplates();
		return data;
	}

	/**
	 * 按世界地图 ID 获取全部刷怪组。
	 * Returns all spawn groups for the given world map id.
	 *
	 * world map id
	 *
	 * @param worldId
	 * @return 刷怪组列表，不存在则为空列表 / spawn groups, or empty list
	 */
	public List<SpawnGroup2> getSpawnsByWorldId(int worldId) {
		if (!allSpawnGroups.containsKey(worldId)) {
			return Collections.emptyList();
		}
		return new ArrayList<>(allSpawnGroups.get(worldId).values());
	}

	/**
	 * 按世界与 NPC ID 获取刷怪定义。
	 * Returns the spawn definition for the given world and npc id.
	 *
	 * world map id
	 * NPC 模板 ID / npc template id
	 * @return 刷怪定义，不存在则为 null / spawn or null
	 */
	public Spawn getSpawnsForNpc(int worldId, int npcId) {
		if (!allSpawnMaps.containsKey(worldId) || !allSpawnMaps.get(worldId).containsKey(npcId)) {
			return null;
		}
		return allSpawnMaps.get(worldId).get(npcId).getValue();
	}

	/**
	 * 按地点 ID 获取攻城据点刷怪组列表。
	 * Returns siege location spawn groups for the given location id.
	 *
	 * siege location id
	 *
	 * @param siegeId
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getSiegeSpawnsByLocId(int siegeId) {
		return siegeSpawnMaps.get(siegeId);
	}

	/**
	 * 按地点 ID 获取军团领地刷怪组列表。
	 * Returns legion dominion spawn groups for the given location id.
	 *
	 * legion dominion id
	 *
	 * @param legionDominionId
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getLegionDominionSpawnsByLocId(int legionDominionId) {
		return legionDominionSpawnMaps.get(legionDominionId);
	}

	/**
	 * 按地点 ID 获取基地刷怪组列表。
	 * Returns base spawn groups for the given location id.
	 *
	 * @param id 基地 ID / base id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getBaseSpawnsByLocId(int id) {
		return baseSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取前哨刷怪组列表。
	 * Returns outpost spawn groups for the given location id.
	 *
	 * @param id 前哨 ID / outpost id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getOutpostSpawnsByLocId(int id) {
		return outpostSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取裂隙刷怪组列表。
	 * Returns rift spawn groups for the given location id.
	 *
	 * @param id 裂隙 ID / rift id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getRiftSpawnsByLocId(int id) {
		return riftSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取次元漩涡刷怪组列表。
	 * Returns vortex spawn groups for the given location id.
	 *
	 * @param id 漩涡 ID / vortex id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getVortexSpawnsByLocId(int id) {
		return vortexSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取贝里特拉入侵刷怪组列表。
	 * Returns beritra invasion spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getBeritraSpawnsByLocId(int id) {
		return beritraSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取代理人战斗刷怪组列表。
	 * Returns agent fight spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getAgentSpawnsByLocId(int id) {
		return agentSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取阿诺哈刷怪组列表。
	 * Returns anoha spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getAnohaSpawnsByLocId(int id) {
		return anohaSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取征服刷怪组列表。
	 * Returns conquest spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getConquestSpawnsByLocId(int id) {
		return conquestSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取 Svs 刷怪组列表。
	 * Returns svs spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getSvsSpawnsByLocId(int id) {
		return svsSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取 Rvr 刷怪组列表。
	 * Returns rvr spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getRvrSpawnsByLocId(int id) {
		return rvrSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取 IU 刷怪组列表。
	 * Returns iu spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getIuSpawnsByLocId(int id) {
		return iuSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取动态裂隙刷怪组列表。
	 * Returns dynamic rift spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getDynamicRiftSpawnsByLocId(int id) {
		return dynamicRiftSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取副本裂隙刷怪组列表。
	 * Returns instance rift spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getInstanceRiftSpawnsByLocId(int id) {
		return instanceRiftSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取梦魇马戏团刷怪组列表。
	 * Returns nightmare circus spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getNightmareCircusSpawnsByLocId(int id) {
		return nightmareCircusSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取伊迪安深渊刷怪组列表。
	 * Returns idian depths spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getIdianDepthsSpawnsByLocId(int id) {
		return idianDepthsSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取佐尔希夫无畏舰刷怪组列表。
	 * Returns zorshiv dredgion spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getZorshivDredgionSpawnsByLocId(int id) {
		return zorshivDredgionSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取熔岩领主刷怪组列表。
	 * Returns moltenus spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getMoltenusSpawnsByLocId(int id) {
		return moltenusSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取登陆点刷怪组列表。
	 * Returns landing spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getLandingSpawnsByLocId(int id) {
		return landingSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取特殊登陆点刷怪组列表。
	 * Returns special landing spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getLandingSpecialSpawnsByLocId(int id) {
		return landingSpecialSpawnMaps.get(id);
	}

	/**
	 * 按地点 ID 获取永恒之塔刷怪组列表。
	 * Returns tower of eternity spawn groups for the given location id.
	 *
	 * @param id 地点 ID / location id
	 * @return 刷怪组列表，可能为 null / spawn groups or null
	 */
	public List<SpawnGroup2> getTowerOfEternitySpawnsByLocId(int id) {
		return towerOfEternitySpawnMaps.get(id);
	}

	/**
	 * 将可见对象的位置保存为自定义刷怪 XML，或删除对应点位。
	 * Saves a visible object's position as custom spawn XML, or deletes the matching spot.
	 *
	 * @param admin 操作管理员 / admin player
	 * target visible object
	 *
	 * @param delete true 表示删除点位 / true to delete the spot
	 * @param visibleObject
	 * @return 是否保存成功 / whether the save succeeded
	 * @param delete
	 * @throws IOException 文件读写失败 / on file I/O failure
	 */
	public synchronized boolean saveSpawn(Player admin, VisibleObject visibleObject, boolean delete)
			throws IOException {
		SpawnTemplate spawn = visibleObject.getSpawn();
		Spawn oldGroup = DataManager.SPAWNS_DATA2.getSpawnsForNpc(visibleObject.getWorldId(), spawn.getNpcId());

		File xml = Config.dataFile("./data/static_data/spawns/" + getRelativePath(visibleObject));
		SpawnsData2 data = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		JAXBContext jc = null;
		boolean addGroup = false;

		try {
			schema = sf.newSchema(Config.dataFile("./data/static_data/spawns/spawns.xsd"));
			jc = JAXBContext.newInstance(SpawnsData2.class);
		} catch (Exception e) {
			// 忽略；若 schema 错误，甚至无法调用该命令。 / ignore, if schemas are wrong then we even could not call the command;
		}

		FileInputStream fin = null;
		if (xml.exists()) {
			try {
				fin = new FileInputStream(xml);
				Unmarshaller unmarshaller = jc.createUnmarshaller();
				unmarshaller.setSchema(schema);
				data = (SpawnsData2) unmarshaller.unmarshal(fin);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				PacketSendUtility.sendMessage(admin, "Could not load old XML file!");
				return false;
			} finally {
				if (fin != null) {
					fin.close();
				}
			}
		}

		if (oldGroup == null || oldGroup.isCustom()) {
			if (data == null) {
				data = new SpawnsData2();
			}
			oldGroup = data.getSpawnsForNpc(visibleObject.getWorldId(), spawn.getNpcId());
			if (oldGroup == null) {
				oldGroup = new Spawn(spawn.getNpcId(), spawn.getRespawnTime(), spawn.getHandlerType());
				addGroup = true;
			}
		} else {
			if (data == null) {
				data = DataManager.SPAWNS_DATA2;
			}
			// 仅从内存移除，稍后会重新加入 / only remove from memory, will be added back later
			allSpawnMaps.get(visibleObject.getWorldId()).remove(spawn.getNpcId());
			addGroup = true;
		}

		SpawnSpotTemplate spot = new SpawnSpotTemplate(visibleObject.getX(), visibleObject.getY(), visibleObject.getZ(),
				visibleObject.getHeading(), visibleObject.getSpawn().getRandomWalk(),
				visibleObject.getSpawn().getWalkerId(), visibleObject.getSpawn().getWalkerIndex());
		boolean changeX = visibleObject.getX() != spawn.getX();
		boolean changeY = visibleObject.getY() != spawn.getY();
		boolean changeZ = visibleObject.getZ() != spawn.getZ();
		boolean changeH = visibleObject.getHeading() != spawn.getHeading();
		if (changeH && visibleObject instanceof Npc) {
			Npc npc = (Npc) visibleObject;
			if (!npc.isAtSpawnLocation() || !npc.isInState(CreatureState.NPC_IDLE) || changeX || changeY || changeZ) {
				// 若 H 改变，XSD 校验失败，因其可能为负；因此重置。 / if H changed, XSD validation fails, because it may be negative; thus, reset
				// 把它还回 / it back
				visibleObject.setXYZH(null, null, null, spawn.getHeading());
				changeH = false;
			}
		}

		SpawnSpotTemplate oldSpot = null;
		for (SpawnSpotTemplate s : oldGroup.getSpawnSpotTemplates()) {
			if (s.getX() == spot.getX() && s.getY() == spot.getY() && s.getZ() == spot.getZ()
					&& s.getHeading() == spot.getHeading()) {
				if (delete || !Objects.equals(s.getWalkerId(), spot.getWalkerId())) {
					oldSpot = s;
					break;
				} else {
					return false; // nothing to change
				}
			} else if (changeX && s.getY() == spot.getY() && s.getZ() == spot.getZ()
					&& s.getHeading() == spot.getHeading()
					|| changeY && s.getX() == spot.getX() && s.getZ() == spot.getZ()
							&& s.getHeading() == spot.getHeading()
					|| changeZ && s.getX() == spot.getX() && s.getY() == spot.getY()
							&& s.getHeading() == spot.getHeading()
					|| changeH && s.getX() == spot.getX() && s.getY() == spot.getY() && s.getZ() == spot.getZ()) {
				oldSpot = s;
				break;
			}
		}

		if (oldSpot != null) {
			oldGroup.getSpawnSpotTemplates().remove(oldSpot);
		}
		if (!delete) {
			oldGroup.addSpawnSpot(spot);
		}
		oldGroup.setCustom(true);

		SpawnMap map = null;
		if (data.templates == null) {
			data.templates = new ArrayList<SpawnMap>();
			map = new SpawnMap(spawn.getWorldId());
			data.templates.add(map);
		} else {
			map = data.templates.get(0);
		}

		if (addGroup) {
			map.addSpawns(oldGroup);
		}
		FileOutputStream fos = null;
		try {
			xml.getParentFile().mkdir();
			fos = new FileOutputStream(xml);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(data, fos);
			DataManager.SPAWNS_DATA2.templates = data.templates;
			DataManager.SPAWNS_DATA2.afterUnmarshal(null, null);
			DataManager.SPAWNS_DATA2.clearTemplates();
			data.clearTemplates();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			PacketSendUtility.sendMessage(admin, "Could not save XML file!");
			return false;
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		return true;
	}

	/**
	 * 计算自定义刷怪 XML 相对 spawns 目录的路径。
	 * Computes the relative path under the spawns directory for a custom spawn XML.
	 *
	 * target visible object
	 * relative path
	 */
	String getRelativePath(VisibleObject visibleObject) {
		String path;
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(visibleObject.getWorldId());
		if (visibleObject.getSpawn().getHandlerType() == SpawnHandlerType.RIFT) {
			path = "Rifts";
		}
		if (visibleObject.getSpawn().getHandlerType() == SpawnHandlerType.VOLATILE_RIFT) {
			path = "Volatile Rifts";
		} else if (visibleObject instanceof Gatherable) {
			path = "Gather";
		} else if (map.isInstanceType()) {
			path = "Instances";
		} else {
			path = "Npcs";
		}
		return path + "/New/" + visibleObject.getWorldId() + "_" + map.getName().replace(' ', '_') + ".xml";
	}

	/**
	 * 返回已索引的世界地图数量。
	 * Returns the number of indexed world maps.
	 *
	 * map count
	 */
	public int size() {
		return allSpawnMaps.size();
	}

	/**
	 * 按 NPC ID 查找第一个刷怪点；优先在给定世界中查找，否则遍历其它地图。
	 * Finds the first spawn spot for an npc id, searching the given world first then other maps.
	 *
	 * @param worldId 优先搜索的世界 ID / world id to search first (optional preference)
	 * NPC 模板 ID / npc template id
	 *
	 * @return 搜索结果，未找到则为 null / search result or null
	 */
	public SpawnSearchResult getFirstSpawnByNpcId(int worldId, int npcId) {
		Spawn spawns = DataManager.SPAWNS_DATA2.getSpawnsForNpc(worldId, npcId);

		if (spawns == null || spawns.getSpawnSpotTemplates().isEmpty()) {
			spawns = null;
			for (WorldMapTemplate template : DataManager.WORLD_MAPS_DATA) {
				if (template.getMapId() == worldId) {
					continue;
				}
				Spawn candidate = DataManager.SPAWNS_DATA2.getSpawnsForNpc(template.getMapId(), npcId);
				if (candidate != null && !candidate.getSpawnSpotTemplates().isEmpty()) {
					spawns = candidate;
					worldId = template.getMapId();
					break;
				}
			}
			if (spawns == null) {
				return null;
			}
		}
		return new SpawnSearchResult(worldId, spawns.getSpawnSpotTemplates().get(0));
	}

	/**
	 * 追加一张刷怪地图（供活动服务注入额外刷怪）。
	 * Appends a spawn map (used by the event service to inject extra spawns).
	 *
	 * @param spawnMap 要追加的刷怪地图 / spawn map to add
	 */
	public void addNewSpawnMap(SpawnMap spawnMap) {
		if (templates == null) {
			templates = new ArrayList<SpawnMap>();
		}
		templates.add(spawnMap);
	}

	/**
	 * 移除活动刷怪对象对应的内存索引条目。
	 * Removes in-memory index entries for the given event spawn objects.
	 *
	 * @param objects 可见对象列表 / visible objects to remove
	 */
	public void removeEventSpawnObjects(List<VisibleObject> objects) {
		for (VisibleObject visObj : objects) {
			if (!allSpawnMaps.contains(visObj.getWorldId())) {
				continue;
			}
			SimpleEntry<SpawnGroup2, Spawn> entry = allSpawnMaps.get(visObj.getWorldId())
					.get(visObj.getObjectTemplate().getTemplateId());
			if (!entry.getValue().isEventSpawn()) {
				continue;
			}
			if (entry.getValue().getEventTemplate().equals(visObj.getSpawn().getEventTemplate())) {
				allSpawnMaps.get(visObj.getWorldId()).remove(visObj.getObjectTemplate().getTemplateId());
				if (allSpawnGroups.containsKey(visObj.getWorldId())) {
					allSpawnGroups.get(visObj.getWorldId()).remove(entry.getValue());
				}
			}
		}
	}

	/**
	 * 返回 JAXB 原始刷怪地图模板列表。
	 * Returns the raw JAXB spawn-map template list.
	 *
	 * @return 刷怪地图列表 / spawn map list
	 */
	public List<SpawnMap> getTemplates() {
		return templates;
	}
}
