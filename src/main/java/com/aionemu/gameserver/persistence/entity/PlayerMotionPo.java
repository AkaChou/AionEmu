package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_motions 的持久化实体。
 * Persistence entity for table player_motions.
 * <p>
 * 类型校正：{@code active} 的 DDL 为 tinyint(1)，遗留实现按 rs.getBoolean 读取 → Boolean。
 * Type note: DDL {@code active} is tinyint(1), but the legacy code read it via rs.getBoolean → Boolean.
 */
@Data
@Table("player_motions")
public class PlayerMotionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer motionId;

	private Integer time;

	private Boolean active;

}
