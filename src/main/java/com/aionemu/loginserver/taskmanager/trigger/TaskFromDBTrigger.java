package com.aionemu.loginserver.taskmanager.trigger;

import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据库任务触发器抽象基类：绑定处理器并定义校验/初始化/运行。
 * Abstract base for DB task triggers: binds a handler and defines validate/init/run.
 *
 * @author nrg
 */
public abstract class TaskFromDBTrigger implements Runnable {

    /**
     * 获取将被触发的处理器。
     * Returns the handler to be triggered.
     *
     * @return 处理器 / handler
     */
    @Getter
    @Setter
    protected TaskFromDBHandler handlerToTrigger;
    /**
     * 获取触发器参数。
     * Returns the trigger parameters.
     *
     * @return 参数 / parameters
     */
    @Getter
    @Setter
    protected String[] params = {""};

    /**
     * 获取关联任务 ID。
     * Returns the associated task id.
     *
     * @return 任务 ID / task id
     */
    public int getTaskId() {
        return handlerToTrigger.getTaskId();
    }

    /**
     * 综合校验：处理器非空、触发器自身有效且处理器参数有效。
     * Combined validation: handler non-null, trigger itself valid and handler params valid.
     *
     * @return 是否有效 / whether valid
     */
    public final boolean isValid() {
        return handlerToTrigger != null && this.isValidTrigger() && handlerToTrigger.isValid();
    }

    /**
     * 校验触发器自身参数。
     * Validates the trigger's own parameters.
     *
     * @return 是否有效 / whether valid
     */
    public abstract boolean isValidTrigger();

    /**
     * 初始化触发器（调度或立即执行）。
     * Initializes the trigger (schedule or run immediately).
     */
    public abstract void initTrigger();

    /**
     * 运行时调用关联处理器。
     * Invokes the bound handler when run.
     */
    @Override
    public void run() {
        handlerToTrigger.trigger();
    }
}
