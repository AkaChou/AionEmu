package com.aionemu.webshop.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 表 shop_order 的持久化实体（订单头）。
 * Persistence entity for table shop_order (order header).
 */
@Data
@Table("shop_order")
public class ShopOrderPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer accountId;

	private String accountName;

	private Integer playerId;

	private String playerName;

	private BigDecimal totalPrice;

	private Timestamp createdAt;
}
