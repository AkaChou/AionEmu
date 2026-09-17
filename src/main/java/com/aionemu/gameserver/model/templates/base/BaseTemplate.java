package com.aionemu.gameserver.model.templates.base;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 基础模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Base")
public class BaseTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "world")
	protected int world;

	@XmlAttribute(name = "name")
	protected String nameId;

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.world;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}
}
