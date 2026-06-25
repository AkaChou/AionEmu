package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.Shutdown;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginProcessRuntimeBridge {

    public Thread shutdownHook() {
        return Shutdown.getInstance();
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void exitWithError() {
        System.exit(ExitCode.CODE_ERROR);
    }
}
