package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.service.LoginShutdownServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 登录服进程级运行时桥接，封装关机钩子注册与进程退出。
 * Login-server process-level runtime bridge wrapping shutdown-hook registration and process exit.
 */
@Component
@Lazy
public class LoginProcessRuntimeBridge {

    private Shutdown shutdown;

    /**
     * 注入可选的 {@link Shutdown} 提供者，转交给关机服务静态注册。
     * Inject optional {@link Shutdown} provider and forward it to the shutdown-service static registry.
     *
     * @param shutdownProvider 关机 Bean 提供者 / shutdown bean provider
     */
    @Autowired(required = false)
    void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.setShutdownProvider(shutdownProvider);
    }

    /**
     * 返回可用作 JVM 关机钩子的 {@link Shutdown} 线程。
     * Return the {@link Shutdown} thread usable as a JVM shutdown hook.
     *
     * @return 关机钩子线程 / shutdown-hook thread
     */
    public Thread shutdownHook() {
        return shutdown();
    }

    /**
     * 将给定线程注册为 JVM 关机钩子。
     * Register the given thread as a JVM shutdown hook.
     *
     * @param shutdownHook 关机钩子线程 / shutdown-hook thread
     */
    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 触发登录服优雅关机（可选重启）。
     * Trigger a graceful login-server shutdown (optionally with restart).
     *
     * whether to restart
     */
    public void shutdown(boolean restart) {
        shutdown().shutdown(restart);
    }

    /**
     * 预热并解析 {@link Shutdown} 实例，确保后续关机路径可用。
     * Eagerly resolve the {@link Shutdown} instance so later shutdown paths are ready.
     */
    public void prepareShutdown() {
        shutdown();
    }

    /**
     * 以错误码退出进程。
     * Exit the process with an error exit code.
     */
    public void exitWithError() {
        exit(ExitCode.CODE_ERROR);
    }

    /**
     * 以指定状态码退出进程。
     * Exit the process with the given status code.
     *
     * exit status
     */
    public void exit(int status) {
        AionProcessExit.exit(status);
    }

    /**
     * 懒加载并缓存 {@link Shutdown} 实例。
     * Lazily resolve and cache the {@link Shutdown} instance.
     *
     * @return 关机处理器 / shutdown handler
     */
    private synchronized Shutdown shutdown() {
        if (shutdown == null) {
            shutdown = LoginShutdownServices.shutdown();
        }
        return shutdown;
    }
}
