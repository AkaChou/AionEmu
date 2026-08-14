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

/**
 * 登录服运行时桥：对接 LoginServer 启动动作与进程级关闭/准备钩子。
 * Login-server runtime bridge: wires LoginServer start actions to process-level prepare/shutdown hooks.
 */
@Component
@Lazy
public class LoginServerRuntimeBridge {

    private ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider;
    private LoginProcessRuntimeBridge processBridge;
    private final Consumer<String[]> startAction;
    private final BiConsumer<String[], LoginStartupSequenceLifecycle> managedStartAction;

    /**
     * 默认构造：委托 LoginServer 静态启动入口。
     * Default constructor: delegates to LoginServer static start entry points.
     */
    public LoginServerRuntimeBridge() {
        this(LoginServer::start, LoginServer::start);
    }

    /**
     * 可替换启动动作的构造（便于测试）。
     * Constructor with replaceable start actions (for tests).
     *
     * @param startAction 无托管序列的启动动作 / start action without managed sequence
     * @param managedStartAction 带启动序列的启动动作 / start action with startup sequence
     */
    LoginServerRuntimeBridge(
        Consumer<String[]> startAction,
        BiConsumer<String[], LoginStartupSequenceLifecycle> managedStartAction
    ) {
        this.startAction = startAction;
        this.managedStartAction = managedStartAction;
    }

    /**
     * 注入可选的登录进程运行时桥提供者。
     * Injects an optional login process runtime-bridge provider.
     *
     * @param processBridgeProvider 登录进程运行时桥提供者 / process-bridge ObjectProvider
     */
    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<LoginProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    /**
     * 准备关闭后执行默认启动。
     * Prepares shutdown, then performs the default start.
     *
     * @param args 启动参数 / startup arguments
     */
    public void start(String[] args) {
        prepareShutdown();
        doStart(args);
    }

    /**
     * 准备关闭后按是否提供启动序列选择启动路径。
     * Prepares shutdown, then chooses start path based on the provided startup sequence.
     *
     * @param args 启动参数 / startup arguments
     * @param startupSequenceLifecycle 可选启动序列生命周期 / optional startup-sequence lifecycle
     */
    public void start(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        prepareShutdown();
        if (startupSequenceLifecycle == null) {
            doStart(args);
            return;
        }
        doStart(args, startupSequenceLifecycle);
    }

    /**
     * 准备进程关闭钩子。
     * Prepares the process shutdown hook.
     */
    public void prepareShutdown() {
        processBridge().prepareShutdown();
    }

    /**
     * 执行无托管序列的启动动作。
     * Executes the unmanaged start action.
     *
     * @param args 启动参数 / startup arguments
     */
    protected void doStart(String[] args) {
        startAction.accept(args);
    }

    /**
     * 执行带启动序列的托管启动动作。
     * Executes the managed start action with a startup sequence.
     *
     * @param args 启动参数 / startup arguments
     * @param startupSequenceLifecycle 启动序列生命周期 / startup-sequence lifecycle
     */
    protected void doStart(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
        managedStartAction.accept(args, startupSequenceLifecycle);
    }

    /**
     * 关闭登录进程。
     * Shuts down the login process.
     *
     * @param restart 是否以重启意图关闭 / whether shutdown is for restart
     */
    public void shutdown(boolean restart) {
        processBridge().shutdown(restart);
    }

    /**
     * 懒加载并缓存登录进程运行时桥。
     * Lazily loads and caches the login process runtime bridge.
     *
     * @return 进程运行时桥 / process runtime bridge
     */
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
