package com.aionemu.gameserver.model.templates.npcshout;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * NPC 喊话条目模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcShout")
public class NpcShout {

	@XmlAttribute(name = "string_id", required = true)
	protected int stringId;

	@XmlAttribute(name = "when", required = true)
	protected ShoutEventType when;

	@XmlAttribute(name = "pattern")
	protected String pattern;

	@XmlAttribute(name = "param")
	protected String param;

	@XmlAttribute(name = "type")
	protected ShoutType type;

	@XmlAttribute(name = "skill_no")
	protected Integer skillNo;

	@XmlAttribute(name = "poll_delay")
	protected Integer pollDelay;

	 /**
	  * 获取 stringId 属性值。
	  * Gets the value of the stringId property
	  */
	public int getStringId() {
		return stringId;
	}

	 /**
	  * 获取 when 属性值。
	  * Gets the value of the when property
	  * @return possible object is {@link ShoutEventType }
	  */
	public ShoutEventType getWhen() {
		return when;
	}

	 /**
	  * 获取 pattern 属性值。
	  * Gets the value of the pattern property
	  * @return possible object is {@link String }
	  */
	public String getPattern() {
		return pattern;
	}

	 /**
	  * 获取 param 属性值。
	  * Gets the value of the param property
	  * @return possible object is {@link String }
	  */
	public String getParam() {
		return param;
	}

	 /**
	  * 获取 type 属性值。
	  * Gets the value of the type property
	  * @return possible object is {@link ShoutType }
	  */
	public ShoutType getShoutType() {
		if (type == null) {
			return ShoutType.BROADCAST;
		}
		return type;
	}

	 /**
	  * 获取 skillNo 属性值。
	  * Gets the value of the skillNo property
	  * @return possible object is {@link Integer }
	  */
	public int getSkillNo() {
		if (skillNo == null) {
			return 0;
		}
		return skillNo;
	}

	/** 返回 poll delay / Returns the poll delay */
	public int getPollDelay() {
		if (pollDelay == null) {
			return 0;
		}
		return pollDelay;
	}

	/** 返回 shout range / Returns the shout range */
	public int getShoutRange(Npc npc) {
		return npc.getObjectTemplate().getMinimumShoutRange();
	}
}
