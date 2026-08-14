package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * NPC 交易列表静态数据容器，分别索引普通交易、以物易物与收购列表。
 * NPC trade-list static-data holder, indexing sell, trade-in and purchase lists by NPC id.
 */
@XmlRootElement(name = "npc_trade_list")
@XmlAccessorType(XmlAccessType.FIELD)
public class TradeListData {
	@XmlElement(name = "tradelist_template")
	private List<TradeListTemplate> tlist;

	@XmlElement(name = "trade_in_list_template")
	private List<TradeListTemplate> tInlist;

	@XmlElement(name = "purchase_list_template")
	private List<TradeListTemplate> ptlist;

	/** 普通交易列表映射 / sell trade-list map */
	private IntObjectHashMap<TradeListTemplate> npctlistData = new IntObjectHashMap<TradeListTemplate>();

	private IntObjectHashMap<TradeListTemplate> npcTradeInlistData = new IntObjectHashMap<TradeListTemplate>();

	private IntObjectHashMap<TradeListTemplate> npcPurchaselistData = new IntObjectHashMap<TradeListTemplate>();

	/**
	 * JAXB 反序列化完成后，将三类交易列表按 NPC ID 索引。
	 * After JAXB unmarshalling, indexes sell, trade-in and purchase lists by NPC id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TradeListTemplate npc : tlist) {
			npctlistData.put(npc.getNpcId(), npc);
		}

		for (TradeListTemplate npc : tInlist) {
			npcTradeInlistData.put(npc.getNpcId(), npc);
		}

		for (TradeListTemplate npc : ptlist) {
			npcPurchaselistData.put(npc.getNpcId(), npc);
		}
	}

	/**
	 * 返回已加载的普通交易列表数量。
	 * Returns the number of loaded sell trade lists.
	 *
	 * @return 已加载的出售交易列表数量 / Returns the number of loaded sell trade lists.
	 */
	public int size() {
		return npctlistData.size();
	}

	/**
	 * 按 NPC ID 获取普通交易列表模板。
	 * Returns the sell trade-list template for the given NPC id.
	 *
	 * @param id NPC ID / npc id
	 * @return 交易列表模板，不存在则为 null / trade-list template or null
	 */
	public TradeListTemplate getTradeListTemplate(int id) {
		return npctlistData.get(id);
	}

	/**
	 * 按 NPC ID 获取以物易物列表模板。
	 * Returns the trade-in list template for the given NPC id.
	 *
	 * @param id NPC ID / npc id
	 * @return 以物易物模板，不存在则为 null / trade-in template or null
	 */
	public TradeListTemplate getTradeInListTemplate(int id) {
		return npcTradeInlistData.get(id);
	}

	/**
	 * 按 NPC ID 获取收购列表模板。
	 * Returns the purchase list template for the given NPC id.
	 *
	 * @param id NPC ID / npc id
	 * @return 收购列表模板，不存在则为 null / purchase template or null
	 */
	public TradeListTemplate getPurchaseListTemplate(int id) {
		return npcPurchaselistData.get(id);
	}

	/**
	 * 返回全部普通交易列表映射。
	 * Returns the full map of sell trade-list templates.
	 *
	 * @return 交易列表映射 / trade-list map
	 */
	public IntObjectHashMap<TradeListTemplate> getTradeListTemplate() {
		return npctlistData;
	}
}
