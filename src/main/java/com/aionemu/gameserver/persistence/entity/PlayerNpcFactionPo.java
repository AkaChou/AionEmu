package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_npc_factions 的持久化实体。
 * Persistence entity for table player_npc_factions.
 * <p>
 * 类型校正：active 的 DDL 原类型为 tinyint(1)，按遗留实现的 rs.getBoolean 读取方式
 * 校正为 Boolean；state 的 DDL 原类型为 enum('NOTING','START','COMPLETE')，按
 * rs.getString 读取方式保持 String。
 * Type correction: active keeps DDL tinyint(1) but is read via rs.getBoolean, hence Boolean;
 * state keeps DDL enum(...) but is read via rs.getString, hence String.
 */
@Data
@Table("player_npc_factions")
public class PlayerNpcFactionPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer playerId;

	@Id
	private Integer factionId;

	private Boolean active;

	private Integer time;

	private String state;

	private Integer questId;

}
