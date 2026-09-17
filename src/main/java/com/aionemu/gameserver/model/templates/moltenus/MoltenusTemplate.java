package com.aionemu.gameserver.model.templates.moltenus;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 熔岩魔模板（静态数据/XML）。
 * Moltenus XML template.
 *
 * @author Rinzler (Encom)
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Moltenus")
public class MoltenusTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;
}
