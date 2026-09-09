package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.InstanceCoolTimeType;
import com.aionemu.gameserver.model.instance.InstanceType;
import lombok.Getter;

/**
 * 副本冷却时间模板（静态数据/XML）。
 * Instance cooldown template (static data / XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceCooltime")
public class InstanceCooltime {
	/** 返回 cool time type / Returns the cool time type */
	@Getter
	@XmlElement(name = "type")
	protected InstanceCoolTimeType coolTimeType;

	/** 获取类型值。 / Returns the type value. */
	@Getter
	@XmlElement(name = "type_value")
	protected String typeValue;

	/** 返回 ent cool time / Returns the ent cool time */
	@Getter
	@XmlElement(name = "ent_cool_time")
	protected Integer entCoolTime;

	@XmlElement(name = "indun_type")
	protected InstanceType indunType;

	/** 返回 max member light / Returns the max member light */
	@Getter
	@XmlElement(name = "max_member_light")
	protected Integer maxMemberLight;

	/** 返回 max member dark / Returns the max member dark */
	@Getter
	@XmlElement(name = "max_member_dark")
	protected Integer maxMemberDark;

	/** 返回进入最小等级光 / Returns the enter min level light*/
	@Getter
	@XmlElement(name = "enter_min_level_light")
	protected Integer enterMinLevelLight;

	/** 返回进入最大等级光 / Returns the enter max level light*/
	@Getter
	@XmlElement(name = "enter_max_level_light")
	protected Integer enterMaxLevelLight;

	/** 返回进入最小等级暗 / Returns the enter min level dark*/
	@Getter
	@XmlElement(name = "enter_min_level_dark")
	protected Integer enterMinLevelDark;

	/** 返回进入最大等级暗 / Returns the enter max level dark*/
	@Getter
	@XmlElement(name = "enter_max_level_dark")
	protected Integer enterMaxLevelDark;

	/** 返回 alarm unit score / Returns the alarm unit score */
	@Getter
	@XmlElement(name = "alarm_unit_score")
	protected Integer alarmUnitScore;

	@XmlElement(name = "can_enter_mentor")
	protected boolean canEnterMentor;

	@XmlElement(name = "enter_guild")
	protected boolean enterGuild;

	@XmlElement(name = "max_count")
	protected Integer max_count;

	// 4.9 版本新增 / Added in 4.9
	/** 返回数量 buildup / Returns the count build up */
	@Getter
	@XmlElement(name = "count_build_up")
	protected Integer countBuildUp;

	/** 返回 count build up level / Returns the count build up level */
	@Getter
	@XmlElement(name = "count_build_up_level")
	protected Integer countBuildUpLevel;

	/**
	 * 副本 ID。
	 * Instance ID.
	 */
	@Getter
	@XmlAttribute(required = true)
	protected int id;

	/** 返回世界 ID / Returns the world id */
	@Getter
	@XmlAttribute(required = true)
	protected int worldId;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(required = true)
	protected Race race;

	/** 获取类型副本。 / Returns the type instance. */
	public InstanceType getTypeInstance() {
		return indunType;
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
}
