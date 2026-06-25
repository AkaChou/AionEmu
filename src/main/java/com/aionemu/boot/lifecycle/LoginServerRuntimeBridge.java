package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.lifecycle.LoginProcessRuntimeBridge;
import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginServerRuntimeBridge {

    private ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider;

    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    public void start(String[] args) {
        LoginServer.start(args);
    }

    public void start(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        LoginServer.start(args, startupSequenceLifecycle);
    }

    public void shutdown(boolean restart) {
        processBridge().shutdown(restart);
    }

    private LoginProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new LoginProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(LoginProcessRuntimeBridge::new);
    }
}
