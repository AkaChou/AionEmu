package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 传送门 Scroll 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalScroll", propOrder = { "portalPath" })
public class PortalScroll {

	/** 获取传送门路径。 / Returns the portal path. */
	@Getter
	@XmlElement(name = "portal_path")
	protected PortalPath portalPath;
	/** 获取名称。 / Returns the name. */
	@Getter
	@Setter
	@XmlAttribute
	protected String name;
}
