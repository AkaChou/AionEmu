package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.io.Serial;

import lombok.Data;

/**
 * 表 tasks 的持久化实体。
 * Persistence entity for table tasks.
 * <p>
 * 列集合以库中实际的 gameserver {@code tasks} 表为准：{@code task_type}/{@code trigger_type}/
 * {@code trigger_param}/{@code exec_param}，由 {@code TaskDaoImpl} 映射为 {@link com.aionemu.gameserver.model.tasks.TaskFromDB}。
 * Columns follow the actual gameserver {@code tasks} table: {@code task_type}/{@code trigger_type}/
 * {@code trigger_param}/{@code exec_param}, mapped to
 * {@link com.aionemu.gameserver.model.tasks.TaskFromDB} by {@code TaskDaoImpl}.
 * </p>
 */
@Data
@Table("tasks")
public class TaskPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 任务 ID。 / Task id. */
	@Id
	private Integer id;

	/** 任务类型枚举 {@code SHUTDOWN}/{@code RESTART}，对照处理器任务名。 / Task type enum {@code SHUTDOWN}/{@code RESTART}, matching the handler task name. */
	private String taskType;

	/** 触发类型枚举 {@code FIXED_IN_TIME}。 / Trigger type enum {@code FIXED_IN_TIME}. */
	private String triggerType;

	/** 触发参数（固定时刻为 HH:MM:SS）。 / Trigger parameter (HH:MM:SS for a fixed-in-time trigger). */
	private String triggerParam;

	/** 执行参数（空格分隔）。 / Execution parameters (space separated). */
	private String execParam;

}
