package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_vars 的持久化实体。
 * Persistence entity for table player_vars.
 */
@Data
@Table("player_vars")
public class PlayerVarPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private String param;

	private String value;

	private String time;

}
