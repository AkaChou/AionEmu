package com.aionemu.loginserver.service;

import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginTaskManagerServices implements DisposableBean {

    private static volatile ObjectProvider<TaskFromDBManager> taskFromDBManagerProvider;

    public LoginTaskManagerServices(ObjectProvider<TaskFromDBManager> taskFromDBManagerProvider) {
        LoginTaskManagerServices.taskFromDBManagerProvider = taskFromDBManagerProvider;
    }

    public static TaskFromDBManager taskFromDBManager() {
        ObjectProvider<TaskFromDBManager> provider = taskFromDBManagerProvider;
        if (provider == null) {
            return fallbackTaskFromDBManager();
        }
        return provider.getIfAvailable(LoginTaskManagerServices::fallbackTaskFromDBManager);
    }

    @Override
    public void destroy() {
        taskFromDBManagerProvider = null;
    }

    private static TaskFromDBManager fallbackTaskFromDBManager() {
        return Fallbacks.TASK_FROM_DB_MANAGER;
    }

    private static final class Fallbacks {

        private static final TaskFromDBManager TASK_FROM_DB_MANAGER = new TaskFromDBManager();
    }
}
