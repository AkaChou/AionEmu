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
import lombok.Getter;

/**
 * 要塞位置模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "siegelocation")
public class SiegeLocationTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;

	/** 获取类型。 / Returns the type. */
	@XmlAttribute(name = "type")
	protected SiegeType type;

	@XmlAttribute(name = "world")
	protected int world;

	@XmlElement(name = "artifact_activation")
	protected ArtifactActivation artifactActivation;

	@XmlElement(name = "door_repair")
	protected DoorRepair doorRepair;

	/** 获取要塞奖励。 / Returns the siege rewards. */
	@XmlElement(name = "siege_reward")
	protected List<SiegeReward> siegeRewards;

	/** 获取要塞军团奖励。 / Returns the siege legion rewards. */
	@XmlElement(name = "legion_reward")
	protected List<SiegeLegionReward> siegeLegionRewards;

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "name_id")
	protected int nameId = 0;

	/** 返回增益 ID / Returns the buff id */
	@XmlAttribute(name = "buff_id")
	protected int buffId = 0;

	/** 返回增益 ID / Returns the buff id a */
	@XmlAttribute(name = "buff_idA")
	protected int buffIdA = 0;

	/** 返回增益 ID E / Returns the buff id e */
	@XmlAttribute(name = "buff_idE")
	protected int buffIdE = 0;

	/** 返回所有者荣耀点 / Returns the owner gp */
	@XmlAttribute(name = "owner_gp")
	protected int ownerGp = 0;

	/** 返回 repeat count / Returns the repeat count */
	@XmlAttribute(name = "repeat_count")
	protected int repeatCount = 1;

	/** 返回 repeat interval / Returns the repeat interval */
	@XmlAttribute(name = "repeat_interval")
	protected int repeatInterval = 1;

	/** 返回攻城时长 / Returns the siege duration */
	@XmlAttribute(name = "siege_duration")
	protected int siegeDuration;

	/** 返回影响力值 / Returns the influence value */
	@XmlAttribute(name = "influence")
	protected int influenceValue;

	/** 返回 occupy count / Returns the occupy count */
	@XmlAttribute(name = "occupy_count")
	protected int occupyCount = 0;

	@XmlList
	@XmlAttribute(name = "fortress_dependency")
	protected List<Integer> fortressDependency;

	// 露娜商店 5.0.5 / Luna Shop 5.0.5
	/** 返回 luna boost price / Returns the luna boost price */
	@XmlElement(name = "luna_boost_price")
	protected List<LunaBoostPrice> lunaBoostPrice;
	/** 获取月华传送价格。 / Returns the luna teleport price. */
	@XmlElement(name = "luna_teleport_price")
	protected List<LunaTeleportPrice> lunaTeleportPrice;
	/** 获取月华奖励。 / Returns the luna reward. */
	@XmlElement(name = "luna_reward")
	protected List<LunaReward> lunaReward;
	/** 获取月华传送。 / Returns the luna teleport. */
	@XmlElement(name = "luna_teleport")
	protected List<LunaTeleport> lunaTeleport;

	// 攻城 5.3 / Siege 5.3
	/** 返回 occupy reward light / Returns the occupy reward light */
	@XmlElement(name = "occupy_reward_light")
	protected List<OccupyRewardLight> occupyRewardLight;
	/** 返回 occupy reward dark / Returns the occupy reward dark */
	@XmlElement(name = "occupy_reward_dark")
	protected List<OccupyRewardDark> occupyRewardDark;
	/** 返回光方队长技能 / Returns the leader skill light */
	@XmlElement(name = "leader_skill_light")
	protected List<LeaderSkillLight> leaderSkillLight;
	/** 返回暗方队长技能 / Returns the leader skill dark */
	@XmlElement(name = "leader_skill_dark")
	protected List<LeaderSkillDark> leaderSkillDark;

	/** 返回 outpost id / Returns the outpost id */
	@XmlAttribute(name = "outpost_id")
	protected int outpostId;

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

	/** 返回 fortress dependency / Returns the fortress dependency */
	public List<Integer> getFortressDependency() {
		if (fortressDependency == null) {
			return Collections.emptyList();
		}
		return fortressDependency;
	}
}
