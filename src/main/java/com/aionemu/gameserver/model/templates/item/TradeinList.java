package com.aionemu.gameserver.model.templates.item;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Tradein 列表模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TradeinList", propOrder = { "tradeinItem" })
public class TradeinList {

	@XmlElement(name = "tradein_item")
	protected List<TradeinItem> tradeinItem;

	/** 返回 tradein item / Returns the tradein item */
	public List<TradeinItem> getTradeinItem() {
		return this.tradeinItem;
	}
}
