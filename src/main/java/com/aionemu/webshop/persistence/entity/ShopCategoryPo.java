package com.aionemu.webshop.persistence.entity;

import java.io.Serial;
import java.io.Serializable;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 表 shop_category 的持久化实体（商城分类树节点，由物品目录的 category_path 聚合而来）。
 * Persistence entity for table shop_category (shop category tree node, aggregated from the catalog's
 * category_path).
 */
@Data
@Table("shop_category")
public class ShopCategoryPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private String name;

	/** 斜杠拼接的完整路径，如 {@code 武器/长剑} / full slash-joined path, e.g. {@code 武器/长剑}. */
	private String path;

	private String parentPath;

	private Integer depth;

	private Integer itemCount;

	private Integer sort;
}
