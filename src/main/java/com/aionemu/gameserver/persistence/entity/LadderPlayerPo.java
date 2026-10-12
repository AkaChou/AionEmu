package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 ladder_player 的持久化实体。
 * Persistence entity for table ladder_player.
 */
@Data
@Table("ladder_player")
public class LadderPlayerPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer rating;

	private Integer wins;

	private Integer losses;

	private Integer leaves;

	private Integer rank;

	private Integer lastRank;

	private Timestamp lastUpdate;

}
