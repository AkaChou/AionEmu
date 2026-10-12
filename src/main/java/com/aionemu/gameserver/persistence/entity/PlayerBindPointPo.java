package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_bind_point 的持久化实体。
 * Persistence entity for table player_bind_point.
 * <p>
 * 类型校正：{@code heading} 的 DDL 为 int，遗留实现按 rs.getByte 读取 → Byte。
 * Type note: DDL {@code heading} is int, but the legacy code read it via rs.getByte → Byte.
 */
@Data
@Table("player_bind_point")
public class PlayerBindPointPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Integer mapId;

	private Float x;

	private Float y;

	private Float z;

	private Byte heading;

}
