package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 传送门 Scroll 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalScroll", propOrder = { "portalPath" })
public class PortalScroll {

	@XmlElement(name = "portal_path")
	protected PortalPath portalPath;
	@XmlAttribute
	protected String name;

	/** 获取传送门路径。 / Returns the portal path. */
	public PortalPath getPortalPath() {
		return portalPath;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 设置名称。 / Sets the name. */
	public void setName(String value) {
		this.name = value;
	}
}
