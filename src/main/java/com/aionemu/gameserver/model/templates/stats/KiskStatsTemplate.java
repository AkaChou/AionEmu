package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 归还之石属性模板（静态数据/XML）。
 * XML template.
 */

@XmlRootElement(name = "kisk_stats")
@XmlAccessorType(XmlAccessType.FIELD)
public class KiskStatsTemplate {
	/** 返回 use mask / Returns the use mask */
	@Getter
	@XmlAttribute(name = "usemask")
	private int useMask = 6;

	/** 返回 max members / Returns the max members */
	@Getter
	@XmlAttribute(name = "members")
	private int maxMembers = 576;

	/** 返回 max resurrects / Returns the max resurrects */
	@Getter
	@XmlAttribute(name = "resurrects")
	private int maxResurrects = 1728;
}
