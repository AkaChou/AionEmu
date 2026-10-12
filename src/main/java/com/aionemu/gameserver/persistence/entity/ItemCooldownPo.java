package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 item_cooldowns 的持久化实体。
 * Persistence entity for table item_cooldowns.
 * <p>
 * 字段类型按遗留实现的读取方式校正：{@code use_delay} 原本经 {@code ResultSet#getInt} 与
 * {@code PreparedStatement#setInt} 读写（值为 {@code int} 的 {@code ItemCooldown#useDelay()}），
 * 故由 {@link Long} 改为 {@link Integer}；{@code reuse_time} 保持 {@link Long}。
 * Field types follow the legacy read style: {@code use_delay} was read and written via {@code getInt} /
 * {@code setInt} (the value is {@code int} from {@code ItemCooldown#useDelay()}), so it changes from
 * {@link Long} to {@link Integer}; {@code reuse_time} stays {@link Long}.
 * </p>
 */
@Data
@Table("item_cooldowns")
public class ItemCooldownPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer delayId;

	/**
	 * DDL 类型 int unsigned；遗留实现经 {@code getInt}/{@code setInt} 读写。
	 * DDL type int unsigned; read and written via {@code getInt}/{@code setInt}.
	 */
	private Integer useDelay;

	private Long reuseTime;

}
