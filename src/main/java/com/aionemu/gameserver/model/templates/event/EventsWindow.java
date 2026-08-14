package com.aionemu.gameserver.model.templates.event;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import javax.xml.datatype.XMLGregorianCalendar;

import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * Events 窗口模板（静态数据/XML）。
 * Events Window Template (static data/XML).
 *
 * @author Ghostfur (Aion-Unique)
 */
@XmlRootElement(name = "atreian_passport")
@XmlAccessorType(value = XmlAccessType.NONE)
public class EventsWindow {

	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "item", required = true)
	private int item;

	@XmlAttribute(name = "count", required = true)
	private long count;

	@XmlAttribute(name = "period_start", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar pStart;

	@XmlAttribute(name = "period_end", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar pEnd;

	@XmlAttribute(name = "remaining_time", required = true)
	private int remaining_time;

	@XmlAttribute(name = "min_level", required = true)
	private int min_level;

	@XmlAttribute(name = "max_level", required = true)
	private int max_level;

	@XmlAttribute(name = "dailyMaxCount", required = true)
	private int dailyMaxCount;

	private Timestamp lastStamp;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return item;
	}

	/** 获取计数。 / Returns the count. */
	public long getCount() {
		return count;
	}

	/** 返回每日最大次数 / Returns the max count of day */
	public int getMaxCountOfDay() {
		return dailyMaxCount;
	}

	/** 返回周期开始时间 / Returns the period start */
	public ZonedDateTime getPeriodStart() {
		return DateTimeUtil.fromCalendar(pStart.toGregorianCalendar());
	}

	/** 返回周期结束时间 / Returns the period end */
	public ZonedDateTime getPeriodEnd() {
		return DateTimeUtil.fromCalendar(pEnd.toGregorianCalendar());
	}

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		return remaining_time;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return min_level;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return max_level;
	}

	/** 返回上次盖章时间 / Returns the last stamp */
	public Timestamp getLastStamp() {
		return lastStamp;
	}

	/** 设置上次盖章时间 / Sets the last stamp */
	public void setLastStamp(Timestamp timestamp) {
		lastStamp = timestamp;
	}
}
