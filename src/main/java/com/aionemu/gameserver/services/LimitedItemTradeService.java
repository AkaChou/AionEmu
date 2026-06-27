/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.GoodsListData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import com.aionemu.gameserver.model.limiteditems.LimitedTradeNpc;
import com.aionemu.gameserver.model.templates.goods.GoodsList;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitedItemTradeService {
	private static final Logger log = LoggerFactory.getLogger(LimitedItemTradeService.class);
	private static volatile ObjectProvider<LimitedItemTradeService> instanceProvider;
	private GoodsListData goodsListData = DataManager.GOODSLIST_DATA;
	private TradeListData tradeListData = DataManager.TRADE_LIST_DATA;
	private Map<Integer, LimitedTradeNpc> limitedTradeNpcs = new LinkedHashMap<Integer, LimitedTradeNpc>();

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
				if (!limitedTradeNpcs.containsKey(npcId)) {
					limitedTradeNpcs.putIfAbsent(npcId, new LimitedTradeNpc(limitedItems));
				} else {
					limitedTradeNpcs.get(npcId).putLimitedItems(limitedItems);
				}
			}
		}
		for (LimitedTradeNpc limitedTradeNpc : limitedTradeNpcs.values()) {
			for (final LimitedItem limitedItem : limitedTradeNpc.getLimitedItems()) {
				CronService.getInstance().schedule(new Runnable() {
					@Override
					public void run() {
						limitedItem.setToDefault();
					}
				}, limitedItem.getSalesTime());
			}
		}
		log.info("Scheduled Limited Items based on cron expression size: " + limitedTradeNpcs.size());
	}

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

	public boolean isLimitedTradeNpc(int npcId) {
		return limitedTradeNpcs.containsKey(npcId);
	}

	public LimitedTradeNpc getLimitedTradeNpc(int npcId) {
		return limitedTradeNpcs.get(npcId);
	}

	public static LimitedItemTradeService getInstance() {
		ObjectProvider<LimitedItemTradeService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	public static void setInstanceProvider(ObjectProvider<LimitedItemTradeService> instanceProvider) {
		LimitedItemTradeService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final LimitedItemTradeService INSTANCE = new LimitedItemTradeService();
	}
}
