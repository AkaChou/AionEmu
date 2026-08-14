package com.aionemu.gameserver.model.templates.siegelocation;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.siege.SiegeType;

/**
 * 要塞位置模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "siegelocation")
public class SiegeLocationTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "type")
	protected SiegeType type;

	@XmlAttribute(name = "world")
	protected int world;

	@XmlElement(name = "artifact_activation")
	protected ArtifactActivation artifactActivation;

	@XmlElement(name = "door_repair")
	protected DoorRepair doorRepair;

	@XmlElement(name = "siege_reward")
	protected List<SiegeReward> siegeRewards;

	@XmlElement(name = "legion_reward")
	protected List<SiegeLegionReward> siegeLegionRewards;

	@XmlAttribute(name = "name_id")
	protected int nameId = 0;

	@XmlAttribute(name = "buff_id")
	protected int buffId = 0;

	@XmlAttribute(name = "buff_idA")
	protected int buffIdA = 0;

	@XmlAttribute(name = "buff_idE")
	protected int buffIdE = 0;

	@XmlAttribute(name = "owner_gp")
	protected int ownerGp = 0;

	@XmlAttribute(name = "repeat_count")
	protected int repeatCount = 1;

	@XmlAttribute(name = "repeat_interval")
	protected int repeatInterval = 1;

	@XmlAttribute(name = "siege_duration")
	protected int siegeDuration;

	@XmlAttribute(name = "influence")
	protected int influenceValue;

	@XmlAttribute(name = "occupy_count")
	protected int occupyCount = 0;

	@XmlList
	@XmlAttribute(name = "fortress_dependency")
	protected List<Integer> fortressDependency;

	// 露娜商店 5.0.5 / Luna Shop 5.0.5
	@XmlElement(name = "luna_boost_price")
	protected List<LunaBoostPrice> lunaBoostPrice;
	@XmlElement(name = "luna_teleport_price")
	protected List<LunaTeleportPrice> lunaTeleportPrice;
	@XmlElement(name = "luna_reward")
	protected List<LunaReward> lunaReward;
	@XmlElement(name = "luna_teleport")
	protected List<LunaTeleport> lunaTeleport;

	// 攻城 5.3 / Siege 5.3
	@XmlElement(name = "occupy_reward_light")
	protected List<OccupyRewardLight> occupyRewardLight;
	@XmlElement(name = "occupy_reward_dark")
	protected List<OccupyRewardDark> occupyRewardDark;
	@XmlElement(name = "leader_skill_light")
	protected List<LeaderSkillLight> leaderSkillLight;
	@XmlElement(name = "leader_skill_dark")
	protected List<LeaderSkillDark> leaderSkillDark;

	@XmlAttribute(name = "outpost_id")
	protected int outpostId;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取类型。 / Returns the type. */
	public SiegeType getType() {
		return this.type;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.world;
	}

	/** 返回 activation / Returns the activation */
	public ArtifactActivation getActivation() {
		return this.artifactActivation;
	}

	/** 返回修理 / Returns the repair */
	public DoorRepair getRepair() {
		return this.doorRepair;
	}

	/** 获取要塞奖励。 / Returns the siege rewards. */
	public List<SiegeReward> getSiegeRewards() {
		return this.siegeRewards;
	}

	/** 获取要塞军团奖励。 / Returns the siege legion rewards. */
	public List<SiegeLegionReward> getSiegeLegionRewards() {
		return this.siegeLegionRewards;
	}

	// 露娜商店 5.0.5 / Luna Shop 5.0.5
	/** 返回 luna boost price / Returns the luna boost price */
	public List<LunaBoostPrice> getLunaBoostPrice() {
		return this.lunaBoostPrice;
	}

	/** 获取月华传送价格。 / Returns the luna teleport price. */
	public List<LunaTeleportPrice> getLunaTeleportPrice() {
		return this.lunaTeleportPrice;
	}

	/** 获取月华奖励。 / Returns the luna reward. */
	public List<LunaReward> getLunaReward() {
		return this.lunaReward;
	}

	/** 获取月华传送。 / Returns the luna teleport. */
	public List<LunaTeleport> getLunaTeleport() {
		return this.lunaTeleport;
	}

	// 攻城 5.3 / Siege 5.3
	/** 返回 occupy reward light / Returns the occupy reward light */
	public List<OccupyRewardLight> getOccupyRewardLight() {
		return this.occupyRewardLight;
	}

	/** 返回 occupy reward dark / Returns the occupy reward dark */
	public List<OccupyRewardDark> getOccupyRewardDark() {
		return this.occupyRewardDark;
	}

	/** 返回光方队长技能 / Returns the leader skill light */
	public List<LeaderSkillLight> getLeaderSkillLight() {
		return this.leaderSkillLight;
	}

	/** 返回暗方队长技能 / Returns the leader skill dark */
	public List<LeaderSkillDark> getLeaderSkillDark() {
		return this.leaderSkillDark;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 返回增益 ID / Returns the buff id a */
	public int getBuffIdA() {
		return buffIdA;
	}

	/** 返回增益 ID E / Returns the buff id e */
	public int getBuffIdE() {
		return buffIdE;
	}

	/** 返回所有者荣耀点 / Returns the owner gp */
	public int getOwnerGp() {
		return ownerGp;
	}

	/** 返回 occupy count / Returns the occupy count */
	public int getOccupyCount() {
		return occupyCount;
	}

	/** 返回 repeat count / Returns the repeat count */
	public int getRepeatCount() {
		return repeatCount;
	}

	/** 返回 repeat interval / Returns the repeat interval */
	public int getRepeatInterval() {
		return repeatInterval;
	}

	/** 返回 fortress dependency / Returns the fortress dependency */
	public List<Integer> getFortressDependency() {
		if (fortressDependency == null) {
			return Collections.emptyList();
		}
		return fortressDependency;
	}

	/** 返回攻城时长 / Returns the siege duration */
	public int getSiegeDuration() {
		return siegeDuration;
	}

	/** 返回影响力值 / Returns the influence value */
	public int getInfluenceValue() {
		return influenceValue;
	}

	/** 返回 outpost id / Returns the outpost id */
	public int getOutpostId() {
		return outpostId;
	}
}
