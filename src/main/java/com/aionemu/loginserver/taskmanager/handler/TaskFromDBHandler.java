package com.aionemu.loginserver.taskmanager.handler;

/**
 * 数据库任务处理器抽象基类：持有任务 ID 与参数，定义校验与触发接口。
 * Abstract base for DB task handlers: holds task id and params, defines validation and trigger APIs.
 *
 * @author nrg
 */
public abstract class TaskFromDBHandler {

    protected int taskId;
    protected String[] params = {""};

    /**
     * 获取任务 ID。
     * Returns the task id.
     *
     * task id
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * 设置任务 ID。
     * Sets the task id.
     *
     * task id
     */
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取任务执行参数。
     * Returns the task execution parameters.
     *
     * parameters
     */
    public String[] getParams() {
        return params;
    }

    /**
     * 设置任务执行参数。
     * Sets the task execution parameter(s).
     *
     * String[]
     */
    public void setParams(String params[]) {
        this.params = params;
    }

    /**
     * 检查任务参数是否有效。
     * Checks whether the task parameters are valid.
     *
     * 若 valid, false otherwise 则为 true / true if valid, false otherwise
     */
    public abstract boolean isValid();

    /**
     * 触发处理器逻辑。
     * Triggers the handler logic.
     */
    public abstract void trigger();
}
