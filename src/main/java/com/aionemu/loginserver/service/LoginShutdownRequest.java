package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录服关机请求入口，委托 {@link LoginShutdownServices}。
 * Login shutdown request entry delegating to {@link LoginShutdownServices}.
 */
@Component
public final class LoginShutdownRequest implements DisposableBean {

    /**
     * 注入关机协调器 provider 并注册到静态桥。
     * Inject the shutdown coordinator provider into the static bridge.
     *
     * @param shutdownProvider 关机协调器提供者 / shutdown provider
     */
    @Autowired
    public LoginShutdownRequest(ObjectProvider<Shutdown> shutdownProvider) {
        setShutdownProvider(shutdownProvider);
    }

    /**
     * 设置关机协调器 provider（遗留静态 API）。
     * Set the shutdown provider (legacy static API).
     *
     * @param shutdownProvider 关机协调器提供者 / shutdown provider
     * @deprecated 迁移期兼容入口 / compatibility entry during boot migration
     */
    @Deprecated(since = "boot-migration")
    public static void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.setShutdownProvider(shutdownProvider);
    }

    /**
     * 执行关机流程但不 halt 进程。
     * Run the shutdown sequence without process halt.
     */
    public static void shutdownWithoutHalt() {
        shutdown().shutdown(false);
    }

    /**
     * 启动关机或仅重启流程。
     * Start shutdown or restart-only flow.
     *
     * @param restartOnly true 表示仅重启 / true means restart only
     */
    public static void startShutdown(boolean restartOnly) {
        Shutdown shutdown = shutdown();
        shutdown.setRestartOnly(restartOnly);
        shutdown.start();
    }

    /**
     * Spring 销毁时清空静态 provider。
     * Clear the static provider on Spring destroy.
     */
    @Override
    public void destroy() {
        LoginShutdownServices.setShutdownProvider(null);
    }

    /**
     * 解析当前 {@link Shutdown} 实例。
     * Resolve the current {@link Shutdown} instance.
     *
     * @return 关机协调器 / shutdown coordinator
     */
    private static Shutdown shutdown() {
        return LoginShutdownServices.shutdown();
    }
}
