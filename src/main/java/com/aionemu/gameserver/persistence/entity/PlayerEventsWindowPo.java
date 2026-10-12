package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 player_events_window 的持久化实体。
 * Persistence entity for table player_events_window.
 */
@Data
@Table("player_events_window")
public class PlayerEventsWindowPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer accountId;

	@Id
	private Integer eventId;

	private Timestamp lastStamp;

	private Integer elapsed;

	private Integer rewardRecivedCount;

}
