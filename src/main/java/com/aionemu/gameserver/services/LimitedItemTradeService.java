package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameCronServices;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.GoodsListData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import com.aionemu.gameserver.model.limiteditems.LimitedTradeNpc;
import com.aionemu.gameserver.model.templates.goods.GoodsList;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 限购商品交易服务，按 NPC 聚合限购项并定时重置销量。
 * Limited-item trade service that aggregates per-NPC limited goods and resets sales on schedule.
 */
@Slf4j
public class LimitedItemTradeService {
	private static volatile ObjectProvider<LimitedItemTradeService> instanceProvider;
	private GoodsListData goodsListData = DataManager.GOODSLIST_DATA;
	private TradeListData tradeListData = DataManager.TRADE_LIST_DATA;
	/** NPC ID 到限购交易数据 / NPC id to limited-trade data*/
	private Map<Integer, LimitedTradeNpc> limitedTradeNpcs = new HashMap<Integer, LimitedTradeNpc>();

	/**
	 * 从交易表加载限购项，并按销售时间 cron 重置。
	 * Loads limited items from trade lists and schedules sales-time cron resets.
	 */
	public void start() {
		for (int npcId : tradeListData.getTradeListTemplate().keys()) {
			for (TradeTab list : tradeListData.getTradeListTemplate(npcId).getTradeTablist()) {
				GoodsList goodsList = goodsListData.getGoodsListById(list.getId());
				if (goodsList == null) {
					continue;
				}
				List<LimitedItem> limitedItems = goodsList.getLimitedItems();
				if (limitedItems.isEmpty()) {
					continue;
				}
				LimitedTradeNpc limitedTradeNpc = limitedTradeNpcs.get(npcId);
				if (limitedTradeNpc == null) {
					limitedTradeNpcs.put(npcId, new LimitedTradeNpc(limitedItems));
				} else {
					limitedTradeNpc.putLimitedItems(limitedItems);
				}
			}
		}
		for (LimitedTradeNpc limitedTradeNpc : limitedTradeNpcs.values()) {
			for (final LimitedItem limitedItem : limitedTradeNpc.getLimitedItems()) {
				GameCronServices.cronService().schedule(new Runnable() {
					@Override
					public void run() {
						limitedItem.setToDefault();
					}
				}, limitedItem.getSalesTime());
			}
		}
		log.info(I18n.get("log.5a5db961d623", limitedTradeNpcs.size()));
	}

	/**
	 * 按物品与 NPC 查询限购项。
	 * Looks up a limited item by item id and NPC id.
	 *
	 * item id
	 * npc id
	 * @return 限购项，不存在则为 null / limited item, or null
	 */
	public LimitedItem getLimitedItem(int itemId, int npcId) {
		if (limitedTradeNpcs.containsKey(npcId)) {
			for (LimitedItem limitedItem : limitedTradeNpcs.get(npcId).getLimitedItems()) {
				if (limitedItem.getItemId() == itemId) {
					return limitedItem;
				}
			}
		}
		return null;
	}

	/**
	 * 判断 NPC 是否为限购商人。
	 * Returns whether the NPC has limited-trade goods.
	 *
	 * npc id
	 *
	 * @param npcId
	 * @return 是否限购商人 / whether limited-trade NPC
	 */
	public boolean isLimitedTradeNpc(int npcId) {
		return limitedTradeNpcs.containsKey(npcId);
	}

	/**
	 * 获取 NPC 的限购交易数据。
	 * Returns limited-trade data for the NPC.
	 *
	 * npc id
	 *
	 * @param npcId
	 * @return 限购交易数据 / limited-trade data
	 */
	public LimitedTradeNpc getLimitedTradeNpc(int npcId) {
		return limitedTradeNpcs.get(npcId);
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static LimitedItemTradeService getInstance() {
		ObjectProvider<LimitedItemTradeService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<LimitedItemTradeService> instanceProvider) {
		LimitedItemTradeService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final LimitedItemTradeService INSTANCE = new LimitedItemTradeService();
	}
}
