package com.aionemu.loginserver.taskmanager.handler;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据库任务处理器抽象基类：持有任务 ID 与参数，定义校验与触发接口。
 * Abstract base for DB task handlers: holds task id and params, defines validation and trigger APIs.
 *
 * @author nrg
 */
public abstract class TaskFromDBHandler {

    /**
     * 获取任务 ID。
     * Returns the task id.
     *
     * @return 任务 ID / task id
     */
    @Getter
    @Setter
    protected int taskId;
    /**
     * 获取任务执行参数。
     * Returns the task execution parameters.
     *
     * @return 参数数组 / parameters
     */
    @Getter
    @Setter
    protected String[] params = {""};

    /**
     * 检查任务参数是否有效。
     * Checks whether the task parameters are valid.
     *
     * @return 参数有效则为 true / true if valid
     */
    public abstract boolean isValid();

    /**
     * 触发处理器逻辑。
     * Triggers the handler logic.
     */
    public abstract void trigger();
}
