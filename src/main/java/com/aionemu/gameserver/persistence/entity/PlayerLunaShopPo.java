package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_luna_shop 的持久化实体。
 * Persistence entity for table player_luna_shop.
 * <p>
 * 类型校正：free_under/free_munition/free_chest 的 DDL 原类型为 tinyint(1)，
 * 按遗留实现的 rs.getBoolean 读取方式校正为 Boolean。
 * Type correction: the DDL type of free_under/free_munition/free_chest is tinyint(1),
 * corrected to Boolean to match the legacy rs.getBoolean read pattern.
 */
@Data
@Table("player_luna_shop")
public class PlayerLunaShopPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	private Boolean freeUnder;

	private Boolean freeMunition;

	private Boolean freeChest;

}
