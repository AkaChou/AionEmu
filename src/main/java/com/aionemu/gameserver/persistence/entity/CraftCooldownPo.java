package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 craft_cooldowns 的持久化实体。
 * Persistence entity for table craft_cooldowns.
 * <p>
 * 类型校正：reuse_time 的 DDL 原类型为 bigint unsigned，按遗留实现的 rs.getLong
 * 读取方式校正为 Long（生成器的 BigInteger 不匹配）。
 * Type correction: reuse_time keeps DDL bigint unsigned but is read via rs.getLong,
 * hence Long instead of the generated BigInteger.
 */
@Data
@Table("craft_cooldowns")
public class CraftCooldownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer delayId;

	private Long reuseTime;

}
