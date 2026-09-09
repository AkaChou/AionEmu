package com.aionemu.gameserver.model.templates.chest;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 宝箱模板（静态数据/XML）。
 * Chest template (static data / XML).
 *
 * @author Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Chest")
public class ChestTemplate {

	/**
	 * @return NPC ID / the npcId
	 */
	@Getter
	@XmlAttribute(name = "npcid")
	protected int npcId;
	/**
	 * @return 名称 / the name
	 */
	@Getter
	@XmlAttribute(name = "name")
	protected String name;
	/**
	 * @return 钥匙物品列表 / the keyItem
	 */
	@Getter
	@XmlElement(name = "keyitem")
	protected List<KeyItem> keyItem;
}
