package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 归还之石属性模板（静态数据/XML）。
 * XML template.
 */

@XmlRootElement(name = "kisk_stats")
@XmlAccessorType(XmlAccessType.FIELD)
public class KiskStatsTemplate {
	@XmlAttribute(name = "usemask")
	private int useMask = 6;

	@XmlAttribute(name = "members")
	private int maxMembers = 576;

	@XmlAttribute(name = "resurrects")
	private int maxResurrects = 1728;

	/** 返回 use mask / Returns the use mask */
	public int getUseMask() {
		return useMask;
	}

	/** 返回 max members / Returns the max members */
	public int getMaxMembers() {
		return maxMembers;
	}

	/** 返回 max resurrects / Returns the max resurrects */
	public int getMaxResurrects() {
		return maxResurrects;
	}
}
