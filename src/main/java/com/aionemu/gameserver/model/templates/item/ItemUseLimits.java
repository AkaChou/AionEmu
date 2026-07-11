package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 物品 UseLimits 模板（静态数据/XML）。
 * XML template. / XML template.
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

	/** 返回 delay id / Returns the delay id */
	public int getDelayId() {
		return useDelayId;
	}

	/** 设置 delay id / Sets the delay id */
	public void setDelayId(int delayId) {
		useDelayId = delayId;
	}

	/** 返回延迟时间 / Returns the delay time*/
	public int getDelayTime() {
		return useDelay;
	}

	/** 设置 delay time / Sets the delay time */
	public void setDelayTime(int useDelay) {
		this.useDelay = useDelay;
	}

	/** 返回 use area / Returns the use area */
	public ZoneName getUseArea() {
		if (usearea == null)
			return null;
		try {
			return ZoneName.get(usearea);
		} catch (Exception e) {
		}
		return null;
	}

	/** 返回 ownership world / Returns the ownership world */
	public int getOwnershipWorld() {
		return ownershipWorldId;
	}

	/** 返回 gender permitted / Returns the gender permitted */
	public Gender getGenderPermitted() {
		return genderPermitted;
	}

	/**
	 * @return Whether ride usable / Whether ride usable
	 */
	public boolean isRideUsable() {
		if (rideUsable == null) {
			return false;
		}
		return rideUsable;
	}

	/** 返回最小军阶 / Returns the min rank*/
	public int getMinRank() {
		return minRank;
	}

	/** 返回最大军阶 / Returns the max rank*/
	public int getMaxRank() {
		return maxRank;
	}

	/** 校验军阶 / Verify Rank */
	public boolean verifyRank(int rank) {
		return (minRank <= rank && maxRank >= rank) || rank >= minRank;
	}
}
