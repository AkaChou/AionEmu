package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginProcessRuntimeBridge {

    private ObjectProvider<Shutdown> shutdownProvider;

    @Autowired(required = false)
    void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        this.shutdownProvider = shutdownProvider;
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

    public void exitWithError() {
        System.exit(ExitCode.CODE_ERROR);
    }

    private Shutdown shutdown() {
        if (shutdownProvider == null) {
            return Shutdown.getInstance();
        }
        return shutdownProvider.getIfAvailable(Shutdown::getInstance);
    }
}
