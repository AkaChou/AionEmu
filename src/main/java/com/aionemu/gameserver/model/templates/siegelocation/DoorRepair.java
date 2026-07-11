package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * DoorRepair 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DoorRepair")
public class DoorRepair {
	@XmlAttribute(name = "itemid")
	protected int itemId;

	@XmlAttribute(name = "repair_fee")
	protected int repairFee;

	@XmlAttribute(name = "repair_cooltime")
	protected int repairCooltime;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 返回 repair fee / Returns the repair fee */
	public int getRepairFee() {
		return repairFee;
	}

	/** 返回 repair cooltime / Returns the repair cooltime */
	public long getRepairCooltime() {
		return (long) (repairCooltime * 1000);
	}
}
