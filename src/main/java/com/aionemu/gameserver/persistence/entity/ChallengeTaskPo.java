package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 challenge_tasks 的持久化实体。
 * Persistence entity for table challenge_tasks.
 */
@Data
@Table("challenge_tasks")
public class ChallengeTaskPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer taskId;

	@Id
	private Integer questId;

	@Id
	private Integer ownerId;

	@Id
	private String ownerType;

	/**
	 * 完成次数；DDL 为 int unsigned，原实现以 {@code rs.getInt} 读取。
	 * Complete count; DDL is int unsigned, while the legacy implementation read it via {@code rs.getInt}.
	 */
	private Integer completeCount;

	private Timestamp completeTime;

}
