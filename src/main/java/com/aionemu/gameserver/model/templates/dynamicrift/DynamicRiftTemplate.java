package com.aionemu.gameserver.model.templates.dynamicrift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 动态裂隙模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dynamic_rift")
public class DynamicRiftTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
