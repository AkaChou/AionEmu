package com.aionemu.gameserver.model.templates.tradelist;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 交易列表模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "tradelist_template")
public class TradeListTemplate {
	@XmlAttribute(name = "npc_id", required = true)
	private int npcId;

	@XmlAttribute(name = "npc_type")
	private TradeNpcType tradeNpcType;

	@XmlAttribute(name = "sell_price_rate")
	private int sellPriceRate;

	@XmlAttribute(name = "buy_price_rate")
	private int buyPriceRate;

	@XmlAttribute(name = "ap_buy_price_rate")
	private int apBuyPriceRate;

	@XmlAttribute(name = "ap_sell_price_rate")
	private int apSellPriceRate;

	@XmlElement(name = "tradelist")
	protected List<TradeTab> tradeTablist;

	public TradeListTemplate() {
		tradeNpcType = TradeNpcType.NORMAL;
		sellPriceRate = 100;
		apSellPriceRate = 100;
	}

	/** 返回 trade tablist / Returns the trade tablist */
	public List<TradeTab> getTradeTablist() {
		if (tradeTablist == null) {
			tradeTablist = new ArrayList<TradeTab>();
		}
		return this.tradeTablist;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return tradeTablist.size();
	}

	/** 返回 trade npc type / Returns the trade npc type */
	public TradeNpcType getTradeNpcType() {
		return tradeNpcType;
	}

	/** 获取卖出价格比率。 / Returns the sell price rate. */
	public int getSellPriceRate() {
		return sellPriceRate;
	}

	/** 获取买入价格比率。 / Returns the buy price rate. */
	public int getBuyPriceRate() {
		return buyPriceRate;
	}

	/** 返回 ap buy price rate / Returns the ap buy price rate */
	public int getApBuyPriceRate() {
		return apBuyPriceRate != 0 ? apBuyPriceRate : buyPriceRate;
	}

	/** 返回 ap sell price rate / Returns the ap sell price rate */
	public int getApSellPriceRate() {
		return apSellPriceRate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "Tradelist")
	public static class TradeTab {
		@XmlAttribute
		protected int id;

		/** 返回 ID / Returns the id */
		public int getId() {
			return id;
		}
	}
}
