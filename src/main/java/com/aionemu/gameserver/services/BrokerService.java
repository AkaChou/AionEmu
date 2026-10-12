package com.aionemu.gameserver.services;


import com.aionemu.gameserver.persistence.DatabaseAccess;
import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.BrokerConfig;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.broker.BrokerItemMask;
import com.aionemu.gameserver.model.broker.BrokerMessages;
import com.aionemu.gameserver.model.broker.BrokerPlayerCache;
import com.aionemu.gameserver.model.broker.BrokerRace;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BROKER_SERVICE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.persistence.dao.BrokerDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 经纪行（交易行）服务：上架、购买、结算、缓存与周期落库。
 * Broker (auction house) service: listing, buying, settlement, player cache and periodic DB save.
 *
 * @author kosyachok
 * @author ATracer
 * @author Antraxx
 */
@Slf4j(topic = "EXCHANGE_LOG")
public class BrokerService {

	private final ConcurrentMap<Integer, BrokerItem> elyosBrokerItems = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, BrokerItem> elyosSettledItems = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, BrokerItem> asmodianBrokerItems = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, BrokerItem> asmodianSettledItems = new ConcurrentHashMap<>();
	private Future<?> expiredItemsTask;
	private final ConcurrentMap<Integer, BrokerPlayerCache> playerBrokerCache = new ConcurrentHashMap<>();
	private static volatile ObjectProvider<BrokerService> instanceProvider;
	private final BrokerDao brokerDAO;
	private final InventoryDao inventoryDAO;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static BrokerService getInstance() {
		ObjectProvider<BrokerService> provider = instanceProvider;
		BrokerService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BrokerService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * @param instanceProvider Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<BrokerService> instanceProvider) {
		BrokerService.instanceProvider = instanceProvider;
	}

	/**
	 * 使用指定的 BrokerDAO 与 InventoryDAO 构造经纪行服务。
	 * Constructs the broker service with the given BrokerDAO and InventoryDAO.
	 *
	 * @param brokerDAO 经纪行数据访问对象 / Broker DAO
	 * @param inventoryDAO 物品数据访问对象 / Inventory DAO
	 */
	public BrokerService(BrokerDao brokerDAO, InventoryDao inventoryDAO) {
		this.brokerDAO = Objects.requireNonNull(brokerDAO, "brokerDAO must not be null");
		this.inventoryDAO = Objects.requireNonNull(inventoryDAO, "inventoryDAO must not be null");
		initBrokerService();
		scheduleExpiredItemsTask();
	}

	/**
	 * 从数据库重新加载经纪行在售与已结算物品。
	 * Reload broker listed and settled items from the database.
	 */
	public synchronized void reload() {
		if (expiredItemsTask != null) {
			expiredItemsTask.cancel(false);
		}
		scheduleExpiredItemsTask();
	}

	private void scheduleExpiredItemsTask() {
		int delay = Math.max(BrokerConfig.CHECK_EXPIRED_ITEMS_INTERVAL * 1000, 60000);
		expiredItemsTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(this::checkExpiredItems, delay, delay);
	}

	private void initBrokerService() {
		log.info(I18n.get("log.907fa7107b89"));
		int loadedBrokerItemsCount = 0;
		int loadedSettledItemsCount = 0;

		List<BrokerItem> brokerItems = brokerDAO.loadBroker();

		for (BrokerItem item : brokerItems) {
			BrokerRace race = item.getItemBrokerRace();
			if (race == BrokerRace.ASMODIAN) {
				(item.isSettled() ? asmodianSettledItems : asmodianBrokerItems).put(item.getItemUniqueId(), item);
			} else if (race == BrokerRace.ELYOS) {
				(item.isSettled() ? elyosSettledItems : elyosBrokerItems).put(item.getItemUniqueId(), item);
			} else {
				continue;
			}
			if (item.isSettled()) {
				loadedSettledItemsCount++;
			} else {
				loadedBrokerItemsCount++;
			}
		}
		log.info(I18n.get("log.e177a3708142", loadedBrokerItemsCount, loadedSettledItemsCount));
	}

