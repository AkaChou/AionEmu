package com.aionemu.gameserver.model.templates.instancerift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 副本裂隙模板（静态数据/XML）。
 * Instance rift template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "instance_rift")
public class InstanceRiftTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id")
	protected int id;
}
