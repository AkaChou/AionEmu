package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 召唤物队伍模板：定义召唤数量、位置与调度间隔。
 * Summon group template: defines summoned counts, position and schedule interval.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonGroup")
public class SummonGroup {

	/** 返回 NPC ID / Returns the npc id */
	@Getter
	@XmlAttribute(name = "npcId")
	protected int npcId;
	/** 返回 x / Returns the x */
	@Getter
	@XmlAttribute(name = "x")
	protected float x;
	/** 返回 y / Returns the y */
	@Getter
	@XmlAttribute(name = "y")
	protected float y;
	/** 返回 z / Returns the z */
	@Getter
	@XmlAttribute(name = "z")
	protected float z;
	/** 返回 h / Returns the h */
	@Getter
	@XmlAttribute(name = "h")
	protected byte h;
	/** 获取计数。 / Returns the count. */
	@Getter
	@XmlAttribute(name = "count")
	protected int count;
	/** 返回最小数量 / Returns the min count*/
	@Getter
	@XmlAttribute(name = "minCount")
	protected int minCount;
	/** 返回最大数量 / Returns the max count*/
	@Getter
	@XmlAttribute(name = "maxCount")
	protected int maxCount;
	/** 返回 distance / Returns the distance */
	@Getter
	@XmlAttribute(name = "distance")
	protected float distance;
	/** 返回 schedule / Returns the schedule */
	@Getter
	@XmlAttribute(name = "schedule")
	protected int schedule;
}
