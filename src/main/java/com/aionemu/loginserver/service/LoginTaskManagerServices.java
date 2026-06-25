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
            return TaskFromDBManager.getInstance();
        }
        return provider.getIfAvailable(TaskFromDBManager::getInstance);
    }

    @Override
    public void destroy() {
        taskFromDBManagerProvider = null;
    }
}
