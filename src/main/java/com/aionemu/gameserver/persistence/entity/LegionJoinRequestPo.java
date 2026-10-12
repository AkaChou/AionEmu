package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legion_join_requests 的持久化实体。
 * Persistence entity for table legion_join_requests.
 */
@Data
@Table("legion_join_requests")
public class LegionJoinRequestPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id@Column("legionId")
	private Integer legionId;

	@Id@Column("playerId")
	private Integer playerId;

	@Column("playerName")
	private String playerName;

	@Column("playerClassId")
	private Integer playerClassId;

	@Column("playerRaceId")
	private Integer playerRaceId;

	@Column("playerLevel")
	private Integer playerLevel;

	@Column("playerGenderId")
	private Integer playerGenderId;

	@Column("joinRequestMsg")
	private String joinRequestMsg;

	private Timestamp date;

}
