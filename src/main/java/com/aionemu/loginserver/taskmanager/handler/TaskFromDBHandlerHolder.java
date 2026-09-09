package com.aionemu.loginserver.taskmanager.handler;

import com.aionemu.loginserver.taskmanager.handler.implementations.CleanAccountsHandler;
import com.aionemu.loginserver.taskmanager.handler.implementations.RestartHandler;
import com.aionemu.loginserver.taskmanager.handler.implementations.ShutdownHandler;
import lombok.Getter;

/**
 * 数据库任务处理器类型枚举，映射名称到具体实现类。
 * Enum of DB task handler types mapping names to concrete handler classes.
 *
 * @author nrg
 */
public enum TaskFromDBHandlerHolder {

        /** 关闭 / Shutdown. */
    SHUTDOWN(ShutdownHandler.class),
        /** 重启 / Restart. */
    RESTART(RestartHandler.class),
        /** 清理账号 / Clean accounts. */
    CLEAN_ACCOUNTS(CleanAccountsHandler.class);
    /**
     * 获取对应的处理器实现类。
     * Returns the associated handler implementation class.
     *
     * @return 处理器实现类 / handler class
     */
    @Getter
    private final Class<? extends TaskFromDBHandler> taskClass;

    TaskFromDBHandlerHolder(Class<? extends TaskFromDBHandler> taskClass) {
        this.taskClass = taskClass;
    }
}
