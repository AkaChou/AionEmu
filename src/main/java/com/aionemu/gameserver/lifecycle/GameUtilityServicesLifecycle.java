package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameUtilityServicesLifecycle {

    private final GameUtilityServicesGateway utilityServicesGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
        if (loaded) {
            return;
        }

        long start = utilityServicesGateway.currentTimeMillis();
        try {
            utilityServicesGateway.initializeExceptionHandler();
            utilityServicesGateway.reportCallbackSupport();
            utilityServicesGateway.initializeCronService();
            utilityServicesGateway.printConfigSection();
            long configStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.loadConfig();
            long configTime = utilityServicesGateway.currentTimeMillis() - configStart;
            log.info("Configuration loaded in {} ms", configTime);
            utilityServicesGateway.initializeDateTime();
            utilityServicesGateway.printDatabaseSection();
            long dbStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.initializeDatabaseFactory();
            long dbInitTime = utilityServicesGateway.currentTimeMillis() - dbStart;
            log.info("Database factory initialized in {} ms", dbInitTime);
            long daoStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.initializeDaoManager();
            long daoTime = utilityServicesGateway.currentTimeMillis() - daoStart;
            log.info("DAO Manager initialized in {} ms", daoTime);
            utilityServicesGateway.loadThreadConfig();
            threadPoolLifecycle.start();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = utilityServicesGateway.currentTimeMillis() - start;
        }
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
}
