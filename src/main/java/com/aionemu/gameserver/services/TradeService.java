package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.GoodsListData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import com.aionemu.gameserver.model.templates.goods.GoodsList;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.TradeinItem;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;
import com.aionemu.gameserver.model.trade.TradeItem;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.OverfowException;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.SafeMath;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 交易服务，处理 NPC 买卖、欧比斯商店与以物易物。
 * Trade service for NPC buy/sell, abyss shop, and trade-in.
 *
 * @author MATTY (ADev.Team)
 */
@Slf4j

public class TradeService {
	private static final TradeListData tradeListData = DataManager.TRADE_LIST_DATA;
	private static final GoodsListData goodsListData = DataManager.GOODSLIST_DATA;

	/**
	 * 从普通商店 NPC 购买物品。
	 * Performs a buy from a regular shop NPC.
	 *
	 * shop npc
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performBuyFromShop(Npc npc, Player player, TradeList tradeList) {
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		if (!validateBuyItems(npc, tradeList, player)) {
			PacketSendUtility.sendMessage(player, "Some items are not allowed to be sold by this npc.");
			return false;
		}
		Storage inventory = player.getInventory();
		int tradeModifier = tradeListData.getTradeListTemplate(npc.getNpcId()).getSellPriceRate();
		if (!tradeList.calculateBuyListPrice(player, tradeModifier)) {
			return false;
		}
		if (!tradeList.calculateRewardBuyListPrice(player)) {
			return false;
		}
		if (!hasInventorySpace(inventory, tradeList)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
			return false;
		}
		long tradeListPrice = tradeList.getRequiredKinah();
		LimitedItemTradeService limitedItemService = GameRuntimeServices.limitedItemTradeService();
		synchronized (limitedItemService) {
			Map<LimitedItem, Long> limitedItems = getLimitedItems(limitedItemService, npc.getNpcId(), tradeList);
			if (limitedItems == null || !canPurchaseLimitedItems(player, limitedItems)) {
				return false;
			}
			if (!decreaseRequiredItems(player, tradeList, "Possible hack. Not removed items on buy in shop.")) {
				return false;
			}
			inventory.decreaseKinah(tradeListPrice);
			applyLimitedItems(player, limitedItems);
			for (TradeItem tradeItem : tradeList.getTradeItems()) {
				long count = ItemService.addItem(player, tradeItem.getItemTemplate().getTemplateId(), tradeItem.getCount());
				if (count != 0) {
					log.warn(I18n.get("log.d629f8029e04", player.getObjectId(), tradeItem.getItemTemplate().getTemplateId(),
							tradeItem.getCount(), count));
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 从欧比斯商店以 AP 购买物品。
	 * Performs a buy from an abyss shop using AP.
	 *
	 * shop npc
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performBuyFromAbyssShop(Npc npc, Player player, TradeList tradeList) {
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		if (!validateBuyItems(npc, tradeList, player)) {
			PacketSendUtility.sendMessage(player, "Some items are not allowed to be selled from this npc");
			return false;
		}
		Storage inventory = player.getInventory();
		if (!tradeList.calculateAbyssBuyListPrice(player)) {
			return false;
		}
		if (tradeList.getRequiredAp() < 0) {
			AuditLogger.info(player, "Posible client hack. tradeList.getRequiredAp() < 0");
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300927));
			return false;
		}
		if (!hasInventorySpace(inventory, tradeList)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300762));
			return false;
		}
		LimitedItemTradeService limitedItemService = GameRuntimeServices.limitedItemTradeService();
		synchronized (limitedItemService) {
			Map<LimitedItem, Long> limitedItems = getLimitedItems(limitedItemService, npc.getNpcId(), tradeList);
			if (limitedItems == null || !canPurchaseLimitedItems(player, limitedItems)) {
				return false;
			}
			if (!decreaseRequiredItems(player, tradeList, "Possible hack. Not removed items on buy in abyss shop.")) {
				return false;
			}
			AbyssPointsService.addAp(player, -tradeList.getRequiredAp());
			applyLimitedItems(player, limitedItems);
			for (TradeItem tradeItem : tradeList.getTradeItems()) {
				long count = ItemService.addItem(player, tradeItem.getItemTemplate().getTemplateId(), tradeItem.getCount());
				if (count != 0) {
					log.warn(I18n.get("log.d629f8029e04", player.getObjectId(), tradeItem.getItemTemplate().getTemplateId(),
							tradeItem.getCount(), count));
					return false;
				}
				if (tradeItem.getCount() > 1) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300785,
							new DescriptionId(tradeItem.getItemTemplate().getNameId()), tradeItem.getCount()));
				} else {
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1300784, new DescriptionId(tradeItem.getItemTemplate().getNameId())));
				}
			}
		}
		return true;
	}

	/**
	 * 从奖励商店购买物品。
	 * Performs a buy from a reward shop.
	 *
	 * shop npc
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performBuyFromRewardShop(Npc npc, Player player, TradeList tradeList) {
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		} if (!validateBuyItems(npc, tradeList, player)) {
			PacketSendUtility.sendMessage(player, "Some items are not allowed to be selled from this npc");
			return false;
		}
		Storage inventory = player.getInventory();
		if (!tradeList.calculateRewardBuyListPrice(player)) {
			return false;
		} if (!hasInventorySpace(inventory, tradeList)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
			return false;
		}
		LimitedItemTradeService limitedItemService = GameRuntimeServices.limitedItemTradeService();
		synchronized (limitedItemService) {
			Map<LimitedItem, Long> limitedItems = getLimitedItems(limitedItemService, npc.getNpcId(), tradeList);
			if (limitedItems == null || !canPurchaseLimitedItems(player, limitedItems)) {
				return false;
			}
			if (!decreaseRequiredItems(player, tradeList, "Possible hack. Not removed items on buy in rewardshop.")) {
				return false;
			}
			applyLimitedItems(player, limitedItems);
			for (TradeItem tradeItem : tradeList.getTradeItems()) {
				long count = ItemService.addItem(player, tradeItem.getItemTemplate().getTemplateId(), tradeItem.getCount());
				if (count != 0) {
					log.warn(I18n.get("log.d629f8029e04", player.getObjectId(), tradeItem.getItemTemplate().getTemplateId(),
							tradeItem.getCount(), count));
					return false;
				}
			}
		}
		return true;
	}

	static boolean hasInventorySpace(Storage inventory, TradeList tradeList) {
		Map<Integer, Long> requestedCounts = new HashMap<>();
		Map<Integer, ItemTemplate> templates = new HashMap<>();
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			requestedCounts.merge(tradeItem.getItemId(), tradeItem.getCount(), Long::sum);
			templates.put(tradeItem.getItemId(), tradeItem.getItemTemplate());
		}

		return ItemService.canAddItems(inventory, requestedCounts, templates);
	}

	private static boolean decreaseRequiredItems(Player player, TradeList tradeList, String auditMessage) {
		if (!ItemService.decreaseItems(player, tradeList.getRequiredItems())) {
			AuditLogger.info(player, auditMessage);
			return false;
		}
		return true;
	}

	private static Map<LimitedItem, Long> getLimitedItems(LimitedItemTradeService service, int npcId,
			TradeList tradeList) {
		Map<LimitedItem, Long> limitedItems = new HashMap<>();
		try {
			for (TradeItem tradeItem : tradeList.getTradeItems()) {
				LimitedItem limitedItem = service.getLimitedItem(tradeItem.getItemId(), npcId);
				if (limitedItem != null) {
					limitedItems.merge(limitedItem, tradeItem.getCount(), Math::addExact);
				}
			}
		} catch (ArithmeticException e) {
			return null;
		}
		return limitedItems;
	}

	static boolean canPurchaseLimitedItems(Player player, Map<LimitedItem, Long> limitedItems) {
		for (Map.Entry<LimitedItem, Long> entry : limitedItems.entrySet()) {
			LimitedItem item = entry.getKey();
			long count = entry.getValue();
			if (count < 1 || count > Integer.MAX_VALUE) {
				return false;
			}
			if (item.getDefaultSellLimit() != 0 && item.getSellLimit() < count) {
				return false;
			}
			long bought = item.getBuyCount().getOrDefault(player.getObjectId(), 0);
			if (item.getBuyLimit() != 0 && bought + count > item.getBuyLimit()) {
				return false;
			}
		}
		return true;
	}

	private static void applyLimitedItems(Player player, Map<LimitedItem, Long> limitedItems) {
		for (Map.Entry<LimitedItem, Long> entry : limitedItems.entrySet()) {
			LimitedItem item = entry.getKey();
			int count = entry.getValue().intValue();
			if (item.getDefaultSellLimit() != 0) {
				item.setSellLimit(item.getSellLimit() - count);
			}
			if (item.getBuyLimit() != 0) {
				item.getBuyCount().merge(player.getObjectId(), count, Integer::sum);
			}
		}
	}

	private static boolean validateBuyItems(Npc npc, TradeList tradeList, Player player) {
		TradeListTemplate tradeListTemplate = tradeListData
				.getTradeListTemplate(npc.getObjectTemplate().getTemplateId());
		Set<Integer> allowedItems = new HashSet<Integer>();
		for (TradeTab tradeTab : tradeListTemplate.getTradeTablist()) {
			GoodsList goodsList = goodsListData.getGoodsListById(tradeTab.getId());
			if (goodsList != null && goodsList.getItemIdList() != null) {
				allowedItems.addAll(goodsList.getItemIdList());
			}
		}
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			if (tradeItem.getCount() < 1) {
				AuditLogger.info(player, "BUY packet hack item count < 1!");
				return false;
			}
			if (!allowedItems.contains(tradeItem.getItemId())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 向商店出售物品换取基纳。
	 * Sells items to a shop for Kinah.
	 *
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performSellToShop(Player player, TradeList tradeList) {
		Storage inventory = player.getInventory();
		long kinahReward = 0;
		List<Item> items = new ArrayList<Item>();
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			Item item = inventory.getItemByObjId(tradeItem.getItemId());
			if (item == null) {
				return false;
			}
			if (!item.isSellable()) {
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1300344, new DescriptionId(item.getNameId())));
				return false;
			}
			Item repurchaseItem = null;
			long sellReward = PricesService.getKinahForSell(item.getItemTemplate().getPrice(), player.getRace());
			long realReward = sellReward * tradeItem.getCount();
			if (!PlayerLimitService.updateSellLimit(player, realReward)) {
				break;
			}
			if (item.getItemCount() - tradeItem.getCount() < 0) {
				AuditLogger.info(player, "Trade exploit, sell item count big");
				return false;
			} else if (item.getItemCount() - tradeItem.getCount() == 0) {
				inventory.delete(item);
				repurchaseItem = item;
			} else if (item.getItemCount() - tradeItem.getCount() > 0) {
				repurchaseItem = ItemFactory.newItem(item.getItemId(), tradeItem.getCount());
				inventory.decreaseItemCount(item, tradeItem.getCount());
			} else {
				return false;
			}
			kinahReward += realReward;
			repurchaseItem.setRepurchasePrice(realReward);
			items.add(repurchaseItem);
		}
		GameFeatureServices.repurchaseService().addRepurchaseItems(player, items);
		inventory.increaseKinah(kinahReward);
		return true;
	}

	/**
	 * 执行以物易物（Trade-in）购买。
	 * Performs a trade-in purchase.
	 *
	 * @param player 玩家 / player
	 * @param npcObjectId NPC 对象 ID / npc object id
	 * @param itemId 目标物品 ID / target item id
	 * @param count 数量 / count
	 * @param TradeinListCount 换购列表数量 / trade-in list count
	 * @param TradeinItemObjectId1 换购物品对象 ID 1 / trade-in item object id 1
	 * @param TradeinItemObjectId2 换购物品对象 ID 2 / trade-in item object id 2
	 * @param TradeinItemObjectId3 换购物品对象 ID 3 / trade-in item object id 3
	 * @return 是否成功 / whether successful
	 */
	public static boolean performBuyFromTradeInTrade(Player player, int npcObjectId, int itemId, int count,
			int TradeinListCount, int TradeinItemObjectId1, int TradeinItemObjectId2, int TradeinItemObjectId3) {
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		if (count < 1) {
			return false;
		}
		VisibleObject visibleObject = player.getKnownList().getObject(npcObjectId);
		if (visibleObject == null || !(visibleObject instanceof Npc)
				|| MathUtil.getDistance(visibleObject, player) > 10) {
			return false;
		}
		int npcId = ((Npc) visibleObject).getNpcId();
		TradeListTemplate tradeInList = tradeListData.getTradeInListTemplate(npcId);
		if (tradeInList == null) {
			return false;
		}
		boolean valid = false;
		for (TradeTab tab : tradeInList.getTradeTablist()) {
			GoodsList goodList = goodsListData.getGoodsInListById(tab.getId());
			if (goodList.getItemIdList().contains(itemId)) {
				valid = true;
				break;
			}
		}
		if (!valid) {
			return false;
		}
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (itemTemplate == null || !ItemService.canAddItem(player, itemId, count)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
			return false;
		}
		Map<Integer, Long> requiredItems = new HashMap<>();
		try {
			for (TradeinItem treadInList : itemTemplate.getTradeinList().getTradeinItem()) {
				requiredItems.merge(treadInList.getId(), (long) SafeMath.multSafe(treadInList.getPrice(), count), Math::addExact);
			}
		} catch (OverfowException e) {
			AuditLogger.info(player, "OverfowException using tradeInTrade " + e.getMessage());
			return false;
		} catch (ArithmeticException e) {
			return false;
		}
		if (!ItemService.decreaseItems(player, requiredItems)) {
			return false;
		}
		long remaining = ItemService.addItem(player, itemId, count);
		if (remaining != 0) {
			long granted = count - remaining;
			if (granted > 0) {
				player.getInventory().decreaseByItemId(itemId, granted);
			}
			for (Map.Entry<Integer, Long> entry : requiredItems.entrySet()) {
				ItemService.addItem(player, entry.getKey(), entry.getValue());
			}
			return false;
		}
		return true;
	}

