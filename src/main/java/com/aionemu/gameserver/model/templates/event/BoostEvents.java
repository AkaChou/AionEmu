package com.aionemu.gameserver.model.templates.event;

import java.time.ZonedDateTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * BoostEvents 模板（静态数据/XML）。
 * Boost Events Template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BoostEvents")
public class BoostEvents {

	@XmlAttribute(name = "id", required = true)
	protected int id;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	@XmlAttribute(name = "buff_value", required = true)
	protected int buffValue;

	@XmlAttribute(name = "start", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar startDate;

	@XmlAttribute(name = "end", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar endDate;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 返回增益值 / Returns the buff value */
	public int getBuffValue() {
		return buffValue;
	}

	/** 返回开始日期 / Returns the start date */
	public ZonedDateTime getStartDate() {
		return DateTimeUtil.fromCalendar(startDate.toGregorianCalendar());
	}

	/** 返回结束日期 / Returns the end date */
	public ZonedDateTime getEndDate() {
		return DateTimeUtil.fromCalendar(endDate.toGregorianCalendar());
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		ZonedDateTime now = DateTimeUtil.now();
		return getStartDate().isBefore(now) && getEndDate().isAfter(now);
	}

	/**
	 * @return 是否已过期 / whether expired
	 */
	public boolean isExpired() {
		return !isActive();
	}
}
