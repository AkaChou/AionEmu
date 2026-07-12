package com.aionemu.gameserver.model.templates.arcadeupgrade;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 街机 Tab 物品模板（静态数据/XML）。
 * XML template.
 */
@XmlType(name = "ArcadeTabItemList")
public class ArcadeTabItem {
	@XmlAttribute(name = "item_id")
	protected int item_id;

	@XmlAttribute(name = "normalcount")
	protected int normalcount;

	@XmlAttribute(name = "frenzycount")
	protected int frenzycount;

	/** 返回物品 ID / Returns the item id */
	public final int getItemId() {
		return item_id;
	}

	/** 返回普通数量 / Returns the normal count*/
	public final int getNormalCount() {
		return normalcount;
	}

	/** 返回 frenzy count / Returns the frenzy count */
	public final int getFrenzyCount() {
		return frenzycount;
	}
}
