package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 传送门物品模板（静态数据/XML）。
 * XML template.
 *
 * @author AionChs Master
 * @author Schattenlilie
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PortalItem")
public class PortalItem {

	/**
	 * @return 物品 ID / the id
	 */
	@XmlAttribute(name = "id")
	protected int id;
	/**
	 * @return 物品编号 / the itemid
	 */
	@XmlAttribute(name = "itemid")
	protected int itemid;
	/**
	 * @return 数量 / the quantity
	 */
	@XmlAttribute(name = "quantity")
	protected int quantity;
}
