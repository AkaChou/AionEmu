package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 surveys 的持久化实体。
 * Persistence entity for table surveys.
 */
@Data
@Table("surveys")
public class SurveyPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer uniqueId;

	private Integer ownerId;

	private Integer itemId;

	/**
	 * 物品数量；DDL 为 decimal(20,0)，原实现以 {@code rs.getLong} 读取。
	 * Item count; DDL is decimal(20,0), while the legacy implementation read it via
	 * {@code rs.getLong} into {@code SurveyItem#count} (long).
	 */
	private Long itemCount;

	private String htmlText;

	private String htmlRadio;

	private Integer used;

	private String usedTime;

}
