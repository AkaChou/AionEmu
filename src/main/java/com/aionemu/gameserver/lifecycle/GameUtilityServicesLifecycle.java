package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import com.aionemu.gameserver.utils.javaagent.JavaAgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameUtilityServicesLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameUtilityServicesLifecycle.class);

    private final Runnable exceptionHandlerInitializer;
    private final Runnable callbackSupportReporter;
    private final Runnable cronInitializer;
    private final Runnable configSectionPrinter;
    private final Runnable configLoader;
    private final Runnable dateTimeInitializer;
    private final Runnable databaseSectionPrinter;
    private final Runnable databaseFactoryInitializer;
    private final Runnable daoInitializer;
    private final Runnable threadConfigLoader;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameUtilityServicesLifecycle() {
        this(
            () -> Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler()),
            GameUtilityServicesLifecycle::reportCallbackSupport,
            () -> CronService.initSingleton(ThreadPoolManagerRunnableRunner.class),
            () -> Util.printSection(" *** Config *** "),
            Config::load,
            DateTimeUtil::init,
            () -> Util.printSection(" *** DataBase *** "),
            DatabaseFactory::init,
            DAOManager::init,
            ThreadConfig::load
        );
    }

    GameUtilityServicesLifecycle(
        Runnable exceptionHandlerInitializer,
        Runnable callbackSupportReporter,
        Runnable cronInitializer,
        Runnable configSectionPrinter,
        Runnable configLoader,
        Runnable dateTimeInitializer,
        Runnable databaseSectionPrinter,
        Runnable databaseFactoryInitializer,
        Runnable daoInitializer,
        Runnable threadConfigLoader
    ) {
        this.exceptionHandlerInitializer = exceptionHandlerInitializer;
        this.callbackSupportReporter = callbackSupportReporter;
        this.cronInitializer = cronInitializer;
        this.configSectionPrinter = configSectionPrinter;
        this.configLoader = configLoader;
        this.dateTimeInitializer = dateTimeInitializer;
        this.databaseSectionPrinter = databaseSectionPrinter;
        this.databaseFactoryInitializer = databaseFactoryInitializer;
        this.daoInitializer = daoInitializer;
        this.threadConfigLoader = threadConfigLoader;
    }

    public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            exceptionHandlerInitializer.run();
            callbackSupportReporter.run();
            cronInitializer.run();
            configSectionPrinter.run();
            long configStart = System.currentTimeMillis();
            configLoader.run();
            long configTime = System.currentTimeMillis() - configStart;
            log.info("Configuration loaded in {} ms", configTime);
            dateTimeInitializer.run();
            databaseSectionPrinter.run();
            long dbStart = System.currentTimeMillis();
            databaseFactoryInitializer.run();
            long dbInitTime = System.currentTimeMillis() - dbStart;
            log.info("Database factory initialized in {} ms", dbInitTime);
            long daoStart = System.currentTimeMillis();
            daoInitializer.run();
            long daoTime = System.currentTimeMillis() - daoStart;
            log.info("DAO Manager initialized in {} ms", daoTime);
            threadConfigLoader.run();
            threadPoolLifecycle.start();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
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

    private static void reportCallbackSupport() {
        if (JavaAgentUtils.isConfigured()) {
            log.info("Callback support is configured.");
        } else {
            log.warn("Callback support is NOT configured. Gameplay callback behavior may be affected.");
        }
    }
}
