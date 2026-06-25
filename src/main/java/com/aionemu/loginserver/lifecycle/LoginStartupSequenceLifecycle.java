package com.aionemu.loginserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginStartupSequenceLifecycle {

    private final LoginStartupGateway startupGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

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

    public synchronized void reset() {
        loaded = false;
        lastFailure = null;
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }

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
