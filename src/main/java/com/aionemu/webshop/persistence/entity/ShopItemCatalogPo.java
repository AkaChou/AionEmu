package com.aionemu.webshop.persistence.entity;

import java.io.Serial;
import java.io.Serializable;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

import lombok.Data;

/**
 * 表 shop_item_catalog 的持久化实体（物品目录，由 aion-shop 静态数据全量导入）。
 * Persistence entity for table shop_item_catalog (item catalog, fully imported from the aion-shop static data).
 */
@Data
@Table("shop_item_catalog")
public class ShopItemCatalogPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 物品 ID，即 Aion item id / Aion item id. */
	@Id
	private Integer itemId;

	private String name;

	private String nameZh;

	private Integer itemLevel;

	private String category1;

	private String category2;

	private String category3;

	private String categoryPath;

	/**
	 * 客户端图标资源文件名（如 {@code Icon_Item_Sword_C01.webp}）。
	 * 该值不是 item id，且大小写不统一，必须逐字保留。
	 * Client icon resource file name (e.g. {@code Icon_Item_Sword_C01.webp}).
	 * It is not the item id and its casing is inconsistent, so it must be preserved verbatim.
	 */
	private String iconFile;

	private String genderLimit;

	private String raceLimit;

	private String classLimit;
}
