package com.aionemu.gameserver.model.templates.beritra;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 贝里特拉模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Beritra")
public class BeritraTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id")
	protected int id;
}
