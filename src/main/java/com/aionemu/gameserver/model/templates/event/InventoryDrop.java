package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 背包掉落模板（静态数据/XML）。
 * Inventory Drop Template (static data/XML).
 *
 * @author Rolandas
 */

@XmlType(name = "InventoryDrop")
@XmlAccessorType(XmlAccessType.FIELD)
public class InventoryDrop {
	/** 获取掉落物品。 / Returns the drop item. */
	@Getter
	@XmlValue
	private int dropItem;

	/** 返回开始等级 / Returns the start level */
	@Getter
	@XmlAttribute(name = "startlevel", required = false)
	private int startLevel;

	/** 返回结束等级 / Returns the end level */
	@Getter
	@XmlAttribute(name = "endlevel", required = false)
	private int endLevel;

	/** 返回间隔 / Returns the interval */
	@Getter
	@XmlAttribute(name = "interval", required = true)
	private int interval;

	/** 获取计数。 / Returns the count. */
	@Getter
	@XmlAttribute(name = "count", required = false)
	private long count = 1;

	/** 返回每日最大次数 / Returns the max count of day */
	@Getter
	@XmlAttribute(name = "maxCountOfDay", required = false)
	private int maxCountOfDay;

	/** 返回清理时间 / Returns the clean time */
	@Getter
	@XmlAttribute(name = "cleanTime", required = false)
	private int cleanTime;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute
	private Race race = Race.PC_ALL;
}
