package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 tasks 的持久化实体。
 * Persistence entity for table tasks.
 * <p>
 * 列集合与类型以原 {@code TaskFromDBDAO} 的读取方式为准，即 {@code task}/{@code type}/
 * {@code last_activation}/{@code start_time}/{@code delay}/{@code param}。
 * Columns and types follow the legacy {@code TaskFromDBDAO} reads: {@code task}/{@code type}/
 * {@code last_activation}/{@code start_time}/{@code delay}/{@code param}.
 * </p>
 * <p>
 * 注意：{@code V1__init.sql} 将该表建为 {@code task_type}/{@code trigger_type}/{@code trigger_param}/
 * {@code exec_param}，与该 DAO 及 {@code TaskFromDB} 模型互不兼容（见交付报告）。
 * Note: {@code V1__init.sql} declares this table as {@code task_type}/{@code trigger_type}/
 * {@code trigger_param}/{@code exec_param}, which is incompatible with both that DAO and the
 * {@code TaskFromDB} model (see the delivery report).
 * </p>
 */
@Data
@Table("tasks")
public class TaskPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/** 任务 ID；原 {@code rs.getInt("id")}。 / Task id; legacy {@code rs.getInt("id")}. */
	@Id
	private Integer id;

	/** 任务名；原 {@code rs.getString("task")}。 / Task name; legacy {@code rs.getString("task")}. */
	private String task;

	/** 触发类型；原 {@code rs.getString("type")}。 / Trigger type; legacy {@code rs.getString("type")}. */
	private String type;

	/** 最近激活时间；原 {@code rs.getTimestamp("last_activation")}。 */
	private Timestamp lastActivation;

	/** 起始时间 HH:MM:SS；原 {@code rs.getString("start_time")}。 */
	private String startTime;

	/** 延迟秒数；原 {@code rs.getInt("delay")}。 / Delay seconds; legacy {@code rs.getInt("delay")}. */
	private Integer delay;

	/** 参数（空格分隔）；原 {@code rs.getString("param")}。 */
	private String param;

}
