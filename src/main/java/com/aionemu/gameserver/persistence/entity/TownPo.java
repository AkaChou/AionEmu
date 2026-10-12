package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 towns 的持久化实体。
 * Persistence entity for table towns.
 */
@Data
@Table("towns")
public class TownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private Integer level;

	private Integer points;

	private String race;

	private Timestamp levelUpDate;

}
