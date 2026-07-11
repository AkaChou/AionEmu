package com.aionemu.chatserver;

import com.aionemu.chatserver.service.ChatCoreServices;
import com.aionemu.chatserver.service.ChatNettyServers;
import com.aionemu.chatserver.service.ChatRestartServices;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.commons.network.CommonsNetworkThreadPoolServices;
import com.aionemu.commons.utils.ExitCode;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 聊天服务器 JVM 关停钩子：关闭重启服务、Netty、GS 状态与线程池，并可选 halt。
 * Chat-server JVM shutdown hook: stop restart service, Netty, GS state, and thread pool, then optionally halt.
 *
 * @author nrg
 */
public class ShutdownHook extends Thread {

    private static final ShutdownHook instance = new ShutdownHook();
    private static final AtomicBoolean shutdownStarted = new AtomicBoolean(false);
    private volatile ChatProcessRuntimeBridge processBridge = new ChatProcessRuntimeBridge();
    private volatile RestartService restartService;
    private volatile GameServerService gameServerService;
    private volatile ObjectProvider<RestartService> restartServiceProvider;
    private volatile ObjectProvider<GameServerService> gameServerServiceProvider;
    /**
     * 为 true 时仅重启，否则正常关停。
     * When {@code true}, restart only; otherwise normal shutdown.
     */
    private static boolean restartOnly = false;

    /**
     * 无参构造：使用默认进程桥，服务延迟解析。
     * No-arg constructor: default process bridge; services resolved lazily.
     */
    public ShutdownHook() {
    }

    /**
     * 直接绑定进程桥与具体服务实例。
     * Bind process bridge and concrete service instances.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * Restart service
     * @param gameServerService 游戏服服务 / Game-server service
     */
    public ShutdownHook(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        configure(processBridge, restartService, gameServerService);
    }

    /**
     * 绑定进程桥与 Spring {@link ObjectProvider}（延迟取 Bean）。
     * Bind process bridge and Spring {@link ObjectProvider}s (lazy bean lookup).
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * @param restartServiceProvider 重启服务提供者 / Restart-service provider
     * @param gameServerServiceProvider 游戏服服务提供者 / Game-server-service provider
     */
    public ShutdownHook(
        ChatProcessRuntimeBridge processBridge,
        ObjectProvider<RestartService> restartServiceProvider,
        ObjectProvider<GameServerService> gameServerServiceProvider
    ) {
        setProcessBridge(processBridge);
        setRestartServiceProvider(restartServiceProvider);
        setGameServerServiceProvider(gameServerServiceProvider);
    }

    /**
     * 取得遗留单例关停钩子（须外部注册到 Runtime）。
     * Get the legacy singleton shutdown hook (must be registered with Runtime externally).
     *
     * @return 关停钩子单例 / Singleton shutdown hook
     * @deprecated boot 迁移后请使用 Spring Bean / Prefer the Spring bean after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance() {
        return instance;
    }

    /**
     * 配置单例的进程桥后返回。
     * Configure the singleton with the process bridge and return it.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * @return 关停钩子单例 / Singleton shutdown hook
     * @deprecated boot 迁移后请使用 Spring Bean / Prefer the Spring bean after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge) {
        instance.configure(processBridge, null, null);
        return instance;
    }

    /**
     * 配置单例的进程桥与重启服务后返回。
     * Configure the singleton with process bridge and restart service, then return it.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * Restart service
     *
     * @param processBridge @return 关停钩子单例 / Singleton shutdown hook
     * @param restartService @deprecated boot 迁移后请使用 Spring Bean / Prefer the Spring bean after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge, RestartService restartService) {
        return getInstance(processBridge, restartService, null);
    }

    /**
     * 配置单例的进程桥与全部服务后返回。
     * Configure the singleton with process bridge and all services, then return it.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * Restart service
     *
     * @param gameServerService 游戏服服务 / Game-server service
     * @param restartService @return 关停钩子单例 / Singleton shutdown hook
     * @param gameServerService @deprecated boot 迁移后请使用 Spring Bean / Prefer the Spring bean after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        instance.configure(processBridge, restartService, gameServerService);
        return instance;
    }

    /**
     * 配置进程桥与直接服务引用。
     * Configure process bridge and direct service references.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * Restart service
     * @param gameServerService 游戏服服务 / Game-server service
     */
    void configure(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        setProcessBridge(processBridge);
        setRestartService(restartService);
        setGameServerService(gameServerService);
    }

