package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 ingameshop 的持久化实体。
 * Persistence entity for table ingameshop.
 * <p>
 * 类型校正：category/sub_category/item_type/gift 的 DDL 原类型为 tinyint(1)，
 * 按遗留实现的 rs.getByte 读取方式校正为 Byte。
 * Type correction: the DDL type of category/sub_category/item_type/gift is tinyint(1),
 * corrected to Byte to match the legacy rs.getByte read pattern.
 */
@Data
@Table("ingameshop")
public class IngameshopPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer objectId;

	private Integer itemId;

	private Long itemCount;

	private Long itemPrice;

	private Byte category;

	private Byte subCategory;

	private Integer list;

	private Integer salesRanking;

	private Byte itemType;

	private Byte gift;

	private String titleDescription;

	private String description;

}
