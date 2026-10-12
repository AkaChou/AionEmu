package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_punishments 的持久化实体。
 * Persistence entity for table player_punishments.
 * <p>
 * 类型校正：{@code start_time}/{@code duration} 的 DDL 为 int unsigned，遗留实现按 rs.getLong 读取 → Long；
 * {@code punishment_type} 的 DDL 为 enum → String。
 * Type notes: DDL {@code start_time}/{@code duration} are int unsigned but the legacy code read them via
 * rs.getLong → Long; DDL {@code punishment_type} is enum → String.
 */
@Data
@Table("player_punishments")
public class PlayerPunishmentPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private String punishmentType;

	private Long startTime;

	private Long duration;

	private String reason;

}