    /**
     * 设置进程桥（非 null 时覆盖）。
     * Set process bridge (override when non-null).
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     */
    private void setProcessBridge(ChatProcessRuntimeBridge processBridge) {
        if (processBridge != null) {
            this.processBridge = processBridge;
        }
    }

    /**
     * 设置直接注入的重启服务。
     * Set directly injected restart service.
     *
     * Restart service
     */
    private void setRestartService(RestartService restartService) {
        this.restartService = restartService;
    }

    /**
     * 设置直接注入的游戏服服务。
     * Set directly injected game-server service.
     *
     * @param gameServerService 游戏服服务 / Game-server service
     */
    private void setGameServerService(GameServerService gameServerService) {
        this.gameServerService = gameServerService;
    }

    /**
     * 设置重启服务的 Spring 提供者。
     * Set Spring provider for restart service.
     *
     * @param restartServiceProvider 重启服务提供者 / Restart-service provider
     */
    private void setRestartServiceProvider(ObjectProvider<RestartService> restartServiceProvider) {
        this.restartServiceProvider = restartServiceProvider;
    }

    /**
     * 设置游戏服服务的 Spring 提供者。
     * Set Spring provider for game-server service.
     *
     * @param gameServerServiceProvider 游戏服服务提供者 / Game-server-service provider
     */
    private void setGameServerServiceProvider(ObjectProvider<GameServerService> gameServerServiceProvider) {
        this.gameServerServiceProvider = gameServerServiceProvider;
    }

    /**
     * 设置是否仅重启（不正常退出）。
     * Set whether to restart only (instead of normal exit).
     *
     * @param restartOnly 为 true 表示仅重启 / {@code true} means restart only
     */
    public static void setRestartOnly(boolean restartOnly) {
        ShutdownHook.restartOnly = restartOnly;
    }

    /**
     * JVM 钩子入口：执行可 halt 的关停。
     * JVM hook entry: perform shutdown that may halt the JVM.
     */
    @Override
    public void run() {
        shutdown(true);
    }

    /**
     * 执行一次关停（幂等）：停重启服务、Netty、标记 GS 离线、关线程池，可选 halt。
     * Perform one-shot shutdown (idempotent): stop restart service, Netty, mark GS offline, shut thread pool, optionally halt.
     *
     * Whether to halt the JVM
     */
    public void shutdown(boolean haltJvm) {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        restartService().shutdown();
        ChatNettyServers.shutdownIfInitialized();
        gameServerService().setOffline();
        CommonsNetworkThreadPoolServices.threadPoolManager().shutdown();

        if (!haltJvm) {
            return;
        }

        // 执行系统退出 / Do system exit
        if (restartOnly) {
            processBridge.halt(ExitCode.CODE_RESTART);
        } else {
            processBridge.halt(ExitCode.CODE_NORMAL);
        }
    }

    /**
     * 解析重启服务：直接引用 → Provider → 遗留静态定位。
     * Resolve restart service: direct ref → provider → legacy static locator.
     *
     * Restart service
     */
    private RestartService restartService() {
        RestartService configuredRestartService = restartService;
        if (configuredRestartService != null) {
            return configuredRestartService;
        }
        ObjectProvider<RestartService> provider = restartServiceProvider;
        if (provider != null) {
            RestartService providedRestartService = provider.getIfAvailable();
            if (providedRestartService != null) {
                return providedRestartService;
            }
        }
        return ChatRestartServices.restartService();
    }

    /**
     * 解析游戏服服务：直接引用 → Provider → 遗留静态定位。
     * Resolve game-server service: direct ref → provider → legacy static locator.
     *
     * @return 游戏服服务 / Game-server service
     */
    private GameServerService gameServerService() {
        GameServerService configuredGameServerService = gameServerService;
        if (configuredGameServerService != null) {
            return configuredGameServerService;
        }
        ObjectProvider<GameServerService> provider = gameServerServiceProvider;
        if (provider != null) {
            GameServerService providedGameServerService = provider.getIfAvailable();
            if (providedGameServerService != null) {
                return providedGameServerService;
            }
        }
        return ChatCoreServices.gameServerService();
    }
}
