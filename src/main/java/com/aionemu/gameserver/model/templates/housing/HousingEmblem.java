package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 住房徽章模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HousingEmblem")
public class HousingEmblem extends PlaceableHouseObject {

	/** 获取等级。 / Returns the level. */
	@XmlAttribute(name = "level", required = true)
	private int level;

	/** 返回类型 ID / Returns the type id */
	@Override
	public byte getTypeId() {
		return 11;
	}
}
