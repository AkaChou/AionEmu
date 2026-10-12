package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_registered_items 的持久化实体。
 * Persistence entity for table player_registered_items.
 * <p>
 * 字段类型按遗留实现的读取方式校正：{@code floor} 原本经 {@code ResultSet#getByte} 读取，
 * 故由 {@link Integer} 改为 {@link Byte}。
 * Field types follow the legacy read style: {@code floor} was read via {@code ResultSet#getByte},
 * so it changes from {@link Integer} to {@link Byte}.
 * </p>
 */
@Data
@Table("player_registered_items")
public class PlayerRegisteredItemPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer itemUniqueId;

	@Id
	private Integer itemId;

	private Integer expireTime;

	private Integer color;

	private Integer colorExpires;

	private Integer ownerUseCount;

	private Integer visitorUseCount;

	private Float x;

	private Float y;

	private Float z;

	private Integer h;

	private String area;

	/** DDL 类型 tinyint；遗留实现经 {@code getByte} 读取。 / DDL type tinyint; read via {@code getByte}. */
	private Byte floor;

}
