package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginServerLifecycleGateway {

    private LoginStartupSequenceLifecycle startupSequenceLifecycle;

    @Autowired(required = false)
    void setStartupSequenceLifecycle(LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        this.startupSequenceLifecycle = startupSequenceLifecycle;
    }

    public void start(String[] args) {
        if (startupSequenceLifecycle == null) {
            com.aionemu.loginserver.LoginServer.start(args);
            return;
        }
        com.aionemu.loginserver.LoginServer.start(args, startupSequenceLifecycle);
    }

    public void stop() {
        com.aionemu.loginserver.Shutdown.getInstance().shutdown(false);
        if (startupSequenceLifecycle != null) {
            startupSequenceLifecycle.reset();
        }
    }
}
