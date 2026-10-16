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
 * 表 shop_product 的持久化实体（上架商品；物品目录信息经 item_id 关联 shop_item_catalog）。
 * Persistence entity for table shop_product (a listed product; catalog details join via item_id).
 */
@Data
@Table("shop_product")
public class ShopProductPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer itemId;

	private BigDecimal price;

	/** 每人限购数量，0 表示不限 / per-player purchase limit, 0 means unlimited. */
	private Integer purchaseLimit;

	private Boolean active;

	private Integer sort;

	private String description;

	private Timestamp createdAt;

	private Timestamp updatedAt;
}
