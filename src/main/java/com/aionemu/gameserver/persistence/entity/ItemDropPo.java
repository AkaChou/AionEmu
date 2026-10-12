package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.io.Serial;

import lombok.Data;

/**
 * 表 item_drop 的持久化实体。
 * Persistence entity for table item_drop.
 */
@Data
@Table("item_drop")
public class ItemDropPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer npcId;

	private Integer itemId;

	private String dropGroup;

	private String race;

	private Integer minAmount;

	private Integer maxAmount;

	private BigDecimal chance;

	private Integer noReduce;

	private Integer eachmember;

}
