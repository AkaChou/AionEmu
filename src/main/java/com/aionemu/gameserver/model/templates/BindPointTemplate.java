package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 绑定点模板（静态数据/XML 模板）。
 * Bind point template (static data / XML template).
 *
 * @author avol
 */
@XmlRootElement(name = "bind_point")
@XmlAccessorType(XmlAccessType.NONE)
public class BindPointTemplate {

	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(name = "name", required = true)
	private String name;

	/** 返回 NPC ID / Returns the npc id */
	@Getter
	@XmlAttribute(name = "npcid")
	private int npcId;

	/** 获取价格。 / Returns the price. */
	@Getter
	@XmlAttribute(name = "price")
	private int price = 0;
}
