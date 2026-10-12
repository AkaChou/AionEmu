package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_minions 的持久化实体。
 * Persistence entity for table player_minions.
 */
@Data
@Table("player_minions")
public class PlayerMinionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer objectId;

	private Integer minionId;

	private String name;

	private String grade;

	/**
	 * 等级：DDL 为 varchar(11)，遗留实现按 rs.getInt 读取，故此处用 Integer。
	 * Level: DDL is varchar(11); the legacy implementation read it via rs.getInt, hence Integer.
	 */
	private Integer level;

	private Timestamp birthday;

	private Integer growthpoints;

	@Column("is_locked")
	private Integer locked;

	private String buffBag;

}
