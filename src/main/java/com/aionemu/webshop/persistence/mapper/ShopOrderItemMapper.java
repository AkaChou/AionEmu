package com.aionemu.webshop.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.webshop.persistence.entity.ShopOrderItemPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 shop_order_item 的映射层。
 * Mapper for table shop_order_item.
 */
public interface ShopOrderItemMapper extends BaseMapper<ShopOrderItemPo> {

	/**
	 * 统计某角色已购买某物品的累计数量，用于限购校验。
	 * Counts how many units of an item a character has already bought, for the purchase-limit check.
	 *
	 * @param playerId 角色 ID / character id
	 * @param itemId   物品 ID / item id
	 * @return 累计数量（无记录时为 0）/ cumulative quantity, 0 when there is no record
	 */
	@Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM shop_order_item oi "
		+ "JOIN shop_order o ON o.id = oi.order_id "
		+ "WHERE o.player_id = #{playerId} AND oi.item_id = #{itemId}")
	long sumPurchasedQuantity(@Param("playerId") int playerId, @Param("itemId") int itemId);
}

