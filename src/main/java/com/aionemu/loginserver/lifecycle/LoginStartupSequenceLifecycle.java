package com.aionemu.loginserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录服启动序列生命周期：按固定顺序驱动 {@link LoginStartupGateway} 各步骤，并记录加载状态与耗时。
 * Login-server startup-sequence lifecycle that drives each {@link LoginStartupGateway} step in a fixed order
 * and records load state and elapsed time.
 */
@Component
@RequiredArgsConstructor
public class LoginStartupSequenceLifecycle {

    private final LoginStartupGateway startupGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    /**
     * 执行完整启动序列；已加载时直接返回。
     * Run the full startup sequence; no-op if already loaded.
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = startupGateway.currentTimeMillis();
        try {
            startupGateway.initializeLogger();
            startupGateway.initializeCronService();
            startupGateway.logStartupTimestamp();
            startupGateway.loadConfig();
            startupGateway.initializeDatabase();
            startupGateway.initializeDaos();
            startupGateway.synchronizeVipAccounts();
            startupGateway.startDeadlockDetector();
            startupGateway.initializeThreadPool();
            initializeKeyGenerator();
            startupGateway.loadGameServers();
            startupGateway.startBannedIpController();
            startupGateway.cleanExpiredMacBans();
            startupGateway.connectNetwork();
            startupGateway.initializePlayerTransferService();
            startupGateway.initializeTaskManager();
            if (!startupGateway.isBootEmbedded()) {
                startupGateway.registerShutdownHook();
            }
            startupGateway.printInfos();
            startupGateway.initializePremiumController();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = startupGateway.currentTimeMillis() - start;
        }
    }

    /**
     * 重置加载状态与上次失败记录，便于重新启动。
     * Reset loaded flag and last-failure record so startup can run again.
     */
    public synchronized void reset() {
        loaded = false;
        lastFailure = null;
    }

    /**
     * 是否已成功完成启动序列。
     * Whether the startup sequence completed successfully.
     *
     * @return 已加载返回 true / true if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 最近一次启动序列耗时（毫秒）；未执行过为 -1。
     * Elapsed millis of the last startup sequence; -1 if never run.
     *
     * elapsed milliseconds
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近一次启动失败的异常；成功则为 null。
     * Exception from the last failed startup; null on success.
     *
     * last failure, or null
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }

    /**
     * 初始化密钥生成器：失败时嵌入式模式抛错，独立模式错误退出。
     * Initialize the key generator: rethrow in embedded mode, exit with error in standalone mode.
     */
    private void initializeKeyGenerator() {
        try {
            startupGateway.initializeKeyGenerator();
        } catch (Exception e) {
            startupGateway.logKeyGeneratorFailure(e);
            if (startupGateway.isBootEmbedded()) {
                throw new IllegalStateException("Failed initializing Key Generator", e);
            }
            startupGateway.exitWithError();
        }
    }
}
