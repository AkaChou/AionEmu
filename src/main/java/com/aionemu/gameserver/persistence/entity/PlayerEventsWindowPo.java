package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
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

	/** 领取计数归属的自然日；跨日后计数视为 0 / Calendar day the counter belongs to; reads on a later day see 0 */
	private LocalDate countDate;

}
