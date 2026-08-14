package com.aionemu.gameserver.model.templates.chest;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宝箱模板（静态数据/XML）。
 * Chest template (static data / XML).
 *
 * @author Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Chest")
public class ChestTemplate {

	@XmlAttribute(name = "npcid")
	protected int npcId;
	@XmlAttribute(name = "name")
	protected String name;
	@XmlElement(name = "keyitem")
	protected List<KeyItem> keyItem;

	/**
	 * @return NPC ID / the npcId
	 */
	public int getNpcId() {
		return npcId;
	}

	/**
	 * @return 名称 / the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return 钥匙物品列表 / the keyItem
	 */
	public List<KeyItem> getKeyItem() {
		return keyItem;
	}
}
