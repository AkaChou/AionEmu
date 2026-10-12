package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.aionemu.gameserver.persistence.mapper.BrokerMapper;
import com.mybatisflex.core.query.QueryWrapper;
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

		try (Connection con = DatabaseAccess.getConnection()) {
			storeInTransaction(con, item);
			item.setPersistentState(PersistentState.UPDATED);
			return true;
		} catch (SQLException e) {
			log.error(I18n.get("log.96bb344ecaba", item.getItemUniqueId()), e);
			return false;
		}
	}

	/**
	 * 在调用方事务连接上按持久化状态存储交易所物品。
	 * Stores a broker item on the caller's transactional connection by persistent state.
	 * @param con 调用方事务连接 / caller's transactional connection
	 * @param item 交易所物品 / broker item
	 * @throws SQLException 底层 JDBC 异常 / underlying JDBC failure
	 */
	@Override
	public void storeInTransaction(Connection con, BrokerItem item) throws SQLException {
		boolean result = switch (item.getPersistentState()) {
			case NEW -> insertBrokerItem(con, item);
			case DELETED -> deleteBrokerItem(con, item);
			case UPDATE_ITEM_BROKER -> updateItem(con, item);
			case UPDATE_REQUIRED -> updateBrokerItem(con, item);
			default -> true;
		};
		if (!result) {
			throw new SQLException("No broker row changed for item " + item.getItemUniqueId());
		}
	}

	/**
	 * 插入交易所条目。
	 * Inserts a broker entry.
	 * @param con 数据库连接 / database connection
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean insertBrokerItem(Connection con, final BrokerItem item) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(BrokerMapper.INSERT_BROKER_QUERY_JDBC)) {
			stmt.setInt(1, item.getItemUniqueId());
			stmt.setInt(2, item.getItemId());
			stmt.setLong(3, item.getItemCount());
			stmt.setString(4, item.getItemCreator());
			stmt.setString(5, item.getSeller());
			stmt.setLong(6, item.getPrice());
			stmt.setString(7, String.valueOf(item.getItemBrokerRace()));
			stmt.setTimestamp(8, item.getExpireTime());
			stmt.setTimestamp(9, item.getSettleTime());
			stmt.setInt(10, item.getSellerId());
			stmt.setBoolean(11, item.isSold());
			stmt.setBoolean(12, item.isSettled());
			stmt.setBoolean(13, item.isSplitSell());
			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * 删除交易所条目。
	 * Deletes a broker entry.
	 * @param con 数据库连接 / database connection
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean deleteBrokerItem(Connection con, final BrokerItem item) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(BrokerMapper.DELETE_BROKER_QUERY_JDBC)) {
			stmt.setInt(1, item.getItemUniqueId());
			stmt.setInt(2, item.getSellerId());
			stmt.setTimestamp(3, item.getExpireTime());
			return stmt.executeUpdate() > 0;
		}
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
	 * 更新交易所条目的出售/结算状态。
	 * Updates the sold/settled state of a broker entry.
	 * @param con 数据库连接 / database connection
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean updateBrokerItem(Connection con, final BrokerItem item) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(BrokerMapper.UPDATE_BROKER_QUERY_JDBC)) {
			stmt.setBoolean(1, item.isSold());
			stmt.setTimestamp(2, item.getSettleTime());
			stmt.setInt(3, item.getItemUniqueId());
			stmt.setTimestamp(4, item.getExpireTime());
			stmt.setInt(5, item.getSellerId());
			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * 更新交易所物品数量、价格与拆分出售等字段。
	 * Updates broker item count, price, split-sell and related fields.
	 * @param con 数据库连接 / database connection
	 * @param item 交易所物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	private boolean updateItem(Connection con, final BrokerItem item) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(BrokerMapper.UPDATE_ITEM_QUERY_JDBC)) {
			stmt.setLong(1, item.getItemCount());
			stmt.setLong(2, item.getPrice());
			stmt.setBoolean(3, item.isSold());
			stmt.setBoolean(4, item.isSettled());
			stmt.setTimestamp(5, item.getSettleTime());
			stmt.setBoolean(6, item.isSplitSell());
			stmt.setInt(7, item.getItemUniqueId());
			stmt.setTimestamp(8, item.getExpireTime());
			stmt.setInt(9, item.getSellerId());
			return stmt.executeUpdate() > 0;
		}
	}

	/**
	 * 获取已使用的玩家 ID 列表（用于 ID 分配）。
	 * Returns used player ids (for id allocation).
	 * @return 已用 ID 数组 / array of used ids
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<Integer> ids = mapper.selectPlayerIds();
			int[] result = new int[ids.size()];
			for (int i = 0; i < ids.size(); i++) {
				result[i] = ids.get(i);
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
