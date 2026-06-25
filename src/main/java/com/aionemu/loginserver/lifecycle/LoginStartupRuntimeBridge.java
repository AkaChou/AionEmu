package com.aionemu.loginserver.lifecycle;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginStartupRuntimeBridge {

    public void initializeThreadPool() {
        ThreadPoolManager.getInstance();
    }

    public void connectNetwork() {
        NetConnector.getInstance().connect();
    }

    public void initializeTaskManager() {
        TaskFromDBManager.getInstance();
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
    }
}
