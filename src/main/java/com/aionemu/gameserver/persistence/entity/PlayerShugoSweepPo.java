package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_shugo_sweep 的持久化实体。
 * Persistence entity for table player_shugo_sweep.
 */
@Data
@Table("player_shugo_sweep")
public class PlayerShugoSweepPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer freeDice;

	private Integer sweepStep;

	private Integer boardId;

}
