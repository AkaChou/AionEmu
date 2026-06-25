package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.lifecycle.LoginProcessRuntimeBridge;
import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginServerRuntimeBridge {

    private ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider;
    private LoginProcessRuntimeBridge processBridge;
    private final Consumer<String[]> startAction;
    private final BiConsumer<String[], LoginStartupSequenceLifecycle> managedStartAction;

    public LoginServerRuntimeBridge() {
        this(LoginServer::start, LoginServer::start);
    }

    LoginServerRuntimeBridge(
        Consumer<String[]> startAction,
        BiConsumer<String[], LoginStartupSequenceLifecycle> managedStartAction
    ) {
        this.startAction = startAction;
        this.managedStartAction = managedStartAction;
    }

    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    public void start(String[] args) {
        prepareShutdown();
        doStart(args);
    }

    public void start(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        prepareShutdown();
        if (startupSequenceLifecycle == null) {
            doStart(args);
            return;
        }
        doStart(args, startupSequenceLifecycle);
    }

    public void prepareShutdown() {
        processBridge().prepareShutdown();
    }

    protected void doStart(String[] args) {
        startAction.accept(args);
    }

    protected void doStart(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        managedStartAction.accept(args, startupSequenceLifecycle);
    }

    public void shutdown(boolean restart) {
        processBridge().shutdown(restart);
    }

    private synchronized LoginProcessRuntimeBridge processBridge() {
        if (processBridge == null) {
            if (processBridgeProvider == null) {
                processBridge = new LoginProcessRuntimeBridge();
            } else {
                processBridge = processBridgeProvider.getIfAvailable(LoginProcessRuntimeBridge::new);
            }
        }
        return processBridge;
    }
}