		/**
	 * 向商店出售物品换取 AP。
	 * Sells items to a shop for AP.
	 *
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performSellForAPToShop(Player player, TradeList tradeList,
			TradeListTemplate purchaseTemplate)
	{
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		Storage inventory = player.getInventory();
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			int itemObjectId = tradeItem.getItemId();
			long count = tradeItem.getCount();
			int priceModifier = purchaseTemplate.getApBuyPriceRate();
			Item item = inventory.getItemByObjId(itemObjectId);
			if (item == null) {
				return false;
			}
			int itemId = item.getItemId();
			boolean valid = false;
			for (TradeTab tab : purchaseTemplate.getTradeTablist()) {
				GoodsList goodList = goodsListData.getGoodsPurchaseListById(tab.getId());
				if (goodList.getItemIdList().contains(itemId)) {
					valid = true;
					break;
				}
			}
			if (!valid) {
				return false;
			}
			if (inventory.decreaseByObjectId(itemObjectId, count)) {
				AbyssPointsService.addAp(player, (item.getItemTemplate().getAcquisition().getRequiredAp() * priceModifier / 1000) * (int) count);
			}
		}
		return true;
	}

	/**
	 * 出售损坏物品以回收 AP。
	 * Sells broken items to reclaim AP.
	 *
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performSellBrokenAPItems(Player player, TradeList tradeList) {
		int apReward = 0;
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		Storage inventory = player.getInventory();
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			int itemObjectId = tradeItem.getItemId();
			long count = tradeItem.getCount();
			Item item = inventory.getItemByObjId(itemObjectId);
			if (item == null) {
				return false;
			}
			int itemId = item.getItemId();
			if (inventory.decreaseByItemId(itemId, count)) {
				int templateAP = (item.getItemTemplate().getAcquisition().getRequiredAp() * (int) count) / 5;
				apReward += templateAP;
			}
		}
		AbyssPointsService.addAp(player, apReward);
		return true;
	}

		/**
	 * 按模板配置向商店出售物品换取基纳。
	 * Sells items to a shop for Kinah using trade template pricing.
	 *
	 * 玩家 / player
	 * 交易列表 / trade list
	 * whether successful
	 */
	public static boolean performSellForKinahToShop(Player player, TradeList tradeList,
			TradeListTemplate purchaseTemplate) {
		if (!RestrictionsManager.canTrade(player)) {
			return false;
		}
		Storage inventory = player.getInventory();
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			int itemObjectId = tradeItem.getItemId();
			long count = tradeItem.getCount();
			Item item = inventory.getItemByObjId(itemObjectId);
			if (item == null) {
				return false;
			}
			long purchaseListPrice = PricesService.getKinahForSell(item.getItemTemplate().getPrice(), player.getRace());
			int itemId = item.getItemId();
			boolean valid = false;
			for (TradeTab tab : purchaseTemplate.getTradeTablist()) {
				GoodsList goodList = goodsListData.getGoodsPurchaseListById(tab.getId());
				if (goodList.getItemIdList().contains(itemId)) {
					valid = true;
					break;
				}
			}
			if (!valid) {
				return false;
			}
			if (inventory.decreaseByObjectId(itemObjectId, count)) {
				inventory.increaseKinah(purchaseListPrice);
			}
		}
		return true;
	}

	/**
	 * 处理已终止/失效物品退回商店逻辑。
	 * Handles returning a terminated item to shop logic.
	 *
	 * @param player 玩家 / player
	 * @param objId 物品对象 ID / item object id
	 */
	public static void terminatedItemToShop(Player player, int objId) {
		try {
			Storage inventory = player.getInventory();

			int itemObjectId = objId;

			Item item = inventory.getItemByObjId(itemObjectId);
			long count = item.getItemCount();

			if (inventory.decreaseByObjectId(itemObjectId, count)) {
				long price = (long) ((item.getItemTemplate().getPrice() * 0.2f) * (int) count);
				inventory.increaseKinah(price);
			}
		} catch (NullPointerException e) {
			log.info(I18n.get("log.b0750906232e", objId));
			return;
		}
	}

	/**
	 * 获取交易列表静态数据。
	 * Returns trade list static data.
	 *
	 * @return 交易列表数据 / trade list data
	 */
	public static TradeListData getTradeListData() {
		return tradeListData;
	}

	/**
	 * 获取商品列表静态数据。
	 * Returns goods list static data.
	 *
	 * @return 商品列表数据 / goods list data
	 */
	public static GoodsListData getGoodsListData() {
		return goodsListData;
	}
}
