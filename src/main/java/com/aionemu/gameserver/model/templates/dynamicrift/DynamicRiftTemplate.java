package com.aionemu.gameserver.model.templates.dynamicrift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 动态裂隙模板（静态数据/XML）。
 * Dynamic rift template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dynamic_rift")
public class DynamicRiftTemplate {
	/** 返回 ID。 / Returns the id. */
	@Getter
	@XmlAttribute(name = "id")
	protected int id;
}
