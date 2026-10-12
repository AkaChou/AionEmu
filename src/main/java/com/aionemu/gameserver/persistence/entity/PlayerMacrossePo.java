package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_macrosses 的持久化实体。
 * Persistence entity for table player_macrosses.
 */
@Data
@Table("player_macrosses")
public class PlayerMacrossePo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer playerId;

	private Integer order;

	private String macro;

}
