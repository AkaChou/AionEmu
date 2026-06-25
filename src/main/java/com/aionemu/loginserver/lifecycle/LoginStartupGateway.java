package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.services.CronService;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.AionRuntimeMode;
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
import com.aionemu.loginserver.service.PlayerTransferService;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.DeadLockDetector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginStartupGateway {

    private ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    public void initializeLogger() {
        LoginServer.initializeLogger();
    }

    public void initializeCronService() {
        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    public void logStartupTimestamp() {
        log.info("\f" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(currentTimeMillis())) + "\f");
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

    public void startDeadlockDetector() {
        DeadLockDetector deadLockDetector = new DeadLockDetector(
            60,
            isBootEmbedded() ? DeadLockDetector.NOTHING : DeadLockDetector.RESTART
        );
        deadLockDetector.setDaemon(isBootEmbedded());
        deadLockDetector.start();
    }

    public void initializeThreadPool() {
        ThreadPoolManager.getInstance();
    }

    public void initializeKeyGenerator() throws Exception {
        KeyGen.init();
    }

    public void logKeyGeneratorFailure(Exception e) {
        log.error("Failed initializing Key Generator. Reason: " + e.getMessage(), e);
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

    public void initializePlayerTransferService() {
        playerTransferService();
    }

    public void initializeTaskManager() {
        TaskFromDBManager.getInstance();
    }

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
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

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private PlayerTransferService playerTransferService() {
        if (playerTransferServiceProvider == null) {
            return PlayerTransferService.getInstance();
        }
        return playerTransferServiceProvider.getIfAvailable(PlayerTransferService::getInstance);
    }
}
