package com.aionemu.boot.lifecycle;

import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服生命周期网关：在可选启动序列与运行时桥之间协调启动/停止。
 * Login-server lifecycle gateway: coordinates start/stop between optional startup sequence and runtime bridge.
 */
@Component
public class LoginServerLifecycleGateway {

    private ObjectProvider<LoginStartupSequenceLifecycle> startupSequenceLifecycleProvider;
    private ObjectProvider<LoginServerRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入可选的登录启动序列生命周期提供者。
     * Injects an optional login startup-sequence lifecycle provider.
     *
     * startup-sequence ObjectProvider
     */
    @Autowired(required = false)
    void setStartupSequenceLifecycleProvider(ObjectProvider<LoginStartupSequenceLifecycle> startupSequenceLifecycleProvider) {
        this.startupSequenceLifecycleProvider = startupSequenceLifecycleProvider;
    }

    /**
     * 注入可选的登录运行时桥提供者。
     * Injects an optional login runtime-bridge provider.
     *
     * runtime-bridge ObjectProvider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<LoginServerRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动登录服：存在托管启动序列时走托管启动，否则直接桥接启动。
     * Starts login: uses managed startup sequence when available, otherwise plain bridge start.
     *
     * @param args 启动参数 / startup arguments
     */
    public void start(String[] args) {
        LoginStartupSequenceLifecycle startupSequenceLifecycle = startupSequenceLifecycle();
        if (startupSequenceLifecycle == null) {
            runtimeBridge().start(args);
            return;
        }
        runtimeBridge().start(args, startupSequenceLifecycle);
    }

    /**
     * 关闭登录服并在可用时重置启动序列状态。
     * Shuts down login and resets startup-sequence state when available.
     */
    public void stop() {
        runtimeBridge().shutdown(false);
        LoginStartupSequenceLifecycle startupSequenceLifecycle = startupSequenceLifecycle();
        if (startupSequenceLifecycle != null) {
            startupSequenceLifecycle.reset();
        }
    }

    /**
     * 解析可选的登录启动序列生命周期。
     * Resolves the optional login startup-sequence lifecycle.
     *
     * @return 启动序列，不可用则为 null / startup sequence, or null if unavailable
     */
    private LoginStartupSequenceLifecycle startupSequenceLifecycle() {
        if (startupSequenceLifecycleProvider == null) {
            return null;
        }
        return startupSequenceLifecycleProvider.getIfAvailable();
    }

    /**
     * 解析登录运行时桥；无 Provider 时新建默认实例。
     * Resolves the login runtime bridge; creates a default when no provider is present.
     *
     * runtime bridge
     */
    private LoginServerRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new LoginServerRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(LoginServerRuntimeBridge::new);
    }
}
