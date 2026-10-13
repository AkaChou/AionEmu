package com.aionemu.gameserver.persistence.dao;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.persistence.entity.BrokerPo;
import com.mybatisflex.core.service.IService;

/**
 * 交易行物品数据访问对象。
 * Broker item data access object.
 */
public interface BrokerDao extends IService<BrokerPo> {

	/**
	 * 加载全部交易行物品。
	 * Loads all broker items.
	 * @return 交易行物品列表 / broker item list
	 */
	List<BrokerItem> loadBroker();

	/**
	 * 存储交易行物品。
	 * Stores a broker item.
	 * @param brokerItem 交易行物品 / broker item
	 * @return 是否成功 / whether successful
	 */
	boolean store(BrokerItem brokerItem);

	/**
	 * 在调用方事务内存储交易行物品。
	 * Stores a broker item inside the caller's transaction.
	 * @param brokerItem 交易行物品 / broker item
	 */
	void storeInTransaction(BrokerItem brokerItem);

	/**
	 * 购买前校验物品是否仍可购买。
	 * Pre-buy check whether the item is still available.
	 * @param itemForCheck 待校验物品 ID / item ID to check
	 * @return 是否可购买 / whether available for purchase
	 */
	boolean preBuyCheck(int itemForCheck);

	/**
	 * 返回本 DAO 已使用的全部 ID（{@code IDFactory} 初始化用）。
	 * Returns an array of all ids used by this DAO (for {@code IDFactory} initialization).
	 * @return 已使用 ID 数组 / array of used ids
	 */
	int[] getUsedIDs();
}
