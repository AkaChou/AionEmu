package com.aionemu.webshop.persistence.dao;

import java.util.List;

import com.aionemu.webshop.model.ShopOrderView;
import com.aionemu.webshop.persistence.entity.ShopOrderItemPo;
import com.aionemu.webshop.persistence.entity.ShopOrderPo;

/**
 * 商城订单数据访问层：下单（订单 + 发货队列同事务）、订单查询与发货重投。
 * Shop order data access: placing an order (order and delivery queue in one transaction), querying orders,
 * and re-queuing a delivery.
 * <p>发货本身不在这里实现：写入 {@code web_reward} 之后，已存在的 {@code WebshopService} 会轮询并把物品
 * 以系统邮件发给在线玩家，离线玩家由 {@code RewardService} 在下次登录时补齐。
 * Delivery itself is not implemented here: once a row is written to {@code web_reward}, the existing
 * {@code WebshopService} polls and mails the item to online players, and {@code RewardService} catches up
 * offline players on their next login.</p>
 */
public interface ShopOrderDao {

	/**
	 * 下单：在单个事务内写入订单头、订单行与发货队列。
	 * Places an order: writes the order header, the order line and the delivery queue row in a single
	 * transaction.
	 *
	 * @param order 订单头（id 由数据库生成并回写）/ order header (the id is generated and written back)
	 * @param line  订单行 / order line
	 * @return 写入成功返回 true / true when the write succeeded
	 */
	boolean placeOrder(ShopOrderPo order, ShopOrderItemPo line);

	/**
	 * 按账号分页查询订单（含订单行与发货状态），按时间倒序。
	 * Pages the orders of an account (with lines and delivery state), newest first.
	 *
	 * @param accountId 账号 ID / account id
	 * @param page      页码，从 0 开始 / zero-based page index
	 * @param pageSize  每页条数 / page size
	 * @return 订单视图 / order views
	 */
	List<ShopOrderView> listByAccount(int accountId, int page, int pageSize);

	/**
	 * 统计账号的订单总数。
	 * Counts the orders of an account.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 订单数量 / order count
	 */
	long countByAccount(int accountId);

	/**
	 * 后台分页查询全部订单，关键字可命中账号名或角色名。
	 * Pages all orders for the admin console; the keyword may match the account name or the character name.
	 *
	 * @param keyword  关键字，null 或空表示不过滤 / keyword, null or blank means no filter
	 * @param page     页码，从 0 开始 / zero-based page index
	 * @param pageSize 每页条数 / page size
	 * @return 订单视图 / order views
	 */
	List<ShopOrderView> listAll(String keyword, int page, int pageSize);

	/**
	 * 统计符合条件的订单总数。
	 * Counts orders matching the same filter as {@link #listAll}.
	 *
	 * @param keyword 关键字 / keyword
	 * @return 订单数量 / order count
	 */
	long countAll(String keyword);

	/**
	 * 统计某角色已购买某物品的累计数量，用于限购校验。
	 * Counts how many units of an item a character has already bought, for the purchase-limit check.
	 *
	 * @param playerId 角色 ID / character id
	 * @param itemId   物品 ID / item id
	 * @return 累计数量 / cumulative quantity
	 */
	long purchasedQuantity(int playerId, int itemId);

	/**
	 * 重投一条发货记录：把 web_reward 的已发标记复位，等待下一轮轮询重新发送。
	 * Re-queues one delivery: resets the delivered flag on a web_reward row so the next poll sends it again.
	 *
	 * @param rewardUnique web_reward 主键 / web_reward primary key
	 * @return 复位成功返回 true / true when the reset succeeded
	 */
	boolean requeueDelivery(int rewardUnique);
}
