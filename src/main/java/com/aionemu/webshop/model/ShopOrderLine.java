package com.aionemu.webshop.model;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 订单行视图（含目录侧物品名，供订单列表直接展示）。
 * Order line view (carries the catalog item name so order lists render without a second lookup).
 */
@Data
public class ShopOrderLine {

	private int id;

	private int orderId;

	private int productId;

	private int itemId;

	private Integer quantity;

	private BigDecimal unitPrice;

	private String nameZh;

	private String name;

	private String iconFile;
}
