package com.aionemu.loginserver.taskmanager.handler;

import com.aionemu.loginserver.taskmanager.handler.implementations.CleanAccountsHandler;
import com.aionemu.loginserver.taskmanager.handler.implementations.RestartHandler;
import com.aionemu.loginserver.taskmanager.handler.implementations.ShutdownHandler;

/**
 * 数据库任务处理器类型枚举，映射名称到具体实现类。
 * Enum of DB task handler types mapping names to concrete handler classes.
 *
 * @author nrg
 */
public enum TaskFromDBHandlerHolder {

    SHUTDOWN(ShutdownHandler.class),
    RESTART(RestartHandler.class),
    CLEAN_ACCOUNTS(CleanAccountsHandler.class);
    private Class<? extends TaskFromDBHandler> taskClass;

    private TaskFromDBHandlerHolder(Class<? extends TaskFromDBHandler> taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 获取对应的处理器实现类。
     * Returns the associated handler implementation class.
     *
     * handler class
     */
    public Class<? extends TaskFromDBHandler> getTaskClass() {
        return taskClass;
    }
}
