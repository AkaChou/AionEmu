package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.controller.BannedIpController;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.network.ncrypt.KeyGen;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.DeadLockDetector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class LoginStartupRuntimeBridge {

    public void initializeLogger() {
        LoginServer.initializeLogger();
    }

    public void initializeCronService() {
        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    public void loadConfig() {
        Config.load();
    }

    public void initializeDatabase() {
        DatabaseFactory.init();
    }

    public void initializeDaos() {
        DAOManager.init();
    }

    public void startDeadlockDetector(boolean bootEmbedded) {
        DeadLockDetector deadLockDetector = new DeadLockDetector(
            60,
            bootEmbedded ? DeadLockDetector.NOTHING : DeadLockDetector.RESTART
        );
        deadLockDetector.setDaemon(bootEmbedded);
        deadLockDetector.start();
    }

    public void initializeThreadPool() {
        ThreadPoolManager.getInstance();
    }

    public void initializeKeyGenerator() throws Exception {
        KeyGen.init();
    }

    public void loadGameServers() {
        GameServerTable.load();
    }

    public void startBannedIpController() {
        BannedIpController.start();
    }

    public void cleanExpiredMacBans() {
        DAOManager.getDAO(BannedMacDAO.class).cleanExpiredBans();
    }

    public void connectNetwork() {
        NetConnector.getInstance().connect();
    }

    public void initializeTaskManager() {
        TaskFromDBManager.getInstance();
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
    }

    public void printInfos() {
        AEInfos.printAllInfos();
    }

    public void initializePremiumController() {
        PremiumController.getController();
    }

    public void exitWithError() {
        System.exit(ExitCode.CODE_ERROR);
    }
}
