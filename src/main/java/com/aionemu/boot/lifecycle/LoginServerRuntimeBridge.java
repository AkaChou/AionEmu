package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginServerRuntimeBridge {

    public void start(String[] args) {
        LoginServer.start(args);
    }

    public void start(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        LoginServer.start(args, startupSequenceLifecycle);
    }

    public void shutdown(boolean restart) {
        Shutdown.getInstance().shutdown(restart);
    }
}