	/**
	 * 按客户端筛选/排序条件向玩家展示经纪行物品列表。
	 * Show broker items to the player using client filter/sort criteria.
	 *
	 * @param player     请求玩家 / requesting player
	 * @param clientMask 客户端筛选掩码 / client filter mask
	 * @param sortType   排序类型 / sort type
	 * @param startPage  起始页 / start page
	 * @param itemList   指定物品模板 ID 列表，可空 / optional item template id list
	 */
	public void showRequestedItems(Player player, int clientMask, int sortType, int startPage, List<Integer> itemList) {
		BrokerItem[] searchItems;
		int playerBrokerMaskCache = getPlayerMask(player);
		BrokerItemMask brokerMaskById = BrokerItemMask.getBrokerMaskById(clientMask);
		boolean isChildrenMask = brokerMaskById.isChildrenMask(playerBrokerMaskCache);
		if (itemList != null && clientMask == 0) {
			Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
			if (brokerItems == null) {
				return;
			}
			searchItems = brokerItems.values().toArray(new BrokerItem[0]);
		} else if ((getFilteredItems(player).length == 0 || !isChildrenMask) && clientMask != 0) {
			searchItems = getItemsByMask(player, clientMask, false);
		} else if (isChildrenMask) {
			searchItems = getItemsByMask(player, clientMask, true);
		} else {
			searchItems = getFilteredItems(player);
		}

		if (searchItems == null) {
			return;
		}

		int totalSearchItemsCount = searchItems.length;

		getPlayerCache(player).setBrokerSortTypeCache(sortType);
		getPlayerCache(player).setBrokerStartPageCache(startPage);

		if (itemList != null) {
			List<BrokerItem> itemsFound = new ArrayList<>();
			for (BrokerItem item : searchItems) {
				if (itemList.contains(item.getItemId())) {
					itemsFound.add(item);
				}
			}
			getPlayerCache(player).setSearchItemsList(itemList);
			searchItems = itemsFound.toArray(new BrokerItem[0]);
			totalSearchItemsCount = searchItems.length;
			getPlayerCache(player).setBrokerListCache(searchItems);
		} else {
			getPlayerCache(player).setSearchItemsList(null);
		}

		sortBrokerItems(searchItems, sortType);
		searchItems = getRequestedPage(searchItems, startPage);

		PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(searchItems, totalSearchItemsCount, startPage));
	}

	private BrokerItem[] getItemsByMask(Player player, int clientMask, boolean cached) {
		BrokerItemMask brokerMask = BrokerItemMask.getBrokerMaskById(clientMask);
		Iterable<BrokerItem> source;
		if (cached) {
			BrokerItem[] brokerItems = getFilteredItems(player);
			if (brokerItems == null) {
				return null;
			}
			source = Arrays.asList(brokerItems);
		} else {
			Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
			if (brokerItems == null) {
				return null;
			}
			source = brokerItems.values();
		}

		List<BrokerItem> searchItems = new ArrayList<>();
		for (BrokerItem item : source) {
			if (item != null && item.getItem() != null && brokerMask.isMatches(item.getItem())) {
				searchItems.add(item);
			}
		}

		BrokerItem[] items = searchItems.toArray(new BrokerItem[0]);
		getPlayerCache(player).setBrokerListCache(items);
		getPlayerCache(player).setBrokerMaskCache(clientMask);

		return items;
	}

	/**
	 * 按排序类型执行排序。 / Perform sorting according to sort type.
	 */
	private void sortBrokerItems(BrokerItem[] brokerItems, int sortType) {
		Arrays.sort(brokerItems, BrokerItem.getComparatoryByType(sortType));
	}

	private BrokerItem[] getRequestedPage(BrokerItem[] brokerItems, int startPage) {
		int fromIndex = startPage * 9;
		if (fromIndex >= brokerItems.length) {
			return new BrokerItem[0];
		}
		int toIndex = Math.min(fromIndex + 45, brokerItems.length);
		return Arrays.copyOfRange(brokerItems, fromIndex, toIndex);
	}

	private ConcurrentMap<Integer, BrokerItem> getRaceBrokerItems(Race race) {
		if (race == Race.ELYOS) {
			return elyosBrokerItems;
		} else if (race == Race.ASMODIANS) {
			return asmodianBrokerItems;
		}
		return null;
	}

	private ConcurrentMap<Integer, BrokerItem> getRaceBrokerSettledItems(Race race) {
		if (race == Race.ELYOS) {
			return elyosSettledItems;
		} else if (race == Race.ASMODIANS) {
			return asmodianSettledItems;
		}
		return null;
	}

	private static BrokerRace toBrokerRace(Race race) {
		if (race == Race.ASMODIANS) {
			return BrokerRace.ASMODIAN;
		} else if (race == Race.ELYOS) {
			return BrokerRace.ELYOS;
		}
		return null;
	}

	/**
	 * 购买经纪行物品（支持拆分售卖数量）。
	 * Buy a broker item (supports split-sell quantities).
	 *
	 * @param player       买家 / buyer
	 * @param itemUniqueId 经纪行物品唯一 ID / broker item unique id
	 * @param itemCount    购买数量 / quantity to buy
	 */
	public void buyBrokerItem(Player player, int itemUniqueId, long itemCount) {
		boolean isEmptyCache = getFilteredItems(player).length == 0;
		Race playerRace = player.getRace();
		Map<Integer, BrokerItem> raceBrokerItems = getRaceBrokerItems(playerRace);
		if (raceBrokerItems == null) {
			return;
		}
		BrokerItem buyingItem = raceBrokerItems.get(itemUniqueId);
		if (!RestrictionsManager.canTrade(player) || buyingItem == null || itemCount <= 0) {
			return;
		}
		if ((buyingItem.isSold() || buyingItem.isSettled()) && (buyingItem.getItem() != null)) {
			PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_VENDOR_SOLD_OUT(buyingItem.getItem().getNameId()));
			return;
		}
		if (SecurityConfig.BROKER_PREBUY_CHECK) {
			if (!brokerDAO.preBuyCheck(itemUniqueId)) {
				PacketSendUtility.sendMessage(player, "Sorry, but this item already sold");
				return;
			}
		}
		if (buyingItem.getSellerId() == player.getObjectId()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_VENDOR_CAN_NOT_BUY_MY_REGISTER_ITEM);
			return;
		}
		synchronized (this) {
			if (itemCount > buyingItem.getItemCount()) {
				long attemptedTotal = (long) (((float) buyingItem.getPrice() / buyingItem.getItemCount()) * itemCount);
				if (BrokerConfig.ANTI_HACK_PUNISHMENT == 0) {
					PacketSendUtility.sendMessage(player,
						"Sorry, you can not buy items more than total count! are you hacking!");
				} else if (BrokerConfig.ANTI_HACK_PUNISHMENT == 1) {
					PacketSendUtility.sendMessage(player,
						"Sorry, you can not buy items more than total count! are you hacking! you have been kicked from game due to malfunction data.");
					player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
				}
				log.info(I18n.get("log.bfe59bc3716e", player.getName(), buyingItem.getItemId(),
					(buyingItem.getItemCount() + itemCount), itemCount, (LoggingConfig.ENABLE_ADVANCED_LOGGING
						? " [Item Name: " + buyingItem.getItem().getItemName() : "]"), buyingItem.getSeller(), attemptedTotal));
				return;
			}
			if (buyingItem.isSold() || buyingItem.isCanceled()) {
				PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_VENDOR_SOLD_OUT(buyingItem.getItem().getNameId()));
				return;
			}
			long price = buyingItem.getPrice();
			boolean isBuyWholeItem = itemCount == buyingItem.getItemCount();
			long totalBuyPrice = isBuyWholeItem ? price
				: (long) (((double) price / buyingItem.getItemCount()) * itemCount);
			Item item = buyingItem.getItem();
			if (player.getInventory().isFull(item.getItemTemplate().getExtraInventoryId())) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
				return;
			}
			if (player.getInventory().getKinah() < totalBuyPrice) {
				return;
			}
			long originalBrokerCount = buyingItem.getItemCount();
			long originalBrokerPrice = buyingItem.getPrice();
			boolean originalSold = buyingItem.isSold();
			boolean originalSettled = buyingItem.isSettled();
			Timestamp originalSettleTime = buyingItem.getSettleTime();
			PersistentState originalBrokerState = buyingItem.getPersistentState();
			long originalItemCount = item.getItemCount();
			int originalItemLocation = item.getItemLocation();
			PersistentState originalItemState = item.getPersistentState();
			Item kinahItem = player.getInventory().getKinahItem();
			PersistentState originalKinahState = kinahItem.getPersistentState();
			BrokerItem splitSettledItem = null;
			Item itemToBuy = item;
			List<BrokerItem> brokerChanges;
			List<InventorySave> inventoryChanges = new ArrayList<>();
			if (isBuyWholeItem) {
				buyingItem.removeItem();
				buyingItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				brokerChanges = List.of(buyingItem);
			} else {
				item.setItemCount(buyingItem.getItemCount() - itemCount);
				buyingItem.setItemCount(buyingItem.getItemCount() - itemCount);
				buyingItem.setPrice(price - totalBuyPrice);
				buyingItem.setPersistentState(PersistentState.UPDATE_ITEM_BROKER);
				itemToBuy = ItemFactory.newItem(item.getItemId(), itemCount);
				copyItemInfo(item, itemToBuy);
				splitSettledItem = createSplitSale(playerRace, buyingItem, itemToBuy, totalBuyPrice, itemCount);
				brokerChanges = List.of(buyingItem, splitSettledItem);
				inventoryChanges.add(new InventorySave(buyingItem.getSellerId(), List.of(item)));
			}
			player.getInventory().decreaseKinah(totalBuyPrice);
			Item boughtItem = player.getInventory().add(itemToBuy);
			if (boughtItem == null) {
				restoreFailedPurchase(player, buyingItem, item, itemToBuy, totalBuyPrice, originalBrokerCount,
					originalBrokerPrice, originalSold, originalSettled, originalSettleTime, originalBrokerState,
					originalItemCount, originalItemLocation, originalItemState, originalKinahState, !isBuyWholeItem);
				return;
			}
			inventoryChanges.add(new InventorySave(player.getObjectId(), List.of(boughtItem, kinahItem)));
			if (!new BrokerOpSaveTask(brokerChanges, inventoryChanges).save()) {
				restoreFailedPurchase(player, buyingItem, item, boughtItem, totalBuyPrice, originalBrokerCount,
					originalBrokerPrice, originalSold, originalSettled, originalSettleTime, originalBrokerState,
					originalItemCount, originalItemLocation, originalItemState, originalKinahState, !isBuyWholeItem);
				return;
			}
			Map<Integer, BrokerItem> raceSettledItems = getRaceBrokerSettledItems(playerRace);
			if (raceSettledItems != null) {
				if (isBuyWholeItem) {
					raceBrokerItems.remove(itemUniqueId);
					raceSettledItems.put(itemUniqueId, buyingItem);
				} else {
					raceSettledItems.put(splitSettledItem.getItemUniqueId(), splitSettledItem);
				}
			}
			notifySellerOfSale(buyingItem);
			if (!isEmptyCache) {
				BrokerItem[] newCache;
				if (isBuyWholeItem) {
					newCache = ArrayUtils.removeElement(getFilteredItems(player), buyingItem);
				} else {
					int buyingItemIndex = ArrayUtils.indexOf(getFilteredItems(player), buyingItem);
					newCache = ArrayUtils.removeElement(getFilteredItems(player), buyingItem);
					List<BrokerItem> updatedCache = new ArrayList<>(Arrays.asList(newCache));
					updatedCache.add(buyingItemIndex, buyingItem);
					newCache = updatedCache.toArray(new BrokerItem[0]);
				}
				getPlayerCache(player).setBrokerListCache(newCache);
			}
		}
		showRequestedItems(player, getPlayerCache(player).getBrokerMaskCache(),
			getPlayerCache(player).getBrokerSortTypeCache(), getPlayerCache(player).getBrokerStartPageCache(),
			getPlayerCache(player).getSearchItemList());
	}

	private BrokerItem createSplitSale(Race race, BrokerItem brokerItem, Item newItem, long totalBuyPrice,
									   long buyItemCount) {
		BrokerRace brRace = toBrokerRace(race);
		if (brRace == null) {
			throw new IllegalArgumentException("Unsupported broker race " + race);
		}
		BrokerItem newBrokerItem = new BrokerItem(newItem, totalBuyPrice, brokerItem.getSeller(),
			brokerItem.getSellerId(), brRace, brokerItem.isSplitSell());
		newBrokerItem.setItemCount(buyItemCount);
		newBrokerItem.removeItem();
		newBrokerItem.setPersistentState(PersistentState.NEW);
		return newBrokerItem;
	}

	private void notifySellerOfSale(BrokerItem brokerItem) {
		Player seller = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(brokerItem.getSellerId());
		if (seller != null) {
			PacketSendUtility.sendPacket(seller, new SM_BROKER_SERVICE(true, getTotalSettledKinah(seller)));
			PacketSendUtility.sendPacket(seller, SM_SYSTEM_MESSAGE.STR_VENDOR_REGISTER_SOLD_OUT(brokerItem.getItem().getNameId()));
		}
	}

	private void restoreFailedPurchase(Player player, BrokerItem buyingItem, Item brokerItem, Item boughtItem,
									   long totalBuyPrice, long originalBrokerCount, long originalBrokerPrice, boolean originalSold,
									   boolean originalSettled, Timestamp originalSettleTime, PersistentState originalBrokerState,
									   long originalItemCount, int originalItemLocation, PersistentState originalItemState,
									   PersistentState originalKinahState, boolean releaseBoughtItemId) {
		if (player.getInventory().getItemByObjId(boughtItem.getObjectId()) != null) {
			player.getInventory().remove(boughtItem);
			PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(boughtItem.getObjectId()));
		}
		player.getInventory().increaseKinah(totalBuyPrice);
		player.getInventory().getKinahItem().setPersistentState(originalKinahState);
		brokerItem.setItemCount(originalItemCount);
		brokerItem.setItemLocation(originalItemLocation);
		brokerItem.setPersistentState(originalItemState);
		buyingItem.restoreTransactionState(originalBrokerCount, originalBrokerPrice, originalSold, originalSettled,
			originalSettleTime, originalBrokerState);
		if (releaseBoughtItemId) {
			ItemService.releaseItemId(boughtItem);
		}
	}

	/**
	 * 复制部分物品值（如镶嵌石与强化等级）。 / Copy some item values like item stones and enchant level
	 */
	private static void copyItemInfo(Item sourceItem, Item newItem) {
		newItem.setOptionalSocket(sourceItem.getOptionalSocket());
		newItem.setItemCreator(sourceItem.getItemCreator());
		if (sourceItem.hasManaStones()) {
			for (ManaStone manaStone : sourceItem.getItemStones()) {
				ItemSocketService.addManaStone(newItem, manaStone.getItemId());
			}
		}
		if (sourceItem.getGodStone() != null) {
			newItem.addGodStone(sourceItem.getGodStone().getItemId(), sourceItem.getGodStone().getActivatedCount());
		}
		if (sourceItem.getEnchantLevel() > 0) {
			newItem.setEnchantLevel(sourceItem.getEnchantLevel());
		}
		if (sourceItem.isSoulBound()) {
			newItem.setSoulBound(true);
		}
		newItem.setBonusNumber(sourceItem.getBonusNumber());
		newItem.setRandomStats(sourceItem.getRandomStats());
		newItem.setRandomCount(sourceItem.getRandomCount());
		newItem.setIdianStone(sourceItem.getIdianStone());
		newItem.setItemColor(sourceItem.getItemColor());
		newItem.setItemSkinTemplate(sourceItem.getItemSkinTemplate());
		newItem.setColorExpireTime(sourceItem.getColorExpireTime());
		newItem.setExpireTime(sourceItem.getExpireTime());
		newItem.setActivationCount(sourceItem.getActivationCount());
		newItem.setEquipped(sourceItem.isEquipped());
		newItem.setEquipmentSlot(sourceItem.getEquipmentSlot());
		newItem.setItemLocation(sourceItem.getItemLocation());
		newItem.setFusionedItem(sourceItem.getFusionedItemTemplate());
		newItem.setOptionalFusionSocket(sourceItem.getOptionalFusionSocket());
		newItem.setWrappableCount(sourceItem.getWrappableCount());
		newItem.setAuthorize(sourceItem.getAuthorize());
		newItem.setPacked(sourceItem.isPacked());
		newItem.setAmplification(sourceItem.isAmplified());
	}

	private boolean putToSettled(Race race, BrokerItem brokerItem) {
		long originalItemCount = brokerItem.getItemCount();
		long originalPrice = brokerItem.getPrice();
		boolean originalSold = brokerItem.isSold();
		boolean originalSettled = brokerItem.isSettled();
		Timestamp originalSettleTime = brokerItem.getSettleTime();
		PersistentState originalState = brokerItem.getPersistentState();

		brokerItem.setSettled();
		brokerItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
		if (!new BrokerOpSaveTask(brokerItem).save()) {
			brokerItem.restoreTransactionState(originalItemCount, originalPrice, originalSold, originalSettled,
				originalSettleTime, originalState);
			return false;
		}

		ConcurrentMap<Integer, BrokerItem> settledMap = getRaceBrokerSettledItems(race);
		if (settledMap != null) {
			settledMap.put(brokerItem.getItemUniqueId(), brokerItem);
		}

		Player seller = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(brokerItem.getSellerId());
		if (seller != null) {
			PacketSendUtility.sendPacket(seller, new SM_BROKER_SERVICE(true, getTotalSettledKinah(seller)));
		}
		return true;
	}

	private int getRegisteredItemsCount(Player player) {
		Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
		if (brokerItems == null) {
			return 0;
		}
		int playerId = player.getObjectId();
		int count = 0;
		for (BrokerItem item : brokerItems.values()) {
			if (item != null && playerId == item.getSellerId()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 将玩家背包物品上架到经纪行。
	 * Register a player inventory item on the broker.
	 *
	 * @param player       卖家 / seller
	 * @param itemUniqueId 物品唯一 ID / item object id
	 * @param count        上架数量 / listed quantity
	 * @param PricePerItem 单价 / price per item
	 * @param isSplitSell  是否允许拆分售卖 / whether split-sell is enabled
	 */
	public void registerItem(Player player, int itemUniqueId, long count, long PricePerItem, boolean isSplitSell) {
		long TotalItemPrice = PricePerItem * count;
		Item itemToRegister = player.getInventory().getItemByObjId(itemUniqueId);
		Race playerRace = player.getRace();

		if (itemToRegister == null || count > itemToRegister.getItemCount()) {
			return;
		}

		if (!RestrictionsManager.canTrade(player)) {
			return;
		}

		if (PricePerItem <= 0 || PricePerItem > 999999999) {
			return;
		}

		// 检查物品是否未灵魂绑定 / check if item is not soulbound
		if (itemToRegister.isSoulBound()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_VENDOR_REGISTER_USED_ITEM);
			return;
		}

		// 检查交易漏洞 / Check Trade Hack
		if (!itemToRegister.isTradeable(player)) {
			return;
		}

		if (!GameRuntimeServices.adminService().canOperate(player, null, itemToRegister, "broker")) {
			return;
		}

		BrokerRace brRace = toBrokerRace(playerRace);
		if (brRace == null) {
			return;
		}

		int registeredItemsCount = getRegisteredItemsCount(player);
		if (registeredItemsCount > 14) {
			PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(BrokerMessages.NO_SPACE_AVAIABLE.getId()));
			return;
		}

		int registrationCommission = Math.round(TotalItemPrice * (registeredItemsCount > 9 ? 0.04f : 0.02f));
		if (registrationCommission < 10) {
			registrationCommission = 10;
		}

		if (player.getInventory().getKinah() < registrationCommission) {
			PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(BrokerMessages.NO_ENOUGHT_KINAH.getId()));
			return;
		}

		Item sourceItem = itemToRegister;
		int originalItemLocation = sourceItem.getItemLocation();
		PersistentState originalItemState = sourceItem.getPersistentState();
		Item kinahItem = player.getInventory().getKinahItem();
		PersistentState originalKinahState = kinahItem.getPersistentState();
		boolean splitStack = itemToRegister.getItemTemplate().isStackable() && count < itemToRegister.getItemCount();

		player.getInventory().decreaseKinah(registrationCommission);
		if (splitStack) {
			int itemId = itemToRegister.getItemId();
			player.getInventory().decreaseItemCount(itemToRegister, count);
			itemToRegister = ItemFactory.newItem(itemId, count);
		} else {
			player.getInventory().remove(itemToRegister);
			PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(itemToRegister.getObjectId()));
		}

		itemToRegister.setItemLocation(126);

		BrokerItem newBrokerItem = new BrokerItem(itemToRegister, TotalItemPrice, player.getName(),
			player.getObjectId(), brRace, isSplitSell);
		List<Item> inventoryChanges = new ArrayList<>();
		if (splitStack) {
			inventoryChanges.add(sourceItem);
		}
		inventoryChanges.add(itemToRegister);
		inventoryChanges.add(kinahItem);
		if (!new BrokerOpSaveTask(List.of(newBrokerItem),
			List.of(new InventorySave(player.getObjectId(), inventoryChanges))).save()) {
			player.getInventory().increaseKinah(registrationCommission);
			player.getInventory().getKinahItem().setPersistentState(originalKinahState);
			if (splitStack) {
				player.getInventory().increaseItemCount(sourceItem, count);
				sourceItem.setPersistentState(originalItemState);
				ItemService.releaseItemId(itemToRegister);
			} else {
				itemToRegister.setItemLocation(originalItemLocation);
				player.getInventory().add(itemToRegister);
				itemToRegister.setPersistentState(originalItemState);
			}
			return;
		}

		(brRace == BrokerRace.ASMODIAN ? asmodianBrokerItems : elyosBrokerItems).put(newBrokerItem.getItemUniqueId(), newBrokerItem);

		PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(newBrokerItem, 0, registeredItemsCount));
	}

	/**
	 * 计算并下发物品均价/最低/最高价窗口数据。
	 * Calculate and send average/low/high price window data for an item.
	 *
	 * @param player       请求玩家 / requesting player
	 * @param itemUniqueId 物品对象 ID / item unique id
	 */
	public void CalcItemAveLowHigh(Player player, int itemUniqueId) {
		Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
		Item targetItem = player.getInventory().getItemByObjId(itemUniqueId);
		if (brokerItems == null || brokerItems.isEmpty() || targetItem == null) {
			PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(itemUniqueId, 0, 0, 0, true));
			return;
		}

		int targetItemId = targetItem.getItemId();
		long low = Long.MAX_VALUE;
		long high = 0;
		long sum = 0;
		int count = 0;

		for (BrokerItem item : brokerItems.values()) {
			if (item != null && item.getItemId() == targetItemId) {
				long piecePrice = item.getPiecePrice();
				sum += piecePrice;
				if (piecePrice < low) {
					low = piecePrice;
				}
				if (piecePrice > high) {
					high = piecePrice;
				}
				count++;
			}
		}

		long currentLow = count > 0 ? low : 0;
		long currentHigh = count > 0 ? high : 0;
		long ave7day = count > 0 ? sum / count : 0;
		boolean isLowHighSame = currentLow == currentHigh;

		PacketSendUtility.sendPacket(player,
			new SM_BROKER_SERVICE(itemUniqueId, ave7day, currentLow, currentHigh, isLowHighSame));
	}

	/**
	 * 向玩家展示其已上架物品。
	 * Show the player their currently registered broker items.
	 *
	 * @param player 玩家 / player
	 */
	public void showRegisteredItems(Player player) {
		Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
		if (brokerItems == null) {
			return;
		}

		List<BrokerItem> registeredItems = new ArrayList<>();
		int playerId = player.getObjectId();

		for (BrokerItem item : brokerItems.values()) {
			if (item != null && item.getItem() != null && playerId == item.getSellerId()) {
				registeredItems.add(item);
			}
		}
		PacketSendUtility.sendPacket(player,
			new SM_BROKER_SERVICE(registeredItems.toArray(new BrokerItem[0])));
	}

	/**
	 * 判断玩家是否仍有上架中的物品。
	 * Whether the player still has items registered on the broker.
	 *
	 * @param player 玩家 / player
	 * @return 有上架物品为 true / true if registered items exist
	 */
	public boolean hasRegisteredItems(Player player) {
		Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
		if (brokerItems == null) {
			return false;
		}
		int playerId = player.getObjectId();
		for (BrokerItem item : brokerItems.values()) {
			if (item != null && item.getItem() != null && playerId == item.getSellerId()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 取消上架并退回物品。
	 * Cancel a registered listing and return the item.
	 *
	 * @param player       玩家 / player
	 * @param brokerItemId 经纪行物品 ID / broker item id
	 */
	public void cancelRegisteredItem(Player player, int brokerItemId) {
		Map<Integer, BrokerItem> brokerItems = getRaceBrokerItems(player.getRace());
		if (brokerItems == null) {
			return;
		}
		BrokerItem brokerItem = brokerItems.get(brokerItemId);

		if (brokerItem != null) {
			if (!brokerItem.getSeller().equals(player.getName())) {
				log.info(I18n.get("log.8d39bb046866", player.getName()));
				return;
			}
			if (player.getInventory().isFull(brokerItem.getItem().getItemTemplate().getExtraInventoryId())) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_VENDOR_FULL_INVENTORY);
				return;
			}
			synchronized (this) {
				if (brokerItems.get(brokerItemId) != brokerItem || brokerItem.isSold() || brokerItem.isSettled()
					|| brokerItem.isCanceled()) {
					return;
				}
				Item item = brokerItem.getItem();
				int originalItemLocation = item.getItemLocation();
				PersistentState originalItemState = item.getPersistentState();
				PersistentState originalBrokerState = brokerItem.getPersistentState();
				if (player.getInventory().add(item) == null) {
					return;
				}
				brokerItem.setPersistentState(PersistentState.DELETED);
				if (!new BrokerOpSaveTask(List.of(brokerItem),
					List.of(new InventorySave(player.getObjectId(), List.of(item)))).save()) {
					player.getInventory().remove(item);
					PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(item.getObjectId()));
					item.setItemLocation(originalItemLocation);
					item.setPersistentState(originalItemState);
					brokerItem.setPersistentState(originalBrokerState);
					return;
				}
				brokerItem.setIsCanceled(true);
				brokerItems.remove(brokerItemId);
			}
		}
		showRegisteredItems(player);
	}

	/**
	 * 向玩家展示已结算（可领取）物品/基纳。
	 * Show the player settled items/kinah available for collection.
	 *
	 * @param player 玩家 / player
	 */
	public void showSettledItems(Player player) {
		Map<Integer, BrokerItem> brokerSettledItems = getRaceBrokerSettledItems(player.getRace());
		if (brokerSettledItems == null) {
			return;
		}
		List<BrokerItem> settledItems = new ArrayList<>();
		int playerId = player.getObjectId();
		long totalKinah = 0;
		for (BrokerItem item : brokerSettledItems.values()) {
			if (item != null && playerId == item.getSellerId()) {
				settledItems.add(item);
				if (item.isSold()) {
					totalKinah += item.getPrice();
				}
			}
		}
		PacketSendUtility.sendPacket(player,
			new SM_BROKER_SERVICE(settledItems.toArray(new BrokerItem[0]), totalKinah));
	}

	/**
	 * 统计玩家待领取的经纪行基纳总额。
	 * Sum kinah waiting for collection for the given player common data.
	 *
	 * @param playerCommonData 玩家公共数据 / player common data
	 * @return 待领取基纳 / kinah to collect
	 */
	public long getCollectedMoney(PlayerCommonData playerCommonData) {
		return getTotalSettledKinah(playerCommonData.getRace(), playerCommonData.getPlayerObjId());
	}

	private long getTotalSettledKinah(Race race, int playerId) {
		Map<Integer, BrokerItem> brokerSettledItems = getRaceBrokerSettledItems(race);
		if (brokerSettledItems == null) {
			return 0;
		}
		long totalKinah = 0;
		for (BrokerItem item : brokerSettledItems.values()) {
			if (item != null && playerId == item.getSellerId() && item.isSold()) {
				totalKinah += item.getPrice();
			}
		}
		return totalKinah;
	}

	private long getTotalSettledKinah(Player player) {
		return getTotalSettledKinah(player.getRace(), player.getObjectId());
	}

	/**
	 * 结算账户：领取已售基纳与过期退回物品。
	 * Settle the account: collect sold kinah and expired returned items.
	 *
	 * @param player 玩家 / player
	 */
	public synchronized void settleAccount(Player player) {
		Race playerRace = player.getRace();
		Map<Integer, BrokerItem> brokerSettledItems = getRaceBrokerSettledItems(playerRace);
		if (brokerSettledItems == null) {
			return;
		}
		List<BrokerItem> collectedItems = new ArrayList<>();
		List<BrokerItem> brokerItemsToDelete = new ArrayList<>();
		List<Item> inventoryItems = new ArrayList<>();
		List<SettlementChange> settlementChanges = new ArrayList<>();
		int playerId = player.getObjectId();
		long kinahCollect = 0;
		boolean itemsLeft = false;

		for (BrokerItem item : brokerSettledItems.values()) {
			if (item.getSellerId() == playerId) {
				collectedItems.add(item);
			}
		}

		for (BrokerItem item : collectedItems) {
			if (item.isSold()) {
				settlementChanges.add(new SettlementChange(item, item.getPersistentState(), null, 0, null));
				item.setPersistentState(PersistentState.DELETED);
				brokerItemsToDelete.add(item);
				kinahCollect += item.getPrice();
			} else {
				if (item.getItem() != null) {
					Item returnedItem = item.getItem();
					int originalLocation = returnedItem.getItemLocation();
					PersistentState originalItemState = returnedItem.getPersistentState();
					Item resultItem = player.getInventory().add(returnedItem);
					if (resultItem != null) {
						settlementChanges.add(new SettlementChange(item, item.getPersistentState(), returnedItem,
							originalLocation, originalItemState));
						item.setPersistentState(PersistentState.DELETED);
						brokerItemsToDelete.add(item);
						inventoryItems.add(returnedItem);
					} else {
						itemsLeft = true;
					}
				} else {
					log.warn(I18n.get("log.8d30e44b916e", item.getItemUniqueId()));
				}
			}
		}
		Item kinahItem = player.getInventory().getKinahItem();
		PersistentState originalKinahState = kinahItem == null ? PersistentState.NOACTION : kinahItem.getPersistentState();
		if (kinahCollect > 0) {
			player.getInventory().increaseKinah(kinahCollect);
			kinahItem = player.getInventory().getKinahItem();
			inventoryItems.add(kinahItem);
		}

		List<InventorySave> inventoryChanges = inventoryItems.isEmpty() ? List.of()
			: List.of(new InventorySave(playerId, inventoryItems));
		if (!brokerItemsToDelete.isEmpty()
			&& !new BrokerOpSaveTask(brokerItemsToDelete, inventoryChanges).save()) {
			if (kinahCollect > 0) {
				player.getInventory().decreaseKinah(kinahCollect);
				kinahItem.setPersistentState(originalKinahState);
			}
			for (SettlementChange change : settlementChanges) {
				if (change.returnedItem() != null) {
					player.getInventory().remove(change.returnedItem());
					PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(change.returnedItem().getObjectId()));
					change.returnedItem().setItemLocation(change.itemLocation());
					change.returnedItem().setPersistentState(change.itemState());
				}
				change.brokerItem().setPersistentState(change.brokerState());
			}
			itemsLeft = true;
		} else {
			for (BrokerItem item : brokerItemsToDelete) {
				brokerSettledItems.remove(item.getItemUniqueId());
			}
		}

		showSettledItems(player);

		if (!itemsLeft) {
			PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(false, 0));
		}
	}

	private synchronized void checkExpiredItems() {
		long currentMillis = System.currentTimeMillis();
		checkExpiredItemsForRace(Race.ASMODIANS, asmodianBrokerItems, currentMillis);
		checkExpiredItemsForRace(Race.ELYOS, elyosBrokerItems, currentMillis);
	}

	private void checkExpiredItemsForRace(Race race, ConcurrentMap<Integer, BrokerItem> raceBrokerItems, long currentMillis) {
		for (BrokerItem item : raceBrokerItems.values()) {
			if (item != null && item.getExpireTime().getTime() <= currentMillis) {
				if (expireItem(race, item)) {
					raceBrokerItems.remove(item.getItemUniqueId());
				}
			}
		}
	}

	private boolean expireItem(Race race, BrokerItem item) {
		PersistentState originalState = item.getPersistentState();
		item.setPersistentState(PersistentState.DELETED);
		if (GameFeatureServices.systemMailService().sendSystemMail("$$VENDOR_RETURN_MAIL", "", "", item.getSeller(),
			item.getItem(), 0, 0, LetterType.NORMAL, con -> brokerDAO.storeInTransaction(con, item))) {
			item.setPersistentState(PersistentState.UPDATED);
			return true;
		}
		item.setPersistentState(originalState);
		return this.putToSettled(race, item);
	}

	/**
	 * 玩家登录时通知是否有经纪行结算可领。
	 * On login, notify the player if broker settlements are available.
	 *
	 * @param player logging-in player
	 */
	public void onPlayerLogin(Player player) {
		Map<Integer, BrokerItem> brokerSettledItems = getRaceBrokerSettledItems(player.getRace());
		if (brokerSettledItems == null) {
			return;
		}
		int playerId = player.getObjectId();
		for (BrokerItem item : brokerSettledItems.values()) {
			if (item != null && playerId == item.getSellerId()) {
				PacketSendUtility.sendPacket(player, new SM_BROKER_SERVICE(true, getTotalSettledKinah(player)));
				break;
			}
		}
	}

	private BrokerPlayerCache getPlayerCache(Player player) {
		return playerBrokerCache.computeIfAbsent(player.getObjectId(), _ -> new BrokerPlayerCache());
	}

	/**
	 * 移除玩家经纪行查询缓存。
	 * Remove the player broker query cache.
	 *
	 * @param player 玩家 / player
	 */
	public void removePlayerCache(Player player) {
		playerBrokerCache.remove(player.getObjectId());
	}

	private int getPlayerMask(Player player) {
		return getPlayerCache(player).getBrokerMaskCache();
	}

	private BrokerItem[] getFilteredItems(Player player) {
		return getPlayerCache(player).getBrokerListCache();
	}

	private record InventorySave(int playerId, List<Item> items) {
	}

	private record SettlementChange(BrokerItem brokerItem, PersistentState brokerState, Item returnedItem,
									int itemLocation, PersistentState itemState) {
	}

	/**
	 * 经纪行操作后的批量落库任务（一次提交相关物品变更）。
	 * Batch DB-save task used after any broker operation.
	 */
	public final class BrokerOpSaveTask implements Runnable {

		private final List<BrokerItem> brokerItems;
		private final List<InventorySave> inventorySaves;

		/**
		 * 构造落库任务，复用所属服务的 DAO 绑定。
		 * Constructs a save task reusing the owning service's DAO bindings.
		 *
		 * @param brokerItems 待存经纪行物品集合 / broker items to save
		 * @param inventorySaves 待存背包物品集合 / inventory items to save
		 */
		public BrokerOpSaveTask(List<BrokerItem> brokerItems, List<InventorySave> inventorySaves) {
			this.brokerItems = List.copyOf(brokerItems);
			this.inventorySaves = List.copyOf(inventorySaves);
		}

		/**
		 * 构造针对单个经纪行物品的落库任务。
		 * Construct a save task for a single broker item.
		 *
		 * @param brokerItem 经纪行物品 / broker item
		 */
		public BrokerOpSaveTask(BrokerItem brokerItem) {
			this(List.of(brokerItem), List.of());
		}

		@Override
		public void run() {
			save();
		}

		private boolean save() {
			try (Connection con = DatabaseAccess.getConnection()) {
				con.setAutoCommit(false);
				try {
					for (InventorySave inventorySave : inventorySaves) {
						inventoryDAO.storeInTransaction(con, inventorySave.items(), inventorySave.playerId(), null, null);
					}
					for (BrokerItem brokerItem : brokerItems) {
						brokerDAO.storeInTransaction(con, brokerItem);
					}
					con.commit();
				} catch (SQLException e) {
					con.rollback();
					throw e;
				}
			} catch (SQLException e) {
				log.error(I18n.get("log.30a6c4139e12",
					inventorySaves.stream().map(InventorySave::playerId).distinct().toList()), e);
				return false;
			}

			for (InventorySave inventorySave : inventorySaves) {
				inventoryDAO.markStored(inventorySave.items());
			}
			for (BrokerItem brokerItem : brokerItems) {
				brokerItem.setPersistentState(PersistentState.UPDATED);
			}
			return true;
		}
	}
}
