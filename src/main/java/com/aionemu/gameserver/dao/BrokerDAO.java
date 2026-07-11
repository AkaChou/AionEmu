package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.BrokerItem;

/**
 * 交易行物品数据访问对象。
 * Broker item data access object.
 */
public abstract class BrokerDAO implements IDFactoryAwareDAO {

	/**
	 * 加载全部交易行物品。
	 * Loads all broker items.
	 *
	 * @return 交易行物品列表 / broker item list
	 */
	public abstract List<BrokerItem> loadBroker();

	/**
	 * 存储交易行物品。
	 * Stores a broker item.
	 *
	 * @param brokerItem 交易行物品 / broker item
	 * whether successful
	 */
	public abstract boolean store(BrokerItem brokerItem);

	/**
	 * 购买前校验物品是否仍可购买。
	 * Pre-buy check whether the item is still available.
	 *
	 * @param itemForCheck 待校验物品 ID / item ID to check
	 * @return 是否可购买 / whether available for purchase
	 */
	public abstract boolean preBuyCheck(int itemForCheck);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return BrokerDAO.class.getName();
	}
}
