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
	private final GoodsListData goodsListData = DataManager.GOODSLIST_DATA;
	private final TradeListData tradeListData = DataManager.TRADE_LIST_DATA;
	/** NPC ID 到限购交易数据 / NPC id to limited-trade data*/
	private final Map<Integer, LimitedTradeNpc> limitedTradeNpcs = new HashMap<>();

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
				GameCronServices.cronService().schedule(() -> limitedItem.setToDefault(), limitedItem.getSalesTime());
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
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static LimitedItemTradeService getInstance() {
		ObjectProvider<LimitedItemTradeService> provider = instanceProvider;
		LimitedItemTradeService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("LimitedItemTradeService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
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
}
