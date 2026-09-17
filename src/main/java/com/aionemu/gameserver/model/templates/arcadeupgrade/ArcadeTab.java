package com.aionemu.gameserver.model.templates.arcadeupgrade;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 街机 Tab 模板（静态数据/XML）。
 * XML template.
 */
@Getter
@XmlType(name = "ArcadeTab")
public class ArcadeTab {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "item")
	private List<ArcadeTabItem> arcadeTabItem;

	/** 返回 arcade tab items / Returns the arcade tab items */
	public List<ArcadeTabItem> getArcadeTabItems() {
		return arcadeTabItem;
	}
}
