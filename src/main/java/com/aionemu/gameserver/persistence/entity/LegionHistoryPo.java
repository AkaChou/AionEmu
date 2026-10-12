package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legion_history 的持久化实体。
 * Persistence entity for table legion_history.
 */
@Data
@Table("legion_history")
public class LegionHistoryPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id(keyType = KeyType.Auto)
	private Integer id;

	private Integer legionId;

	private Timestamp date;

	private String historyType;

	private String name;

	private Integer tabId;

	private String description;

}
