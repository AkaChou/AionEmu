package com.aionemu.webshop.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 表 shop_order_item 的持久化实体（订单行）。
 * Persistence entity for table shop_order_item (order line).
 */
@Data
@Table("shop_order_item")
public class ShopOrderItemPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer orderId;

	private Integer productId;

	private Integer itemId;

	private Integer quantity;

	private BigDecimal unitPrice;
}
