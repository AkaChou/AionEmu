package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 召唤物队伍模板：定义召唤数量、位置与调度间隔。
 * Summon group template: defines summoned counts, position and schedule interval.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonGroup")
public class SummonGroup {

	@XmlAttribute(name = "npcId")
	protected int npcId;
	@XmlAttribute(name = "x")
	protected float x;
	@XmlAttribute(name = "y")
	protected float y;
	@XmlAttribute(name = "z")
	protected float z;
	@XmlAttribute(name = "h")
	protected byte h;
	@XmlAttribute(name = "count")
	protected int count;
	@XmlAttribute(name = "minCount")
	protected int minCount;
	@XmlAttribute(name = "maxCount")
	protected int maxCount;
	@XmlAttribute(name = "distance")
	protected float distance;
	@XmlAttribute(name = "schedule")
	protected int schedule;

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 返回 h / Returns the h */
	public byte getH() {
		return h;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 返回最小数量 / Returns the min count*/
	public int getMinCount() {
		return minCount;
	}

	/** 返回最大数量 / Returns the max count*/
	public int getMaxCount() {
		return maxCount;
	}

	/** 返回 distance / Returns the distance */
	public float getDistance() {
		return distance;
	}

	/** 返回 schedule / Returns the schedule */
	public int getSchedule() {
		return schedule;
	}
}
