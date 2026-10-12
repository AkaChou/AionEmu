package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 limited_quest_counters 的持久化实体。
 * Persistence entity for table limited_quest_counters.
 */
@Data
@Table("limited_quest_counters")
public class LimitedQuestCounterPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Long questId;

	private Long remaining;

}
