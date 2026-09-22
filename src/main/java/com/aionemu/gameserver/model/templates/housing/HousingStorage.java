package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 住房仓库模板（静态数据/XML）。
 * XML template.
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HousingStorage")
public class HousingStorage extends PlaceableHouseObject {

	/**
	 * 获取 warehouseId 属性值。
	 * Gets the value of the warehouseId property
	 */
	@XmlAttribute(name = "warehouse_id", required = true)
	protected int warehouseId;

	/** 返回类型 ID / Returns the type id */
	@Override
	public byte getTypeId() {
		return 2;
	}
}
