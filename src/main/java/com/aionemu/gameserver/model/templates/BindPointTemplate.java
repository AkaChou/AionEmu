package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 绑定点模板（静态数据/XML 模板）。
 * XML template.
 *
 * @author avol
 */
@XmlRootElement(name = "bind_points")
@XmlAccessorType(XmlAccessType.NONE)
public class BindPointTemplate {

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlAttribute(name = "npcid")
	private int npcId;

	@XmlAttribute(name = "price")
	private int price = 0;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 获取价格。 / Returns the price. */
	public int getPrice() {
		return price;
	}
}
