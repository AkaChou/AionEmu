package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 item_stones 的持久化实体。
 * Persistence entity for table item_stones.
 */
@Data
@Table("item_stones")
public class ItemStonePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer itemUniqueId;

	private Integer itemId;

	@Id
	private Integer slot;

	@Id
	private Integer category;

	@Column("polishNumber")
	private Integer polishNumber;

	@Column("polishCharge")
	private Integer polishCharge;

	private Integer procCount;

}
