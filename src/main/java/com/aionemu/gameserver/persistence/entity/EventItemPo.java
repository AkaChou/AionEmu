package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 event_items 的持久化实体。
 * Persistence entity for table event_items.
 * <p>
 * 字段类型按遗留实现的读取方式校正：{@code counts} 原本经 {@code ResultSet#getInt} 读取，
 * 写入值同样来自 {@code int}（{@code MaxCountOfDay#getThisCount()}），
 * 故由 {@link Long} 改为 {@link Integer}。
 * Field types follow the legacy read style: {@code counts} was read via {@code ResultSet#getInt} and
 * written from an {@code int} ({@code MaxCountOfDay#getThisCount()}), so it changes from
 * {@link Long} to {@link Integer}.
 * </p>
 */
@Data
@Table("event_items")
public class EventItemPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer itemId;

	/**
	 * DDL 类型 int unsigned。
	 * DDL type int unsigned.
	 */
	private Integer counts;

}
