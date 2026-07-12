package com.aionemu.gameserver.model.templates.event;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import javax.xml.datatype.XMLGregorianCalendar;

import com.aionemu.gameserver.model.AttendType;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * 艾特里亚 Passport 模板（静态数据/XML）。
 * XML template.
 *
 * @author Ghostfur (Aion-Unique)
 */
@XmlRootElement(name = "atreian_passport")
@XmlAccessorType(XmlAccessType.FIELD)
public class AtreianPassport {

	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "name")
	private String name = "";

	@XmlAttribute(name = "active", required = true)
	private int active;

	@XmlAttribute(name = "attend_type", required = true)
	private AttendType attendType;

	@XmlAttribute(name = "attend_num")
	private int attendNum;

	@XmlAttribute(name = "period_start", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar pStart;

	@XmlAttribute(name = "period_end", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar pEnd;

	protected List<AtreianPassportRewards> atreian_passport_reward;

	/** 返回当前 / Returns the active */
	public int getActive() {
		return active;
	}

	/** 获取签到类型。 / Returns the attend type. */
	public AttendType getAttendType() {
		return attendType;
	}

	/** 返回 attend num / Returns the attend num */
	public int getAttendNum() {
		return attendNum;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 period start / Returns the period start */
	public ZonedDateTime getPeriodStart() {
		return DateTimeUtil.fromCalendar(pStart.toGregorianCalendar());
	}

	/** 返回 period end / Returns the period end */
	public ZonedDateTime getPeriodEnd() {
		return DateTimeUtil.fromCalendar(pEnd.toGregorianCalendar());
	}

	/** 返回 atreian passport rewards / Returns the atreian passport rewards */
	public List<AtreianPassportRewards> getAtreianPassportRewards() {
		if (atreian_passport_reward == null) {
			atreian_passport_reward = new ArrayList<AtreianPassportRewards>();
		}
		return atreian_passport_reward;
	}
}
