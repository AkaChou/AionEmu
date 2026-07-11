package com.aionemu.loginserver.service;

import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服数据库任务管理器服务定位器，提供 {@link TaskFromDBManager} 的静态访问与 Spring 回退。
 * Login-server DB-task-manager service locator providing static access to {@link TaskFromDBManager}
 * with Spring resolution and a local fallback.
 */
@Component
public final class LoginTaskManagerServices implements DisposableBean {

    private static volatile ObjectProvider<TaskFromDBManager> taskFromDBManagerProvider;

    /**
     * 构造并注册 {@link TaskFromDBManager} 的 Spring 提供者。
     * Construct and register the Spring provider for {@link TaskFromDBManager}.
     *
     * @param taskFromDBManagerProvider 任务管理器提供者 / task-manager provider
     */
    public LoginTaskManagerServices(ObjectProvider<TaskFromDBManager> taskFromDBManagerProvider) {
        LoginTaskManagerServices.taskFromDBManagerProvider = taskFromDBManagerProvider;
    }

    /**
     * 获取数据库任务管理器：优先 Spring Bean，否则回退本地单例。
     * Obtain the DB task manager: prefer Spring bean, else fall back to a local singleton.
     *
     * @return 任务管理器 / task manager
     */
    public static TaskFromDBManager taskFromDBManager() {
        ObjectProvider<TaskFromDBManager> provider = taskFromDBManagerProvider;
        if (provider == null) {
            return fallbackTaskFromDBManager();
        }
        return provider.getIfAvailable(LoginTaskManagerServices::fallbackTaskFromDBManager);
    }

    /**
     * Spring 销毁时清空静态提供者引用。
     * Clear the static provider reference on Spring destroy.
     */
    @Override
    public void destroy() {
        taskFromDBManagerProvider = null;
    }

    /**
     * 返回回退用的本地 {@link TaskFromDBManager} 实例。
     * Return the local fallback {@link TaskFromDBManager} instance.
     *
     * @return 回退任务管理器 / fallback task manager
     */
    private static TaskFromDBManager fallbackTaskFromDBManager() {
        return Fallbacks.TASK_FROM_DB_MANAGER;
    }

    /**
     * 延迟初始化的回退单例持有者。
     * Lazy holder for the fallback singleton.
     */
    private static final class Fallbacks {

        private static final TaskFromDBManager TASK_FROM_DB_MANAGER = new TaskFromDBManager();
    }
}
