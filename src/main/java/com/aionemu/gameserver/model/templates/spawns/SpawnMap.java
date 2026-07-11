package com.aionemu.gameserver.model.templates.spawns;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

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

/**
 * 刷新点地图模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "SpawnMap")
public class SpawnMap {
	@XmlElement(name = "spawn")
	private List<Spawn> spawns;

	@XmlElement(name = "siege_spawn")
	private List<SiegeSpawn> siegeSpawns;

	@XmlElement(name = "legion_dominion_spawn")
	private List<LegionDominionSpawn> legionDominionSpawns;

	@XmlElement(name = "base_spawn")
	private List<BaseSpawn> baseSpawns;

	@XmlElement(name = "outpost_spawn")
	private List<OutpostSpawn> outpostSpawns;

	@XmlElement(name = "rift_spawn")
	private List<RiftSpawn> riftSpawns;

	@XmlElement(name = "vortex_spawn")
	private List<VortexSpawn> vortexSpawns;

	@XmlElement(name = "beritra_spawn")
	private List<BeritraSpawn> beritraSpawns;

	@XmlElement(name = "agent_spawn")
	private List<AgentSpawn> agentSpawns;

	@XmlElement(name = "anoha_spawn")
	private List<AnohaSpawn> anohaSpawns;

	@XmlElement(name = "conquest_spawn")
	private List<ConquestSpawn> conquestSpawns;

	@XmlElement(name = "svs_spawn")
	private List<SvsSpawn> svsSpawns;

	@XmlElement(name = "rvr_spawn")
	private List<RvrSpawn> rvrSpawns;

	@XmlElement(name = "iu_spawn")
	private List<IuSpawn> iuSpawns;

	@XmlElement(name = "moltenus_spawn")
	private List<MoltenusSpawn> moltenusSpawns;

	@XmlElement(name = "dynamic_rift_spawn")
	private List<DynamicRiftSpawn> dynamicRiftSpawns;

	@XmlElement(name = "instance_rift_spawn")
	private List<InstanceRiftSpawn> instanceRiftSpawns;

	@XmlElement(name = "nightmare_circus_spawn")
	private List<NightmareCircusSpawn> nightmareCircusSpawns;

	@XmlElement(name = "idian_depths_spawn")
	private List<IdianDepthsSpawn> idianDepthsSpawns;

	@XmlElement(name = "zorshiv_dredgion_spawn")
	private List<ZorshivDredgionSpawn> zorshivDredgionSpawns;

	@XmlElement(name = "landing_spawn")
	private List<LandingSpawn> landingSpawns;

	@XmlElement(name = "landing_special_spawn")
	private List<LandingSpecialSpawn> landingSpecialSpawns;

	@XmlElement(name = "tower_of_eternity_spawn")
	private List<TowerOfEternitySpawn> towerOfEternitySpawns;

	@XmlAttribute(name = "map_id")
	private int mapId;

	public SpawnMap() {
	}

	public SpawnMap(int mapId) {
		this.mapId = mapId;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapId;
	}

	/** 获取刷新。 / Returns the spawns. */
	public List<Spawn> getSpawns() {
		if (spawns == null) {
			spawns = new ArrayList<Spawn>();
		}
		return spawns;
	}

	/** 添加刷新。 / Adds spawns. */
	public void addSpawns(Spawn spawns) {
		getSpawns().add(spawns);
	}

	/** 移除刷新。 / Removes spawns. */
	public void removeSpawns(Spawn spawns) {
		getSpawns().remove(spawns);
	}

	/** 获取要塞刷新。 / Returns the siege spawns. */
	public List<SiegeSpawn> getSiegeSpawns() {
		if (siegeSpawns == null) {
			siegeSpawns = new ArrayList<SiegeSpawn>();
		}
		return siegeSpawns;
	}

	/** 获取军团领地刷新。 / Returns the legion dominion spawns. */
	public List<LegionDominionSpawn> getLegionDominionSpawns() {
		if (legionDominionSpawns == null) {
			legionDominionSpawns = new ArrayList<LegionDominionSpawn>();
		}
		return legionDominionSpawns;
	}

	/** 获取基础刷新。 / Returns the base spawns. */
	public List<BaseSpawn> getBaseSpawns() {
		if (baseSpawns == null) {
			baseSpawns = new ArrayList<BaseSpawn>();
		}
		return baseSpawns;
	}

	/** 获取前哨刷新。 / Returns the outpost spawns. */
	public List<OutpostSpawn> getOutpostSpawns() {
		if (outpostSpawns == null) {
			outpostSpawns = new ArrayList<OutpostSpawn>();
		}
		return outpostSpawns;
	}

	/** 获取裂隙刷新。 / Returns the rift spawns. */
	public List<RiftSpawn> getRiftSpawns() {
		if (riftSpawns == null) {
			riftSpawns = new ArrayList<RiftSpawn>();
		}
		return riftSpawns;
	}

	/** 获取漩涡刷新。 / Returns the vortex spawns. */
	public List<VortexSpawn> getVortexSpawns() {
		if (vortexSpawns == null) {
			vortexSpawns = new ArrayList<VortexSpawn>();
		}
		return vortexSpawns;
	}

	/** 获取贝里特拉刷新。 / Returns the beritra spawns. */
	public List<BeritraSpawn> getBeritraSpawns() {
		if (beritraSpawns == null) {
			beritraSpawns = new ArrayList<BeritraSpawn>();
		}
		return beritraSpawns;
	}

	/** 获取代理人刷新。 / Returns the agent spawns. */
	public List<AgentSpawn> getAgentSpawns() {
		if (agentSpawns == null) {
			agentSpawns = new ArrayList<AgentSpawn>();
		}
		return agentSpawns;
	}

	/** 获取阿诺哈刷新。 / Returns the anoha spawns. */
	public List<AnohaSpawn> getAnohaSpawns() {
		if (anohaSpawns == null) {
			anohaSpawns = new ArrayList<AnohaSpawn>();
		}
		return anohaSpawns;
	}

	/** 获取征服刷新。 / Returns the conquest spawns. */
	public List<ConquestSpawn> getConquestSpawns() {
		if (conquestSpawns == null) {
			conquestSpawns = new ArrayList<ConquestSpawn>();
		}
		return conquestSpawns;
	}

	/** 获取势力战刷新。 / Returns the svs spawns. */
	public List<SvsSpawn> getSvsSpawns() {
		if (svsSpawns == null) {
			svsSpawns = new ArrayList<SvsSpawn>();
		}
		return svsSpawns;
	}

	/** 获取阵营战刷新。 / Returns the rvr spawns. */
	public List<RvrSpawn> getRvrSpawns() {
		if (rvrSpawns == null) {
			rvrSpawns = new ArrayList<RvrSpawn>();
		}
		return rvrSpawns;
	}

	/** 返回 iu spawns / Returns the iu spawns */
	public List<IuSpawn> getIuSpawns() {
		if (iuSpawns == null) {
			iuSpawns = new ArrayList<IuSpawn>();
		}
		return iuSpawns;
	}

	/** 获取熔岩魔刷新。 / Returns the moltenus spawns. */
	public List<MoltenusSpawn> getMoltenusSpawns() {
		if (moltenusSpawns == null) {
			moltenusSpawns = new ArrayList<MoltenusSpawn>();
		}
		return moltenusSpawns;
	}

	/** 获取动态裂隙刷新。 / Returns the dynamic rift spawns. */
	public List<DynamicRiftSpawn> getDynamicRiftSpawns() {
		if (dynamicRiftSpawns == null) {
			dynamicRiftSpawns = new ArrayList<DynamicRiftSpawn>();
		}
		return dynamicRiftSpawns;
	}

	/** 获取副本裂隙刷新。 / Returns the instance rift spawns. */
	public List<InstanceRiftSpawn> getInstanceRiftSpawns() {
		if (instanceRiftSpawns == null) {
			instanceRiftSpawns = new ArrayList<InstanceRiftSpawn>();
		}
		return instanceRiftSpawns;
	}

	/** 获取梦魇马戏团刷新。 / Returns the nightmare circus spawns. */
	public List<NightmareCircusSpawn> getNightmareCircusSpawns() {
		if (nightmareCircusSpawns == null) {
			nightmareCircusSpawns = new ArrayList<NightmareCircusSpawn>();
		}
		return nightmareCircusSpawns;
	}

	/** 获取伊迪安深渊刷新。 / Returns the idian depths spawns. */
	public List<IdianDepthsSpawn> getIdianDepthsSpawns() {
		if (idianDepthsSpawns == null) {
			idianDepthsSpawns = new ArrayList<IdianDepthsSpawn>();
		}
		return idianDepthsSpawns;
	}

	/** 获取佐希夫无畏舰刷新。 / Returns the zorshiv dredgion spawns. */
	public List<ZorshivDredgionSpawn> getZorshivDredgionSpawns() {
		if (zorshivDredgionSpawns == null) {
			zorshivDredgionSpawns = new ArrayList<ZorshivDredgionSpawn>();
		}
		return zorshivDredgionSpawns;
	}

	/** 获取登陆刷新。 / Returns the landing spawns. */
	public List<LandingSpawn> getLandingSpawns() {
		if (landingSpawns == null) {
			landingSpawns = new ArrayList<LandingSpawn>();
		}
		return landingSpawns;
	}

	/** 返回 landing special spawns / Returns the landing special spawns */
	public List<LandingSpecialSpawn> getLandingSpecialSpawns() {
		if (landingSpecialSpawns == null) {
			landingSpecialSpawns = new ArrayList<LandingSpecialSpawn>();
		}
		return landingSpecialSpawns;
	}

	/** 返回 tower of eternity spawns / Returns the tower of eternity spawns */
	public List<TowerOfEternitySpawn> getTowerOfEternitySpawns() {
		if (towerOfEternitySpawns == null) {
			towerOfEternitySpawns = new ArrayList<TowerOfEternitySpawn>();
		}
		return towerOfEternitySpawns;
	}

	/** 添加要塞刷新。 / Adds siege spawns. */
	public void addSiegeSpawns(SiegeSpawn spawns) {
		getSiegeSpawns().add(spawns);
	}

	/** 添加基础刷新。 / Adds base spawns. */
	public void addBaseSpawns(BaseSpawn spawns) {
		getBaseSpawns().add(spawns);
	}

	/** 添加裂隙刷新。 / Adds rift spawns. */
	public void addRiftSpawns(RiftSpawn spawns) {
		getRiftSpawns().add(spawns);
	}

	/** 添加漩涡刷新。 / Adds vortex spawns. */
	public void addVortexSpawns(VortexSpawn spawns) {
		getVortexSpawns().add(spawns);
	}

	/** 添加贝里特拉刷新。 / Adds beritra spawns. */
	public void addBeritraSpawns(BeritraSpawn spawns) {
		getBeritraSpawns().add(spawns);
	}

	/** 添加代理人刷新。 / Adds agent spawns. */
	public void addAgentSpawns(AgentSpawn spawns) {
		getAgentSpawns().add(spawns);
	}

	/** 添加阿诺哈刷新。 / Adds anoha spawns. */
	public void addAnohaSpawns(AnohaSpawn spawns) {
		getAnohaSpawns().add(spawns);
	}

	/** 添加征服刷新。 / Adds conquest spawns. */
	public void addConquestSpawns(ConquestSpawn spawns) {
		getConquestSpawns().add(spawns);
	}

	/** 添加势力战刷新。 / Adds svs spawns. */
	public void addSvsSpawns(SvsSpawn spawns) {
		getSvsSpawns().add(spawns);
	}

	/** 添加阵营战刷新。 / Adds rvr spawns. */
	public void addRvrSpawns(RvrSpawn spawns) {
		getRvrSpawns().add(spawns);
	}

	/** 添加 iu spawns / Adds iu spawns */
	public void addIuSpawns(IuSpawn spawns) {
		getIuSpawns().add(spawns);
	}

	/** 添加熔岩魔刷新。 / Adds moltenus spawns. */
	public void addMoltenusSpawns(MoltenusSpawn spawns) {
		getMoltenusSpawns().add(spawns);
	}

	/** 添加动态裂隙刷新。 / Adds dynamic rift spawns. */
	public void addDynamicRiftSpawns(DynamicRiftSpawn spawns) {
		getDynamicRiftSpawns().add(spawns);
	}

	/** 添加副本裂隙刷新。 / Adds instance rift spawns. */
	public void addInstanceRiftSpawns(InstanceRiftSpawn spawns) {
		getInstanceRiftSpawns().add(spawns);
	}

	/** 添加梦魇马戏团刷新。 / Adds nightmare circus spawns. */
	public void addNightmareCircusSpawns(NightmareCircusSpawn spawns) {
		getNightmareCircusSpawns().add(spawns);
	}

	/** 添加伊迪安深渊刷新。 / Adds idian depths spawns. */
	public void addIdianDepthsSpawns(IdianDepthsSpawn spawns) {
		getIdianDepthsSpawns().add(spawns);
	}

	/** 添加佐希夫无畏舰刷新。 / Adds zorshiv dredgion spawns. */
	public void addZorshivDredgionSpawns(ZorshivDredgionSpawn spawns) {
		getZorshivDredgionSpawns().add(spawns);
	}

	/** 添加登陆刷新。 / Adds landing spawns. */
	public void addLandingSpawns(LandingSpawn spawns) {
		getLandingSpawns().add(spawns);
	}

	/** 添加 landing special spawns / Adds landing special spawns */
	public void addLandingSpecialSpawns(LandingSpecialSpawn spawns) {
		getLandingSpecialSpawns().add(spawns);
	}

	/** 添加 tower of eternity spawns / Adds tower of eternity spawns */
	public void addTowerOfEternitySpawns(TowerOfEternitySpawn spawns) {
		getTowerOfEternitySpawns().add(spawns);
	}
}
