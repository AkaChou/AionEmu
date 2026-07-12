package com.aionemu.gameserver.model.templates.arcadeupgrade;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 街机 Tab 模板（静态数据/XML）。
 * XML template.
 */
@XmlType(name = "ArcadeTab")
public class ArcadeTab {
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "item")
	private List<ArcadeTabItem> arcadeTabItem;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 arcade tab items / Returns the arcade tab items */
	public List<ArcadeTabItem> getArcadeTabItems() {
		return arcadeTabItem;
	}
}
