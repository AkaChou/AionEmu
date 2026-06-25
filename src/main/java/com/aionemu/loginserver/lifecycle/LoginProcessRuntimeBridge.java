package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.service.LoginShutdownServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginProcessRuntimeBridge {

    private Shutdown shutdown;

    @Autowired(required = false)
    void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.setShutdownProvider(shutdownProvider);
    }

    public Thread shutdownHook() {
        return shutdown();
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void shutdown(boolean restart) {
        shutdown().shutdown(restart);
    }

    public void prepareShutdown() {
        shutdown();
    }

    public void exitWithError() {
        exit(ExitCode.CODE_ERROR);
    }

    public void exit(int status) {
        AionProcessExit.exit(status);
    }

    private synchronized Shutdown shutdown() {
        if (shutdown == null) {
            shutdown = LoginShutdownServices.shutdown();
        }
        return shutdown;
    }
}
