package com.aionemu.gameserver.model.templates.luna_dice;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华 DiceTable 模板（静态数据/XML）。
 * XML template.
 */

@XmlType(name = "LunaDiceTable")
public class LunaDiceTable {
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "reward")
	private List<LunaDiceItem> lunaDiceTabItem;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 luna dice tab items / Returns the luna dice tab items */
	public List<LunaDiceItem> getLunaDiceTabItems() {
		return lunaDiceTabItem;
	}
}
