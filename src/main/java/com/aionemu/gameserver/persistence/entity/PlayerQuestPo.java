package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_quests 的持久化实体。
 * Persistence entity for table player_quests.
 */
@Data
@Table("player_quests")
public class PlayerQuestPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer questId;

	private String status;

	private Integer questVars;

	private Integer completeCount;

	private Timestamp completeTime;

	private Timestamp nextRepeatTime;

	private Integer reward;

}
