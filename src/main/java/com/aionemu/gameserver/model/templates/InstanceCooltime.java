package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.InstanceCoolTimeType;
import com.aionemu.gameserver.model.instance.InstanceType;

/**
 * 副本冷却时间模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceCooltime")
public class InstanceCooltime {
	@XmlElement(name = "type")
	protected InstanceCoolTimeType coolTimeType;

	@XmlElement(name = "type_value")
	protected String typeValue;

	@XmlElement(name = "ent_cool_time")
	protected Integer entCoolTime;

	@XmlElement(name = "indun_type")
	protected InstanceType indunType;

	@XmlElement(name = "max_member_light")
	protected Integer maxMemberLight;

	@XmlElement(name = "max_member_dark")
	protected Integer maxMemberDark;

	@XmlElement(name = "enter_min_level_light")
	protected Integer enterMinLevelLight;

	@XmlElement(name = "enter_max_level_light")
	protected Integer enterMaxLevelLight;

	@XmlElement(name = "enter_min_level_dark")
	protected Integer enterMinLevelDark;

	@XmlElement(name = "enter_max_level_dark")
	protected Integer enterMaxLevelDark;

	@XmlElement(name = "alarm_unit_score")
	protected Integer alarmUnitScore;

	@XmlElement(name = "can_enter_mentor")
	protected boolean canEnterMentor;

	@XmlElement(name = "enter_guild")
	protected boolean enterGuild;

	@XmlElement(name = "max_count")
	protected Integer max_count;

	// 4.9
	@XmlElement(name = "count_build_up")
	protected Integer countBuildUp;

	@XmlElement(name = "count_build_up_level")
	protected Integer countBuildUpLevel;

	/**
	*/
	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(required = true)
	protected int worldId;

	@XmlAttribute(required = true)
	protected Race race;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回 cool time type / Returns the cool time type */
	public InstanceCoolTimeType getCoolTimeType() {
		return coolTimeType;
	}

	/** 获取类型值。 / Returns the type value. */
	public String getTypeValue() {
		return typeValue;
	}

	/** 获取类型副本。 / Returns the type instance. */
	public InstanceType getTypeInstance() {
		return indunType;
	}

	/** 返回 ent cool time / Returns the ent cool time */
	public Integer getEntCoolTime() {
		return entCoolTime;
	}

	/** 返回 max member light / Returns the max member light */
	public Integer getMaxMemberLight() {
		return maxMemberLight;
	}

	/** 返回 max member dark / Returns the max member dark */
	public Integer getMaxMemberDark() {
		return maxMemberDark;
	}

	/** 返回进入最小等级光 / Returns the enter min level light*/
	public Integer getEnterMinLevelLight() {
		return enterMinLevelLight;
	}

	/** 返回进入最大等级光 / Returns the enter max level light*/
	public Integer getEnterMaxLevelLight() {
		return enterMaxLevelLight;
	}

	/** 返回进入最小等级暗 / Returns the enter min level dark*/
	public Integer getEnterMinLevelDark() {
		return enterMinLevelDark;
	}

	/** 返回进入最大等级暗 / Returns the enter max level dark*/
	public Integer getEnterMaxLevelDark() {
		return enterMaxLevelDark;
	}

	/** 返回 alarm unit score / Returns the alarm unit score */
	public Integer getAlarmUnitScore() {
		return alarmUnitScore;
	}

	/** 返回 can enter mentor / Returns the can enter mentor */
	public boolean getCanEnterMentor() {
		return canEnterMentor;
	}

	/** 返回 enter guild / Returns the enter guild */
	public boolean getEnterGuild() {
		return enterGuild;
	}

	/** 返回 max entries count / Returns the max entries count */
	public Integer getMaxEntriesCount() {
		return max_count;
	}

	/** 返回数量 buildup / Returns the count build up */
	public Integer getCountBuildUp() {
		return countBuildUp;
	}

	/** 返回 count build up level / Returns the count build up level */
	public Integer getCountBuildUpLevel() {
		return countBuildUpLevel;
	}
}
