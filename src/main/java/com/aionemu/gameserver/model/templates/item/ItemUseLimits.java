package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 物品使用限制模板：延迟、性别、军阶与可用区域。
 * Item use-limits template: delay, gender, rank and use area.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UseLimits")
public class ItemUseLimits {

	@XmlAttribute(name = "usedelay")
	private int useDelay;

	@XmlAttribute(name = "usedelayid")
	private int useDelayId;

	@XmlAttribute(name = "ownership_world")
	private int ownershipWorldId;

	@XmlAttribute
	private String usearea;

	@XmlAttribute(name = "gender")
	private Gender genderPermitted;

	@XmlAttribute(name = "ride_usable")
	private Boolean rideUsable;

	@XmlAttribute(name = "rank_min")
	private int minRank;

	@XmlAttribute(name = "rank_max")
	private int maxRank = AbyssRankEnum.SUPREME_COMMANDER.getId();

	/** 返回延迟 ID / Returns the delay id */
	public int getDelayId() {
		return useDelayId;
	}

	/** 设置延迟 ID / Sets the delay id */
	public void setDelayId(int delayId) {
		useDelayId = delayId;
	}

	/** 返回延迟时间 / Returns the delay time */
	public int getDelayTime() {
		return useDelay;
	}

	/** 设置延迟时间 / Sets the delay time */
	public void setDelayTime(int useDelay) {
		this.useDelay = useDelay;
	}

	/** 返回可用区域 / Returns the use area */
	public ZoneName getUseArea() {
		if (usearea == null)
			return null;
		try {
			return ZoneName.get(usearea);
		} catch (Exception e) {
		}
		return null;
	}

	/** 返回拥有者世界 / Returns the ownership world */
	public int getOwnershipWorld() {
		return ownershipWorldId;
	}

	/** 返回允许性别 / Returns the gender permitted */
	public Gender getGenderPermitted() {
		return genderPermitted;
	}

	/**
	 * 骑乘状态下是否可用。
	 * Whether usable while riding.
	 *
	 * @return 是否可骑乘使用 / Whether ride usable
	 */
	public boolean isRideUsable() {
		if (rideUsable == null) {
			return false;
		}
		return rideUsable;
	}

	/** 返回最小军阶 / Returns the min rank */
	public int getMinRank() {
		return minRank;
	}

	/** 返回最大军阶 / Returns the max rank */
	public int getMaxRank() {
		return maxRank;
	}

	/** 校验军阶 / Verify Rank */
	public boolean verifyRank(int rank) {
		return (minRank <= rank && maxRank >= rank) || rank >= minRank;
	}
}
