package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LoginServerLifecycleGateway {

    private ObjectProvider<LoginStartupSequenceLifecycle> startupSequenceLifecycleProvider;
    private ObjectProvider<LoginServerRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setStartupSequenceLifecycleProvider(ObjectProvider<LoginStartupSequenceLifecycle> startupSequenceLifecycleProvider) {
        this.startupSequenceLifecycleProvider = startupSequenceLifecycleProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<LoginServerRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start(String[] args) {
        LoginStartupSequenceLifecycle startupSequenceLifecycle = startupSequenceLifecycle();
        if (startupSequenceLifecycle == null) {
            runtimeBridge().start(args);
            return;
        }
        runtimeBridge().start(args, startupSequenceLifecycle);
    }

    public void stop() {
        runtimeBridge().shutdown(false);
        LoginStartupSequenceLifecycle startupSequenceLifecycle = startupSequenceLifecycle();
        if (startupSequenceLifecycle != null) {
            startupSequenceLifecycle.reset();
        }
    }

    private LoginStartupSequenceLifecycle startupSequenceLifecycle() {
        if (startupSequenceLifecycleProvider == null) {
            return null;
        }
        return startupSequenceLifecycleProvider.getIfAvailable();
    }

    private LoginServerRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new LoginServerRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(LoginServerRuntimeBridge::new);
    }
}
