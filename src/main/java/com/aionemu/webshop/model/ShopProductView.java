package com.aionemu.webshop.model;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 商品列表项视图：shop_product 与其目录条目（shop_item_catalog）的连接结果。
 * Product list item view: the join of shop_product with its catalog row (shop_item_catalog).
 * <p>字段名与 SQL 别名一一对应；用可变 POJO 以避免依赖 MyBatis 的构造器自动映射。
 * Field names match the SQL aliases one-to-one; a mutable POJO avoids relying on MyBatis constructor
 * auto-mapping.</p>
 */
@Data
public class ShopProductView {

	private int productId;

	private int itemId;

	private String name;

	private String nameZh;

	private Integer itemLevel;

	private String categoryPath;

	private String iconFile;

	private BigDecimal price;

	private Integer purchaseLimit;

	private Boolean active;

	private String description;

	private String genderLimit;

	private String raceLimit;

	private String classLimit;
}
