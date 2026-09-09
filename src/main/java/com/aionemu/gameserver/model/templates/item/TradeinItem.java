package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 兑换（以旧换新）物品模板：物品与价格。
 * Tradein item template: item and price.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TradeinItem")
public class TradeinItem {

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute
	protected int id;
	/** 获取价格。 / Returns the price. */
	@Getter
	@XmlAttribute
	protected int price;
}
