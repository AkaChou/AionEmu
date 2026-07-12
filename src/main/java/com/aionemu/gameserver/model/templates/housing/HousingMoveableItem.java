package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 住房 Moveable 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HousingMoveableItem")
public class HousingMoveableItem extends PlaceableHouseObject {

	/** 返回类型 ID / Returns the type id */
	@Override
	public byte getTypeId() {
		return 0;
	}
}
