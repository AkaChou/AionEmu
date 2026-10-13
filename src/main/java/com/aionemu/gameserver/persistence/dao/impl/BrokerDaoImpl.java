package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.broker.BrokerRace;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.persistence.dao.BrokerDao;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.entity.BrokerPo;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.aionemu.gameserver.persistence.entity.PlayerPo;
import com.aionemu.gameserver.persistence.mapper.BrokerMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 交易所（Broker）数据访问实现。
 * Data access implementation for the broker exchange.
 */
@Slf4j
@Service
@Lazy
@RequiredArgsConstructor
public class BrokerDaoImpl extends ServiceImpl<BrokerMapper, BrokerPo> implements BrokerDao {

	/**
	 * 镶嵌石数据访问（跨闭包依赖，注入对方 Dao 而非 Mapper）。
	 * Item-stone data access (cross-closure dependency; the peer Dao is injected, not its Mapper).
	 */
	private final ItemStoneDao itemStoneDao;

	/**
	 * 加载全部交易所物品（含未售出物品详情与镶嵌石）。
	 * Loads all broker items (including unsold item details and stones).
	 * @return 交易所物品列表 / list of broker items
	 */
	@Override
	public List<BrokerItem> loadBroker() {
		final List<Item> items = getBrokerItems();

		if (!items.isEmpty()) {
			itemStoneDao.load(items);
		}

		List<BrokerItem> brokerItems = new ArrayList<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create()).forEach(po -> brokerItems.add(toBrokerItem(po, items)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.39996ee23f7f"), e);
		}

		return brokerItems;
	}

	/**
	 * 从库存表加载交易所位置（location=126）的物品。
	 * Loads items from inventory at broker location (location=126).
	 * @return 物品列表 / list of items
	 */
	private List<Item> getBrokerItems() {
		List<Item> brokerItems = new ArrayList<>();

		try {
			mapper.selectBrokerItems().forEach(po -> brokerItems.add(toItem(po)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.21c8f55ddfc2"), e);
		}

		return brokerItems;
	}

	/**
	 * 由 broker 行构造交易行物品，未售出时关联托管物品。
	 * Builds a broker item from a broker row, linking the held item when unsold.
	 * @param po broker 行 / broker row
	 * @param items 托管物品列表 / held item list
	 * @return 交易行物品 / broker item
	 */
	private static BrokerItem toBrokerItem(BrokerPo po, List<Item> items) {
		boolean isSold = Boolean.TRUE.equals(po.getSold());

		Item item = null;
		if (!isSold) {
			for (Item brItem : items) {
				if (intValue(po.getItemPointer()) == brItem.getObjectId()) {
					item = brItem;
					break;
				}
			}
		}

		return new BrokerItem(item, po.getItemId(), po.getItemPointer(), po.getItemCount(), po.getItemCreator(),
			po.getPrice(), po.getSeller(), po.getSellerId(), BrokerRace.valueOf(po.getBrokerRace()), isSold,
			Boolean.TRUE.equals(po.getSettled()), po.getExpireTime(), po.getSettleTime(),
			Boolean.TRUE.equals(po.getSplitsell()));
	}

	/**
	 * 由 inventory 行构造托管物品（列与字段顺序同原 SELECT_INVENTORY_QUERY）。
	 * Builds a held item from an inventory row (columns and order as the original query).
	 * @param po inventory 行 / inventory row
	 * @return 物品 / item
	 */
	private static Item toItem(InventoryPo po) {
		Item item = new Item(po.getItemUniqueId(), po.getItemId(), po.getItemCount(), po.getItemColor(),
			intValue(po.getColorExpires()), po.getItemCreator(), po.getExpireTime(), po.getActivationCount(), false,
			false, po.getSlot(), intValue(po.getItemLocation()), po.getEnchant(), po.getEnchantBonus(),
			po.getItemSkin(), po.getFusionedItem(), po.getOptionalSocket(), po.getOptionalFusionSocket(),
			po.getCharge(), intValue(po.getRndBonus()), po.getRndCount(), po.getWrappableCount(), false,
			po.getTemperingLevel(), false, 0, 0, false, po.getReductionLevel(), po.getSeal(),
			Boolean.TRUE.equals(po.getIsEnhance()), po.getEnhanceSkillId(), po.getEnhanceSkillEnchant());
		item.setPersistentState(PersistentState.UPDATED);
		return item;
	}

	/**
	 * 按持久化状态存储交易所物品。
	 * Stores a broker item according to its persistent state.
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean store(BrokerItem item) {
		if (item == null) {
			log.warn(I18n.get("log.89da0d5f3c1f"));
			return false;
		}

		try {
			storeInTransaction(item);
			item.setPersistentState(PersistentState.UPDATED);
			return true;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.96bb344ecaba", item.getItemUniqueId()), e);
			return false;
		}
	}

	/**
	 * 在调用方事务内按持久化状态存储交易所物品。
	 * Stores a broker item inside the caller's transaction, by persistent state.
	 * @param item 交易所物品 / broker item
	 */
	@Override
	public void storeInTransaction(BrokerItem item) {
		boolean result = switch (item.getPersistentState()) {
			case NEW -> insertBrokerItem(item);
			case DELETED -> deleteBrokerItem(item);
			case UPDATE_ITEM_BROKER -> updateItem(item);
			case UPDATE_REQUIRED -> updateBrokerItem(item);
			default -> true;
		};
		if (!result) {
			throw new DataAccessException("No broker row changed for item " + item.getItemUniqueId()) {
			};
		}
	}

	/**
	 * 插入交易所条目。
	 * Inserts a broker entry.
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean insertBrokerItem(final BrokerItem item) {
		BrokerPo po = new BrokerPo();
		po.setItemPointer(item.getItemUniqueId());
		po.setItemId(item.getItemId());
		po.setItemCount(item.getItemCount());
		po.setItemCreator(item.getItemCreator());
		po.setSeller(item.getSeller());
		po.setPrice(item.getPrice());
		po.setBrokerRace(String.valueOf(item.getItemBrokerRace()));
		po.setExpireTime(item.getExpireTime());
		po.setSettleTime(item.getSettleTime());
		po.setSellerId(item.getSellerId());
		po.setSold(item.isSold());
		po.setSettled(item.isSettled());
		po.setSplitsell(item.isSplitSell());
		return mapper.insert(po) > 0;
	}

	/**
	 * 删除交易所条目。
	 * Deletes a broker entry.
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean deleteBrokerItem(final BrokerItem item) {
		return mapper.deleteByQuery(QueryWrapper.create()
			.where(BrokerPo::getItemPointer).eq(item.getItemUniqueId())
			.and(BrokerPo::getSellerId).eq(item.getSellerId())
			.and(BrokerPo::getExpireTime).eq(item.getExpireTime())) > 0;
	}

	/**
	 * 购买前检查物品是否仍可售。
	 * Checks whether the item is still available for purchase.
	 * @param itemForCheck 物品指针 ID / item pointer id
	 * @return 是否可用 / whether available
	 */
	@Override
	public boolean preBuyCheck(int itemForCheck) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create()
				.where(BrokerPo::getItemPointer).eq(itemForCheck)
				.and(BrokerPo::getSold).eq(false)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.565c72023756", itemForCheck), e);
			return false;
		}
	}

