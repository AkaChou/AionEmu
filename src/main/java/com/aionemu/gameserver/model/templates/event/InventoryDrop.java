package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

import com.aionemu.gameserver.model.Race;

/**
 * 背包掉落模板（静态数据/XML）。
 * Inventory Drop Template (static data/XML).
 *
 * @author Rolandas
 */

@XmlType(name = "InventoryDrop")
@XmlAccessorType(XmlAccessType.FIELD)
public class InventoryDrop {
	@XmlValue
	private int dropItem;

	@XmlAttribute(name = "startlevel", required = false)
	private int startLevel;

	@XmlAttribute(name = "endlevel", required = false)
	private int endLevel;

	@XmlAttribute(name = "interval", required = true)
	private int interval;

	@XmlAttribute(name = "count", required = false)
	private long count = 1;

	@XmlAttribute(name = "maxCountOfDay", required = false)
	private int maxCountOfDay;

	@XmlAttribute(name = "cleanTime", required = false)
	private int cleanTime;

	@XmlAttribute
	private Race race = Race.PC_ALL;

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 获取掉落物品。 / Returns the drop item. */
	public int getDropItem() {
		return dropItem;
	}

	/** 返回开始等级 / Returns the start level */
	public int getStartLevel() {
		return startLevel;
	}

	/** 返回结束等级 / Returns the end level */
	public int getEndLevel() {
		return endLevel;
	}

	/** 返回间隔 / Returns the interval */
	public int getInterval() {
		return interval;
	}

	/** 获取计数。 / Returns the count. */
	public long getCount() {
		return count;
	}

	/** 返回每日最大次数 / Returns the max count of day */
	public int getMaxCountOfDay() {
		return maxCountOfDay;
	}

	/** 返回清理时间 / Returns the clean time */
	public int getCleanTime() {
		return cleanTime;
	}
}
