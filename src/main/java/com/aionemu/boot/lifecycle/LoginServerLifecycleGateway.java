package com.aionemu.boot.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class LoginServerLifecycleGateway {

    public void start(String[] args) {
        com.aionemu.loginserver.LoginServer.start(args);
    }

    public void stop() {
        com.aionemu.loginserver.Shutdown.getInstance().shutdown(false);
    }
}