	/**
	 * 更新交易所条目的出售/结算状态（仅未结算行）。
	 * Updates the sold/settled state of a broker entry (unsettled rows only).
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean updateBrokerItem(final BrokerItem item) {
		return UpdateChain.of(BrokerPo.class)
			.set(BrokerPo::getSold, item.isSold())
			.set(BrokerPo::getSettled, true)
			.set(BrokerPo::getSettleTime, item.getSettleTime())
			.where(BrokerPo::getItemPointer).eq(item.getItemUniqueId())
			.and(BrokerPo::getExpireTime).eq(item.getExpireTime())
			.and(BrokerPo::getSellerId).eq(item.getSellerId())
			.and(BrokerPo::getSettled).eq(false)
			.update();
	}

	/**
	 * 更新交易所物品数量、价格与拆分出售等字段（仅未结算行）。
	 * Updates broker item count, price, split-sell and related fields (unsettled rows only).
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean updateItem(final BrokerItem item) {
		return UpdateChain.of(BrokerPo.class)
			.set(BrokerPo::getItemCount, item.getItemCount())
			.set(BrokerPo::getPrice, item.getPrice())
			.set(BrokerPo::getSold, item.isSold())
			.set(BrokerPo::getSettled, item.isSettled())
			.set(BrokerPo::getSettleTime, item.getSettleTime())
			.set(BrokerPo::getSplitsell, item.isSplitSell())
			.where(BrokerPo::getItemPointer).eq(item.getItemUniqueId())
			.and(BrokerPo::getExpireTime).eq(item.getExpireTime())
			.and(BrokerPo::getSellerId).eq(item.getSellerId())
			.and(BrokerPo::getSettled).eq(false)
			.update();
	}

	/**
	 * 获取已使用的玩家 ID 列表（用于 ID 分配）。
	 * Returns used player ids (for id allocation).
	 * @return 已用 ID 数组 / array of used ids
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<Object> ids = mapper.selectObjectListByQuery(
				QueryWrapper.create().select(PlayerPo::getId).from(PlayerPo.class));
			int[] result = new int[ids.size()];
			for (int i = 0; i < ids.size(); i++) {
				result[i] = ((Number) ids.get(i)).intValue();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ce9d363cd21c"), e);
			return new int[0];
		}
	}

	/**
	 * 空值安全取整：与原 rs.getInt 的 NULL→0 语义一致。
	 * Null-safe int, matching the original rs.getInt NULL→0 semantics.
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
