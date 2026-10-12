package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 legion_announcement_list 的持久化实体。
 * Persistence entity for table legion_announcement_list.
 */
@Data
@Table("legion_announcement_list")
public class LegionAnnouncementListPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Integer legionId;

	private String announcement;

	private Timestamp date;

}
