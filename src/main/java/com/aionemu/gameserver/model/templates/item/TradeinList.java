package com.aionemu.gameserver.model.templates.item;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 兑换列表模板：一组可兑换物品。
 * Tradein list template: a set of tradein items.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TradeinList", propOrder = { "tradeinItem" })
public class TradeinList {

	@XmlElement(name = "tradein_item")
	protected List<TradeinItem> tradeinItem;

	/** 返回兑换物品列表 / Returns the tradein item */
	public List<TradeinItem> getTradeinItem() {
		return this.tradeinItem;
	}
}
