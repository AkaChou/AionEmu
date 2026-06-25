package com.aionemu.loginserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.service.PlayerTransferService;
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
    private ObjectProvider<LoginStartupRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setPlayerTransferServiceProvider(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        this.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<LoginStartupRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void initializeLogger() {
        runtimeBridge().initializeLogger();
    }

    public void initializeCronService() {
        runtimeBridge().initializeCronService();
    }

    public void logStartupTimestamp() {
        log.info("\f" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(currentTimeMillis())) + "\f");
    }

    public void loadConfig() {
        runtimeBridge().loadConfig();
    }

    public void initializeDatabase() {
        runtimeBridge().initializeDatabase();
    }

    public void initializeDaos() {
        runtimeBridge().initializeDaos();
    }

    public void startDeadlockDetector() {
        runtimeBridge().startDeadlockDetector(isBootEmbedded());
    }

    public void initializeThreadPool() {
        runtimeBridge().initializeThreadPool();
    }

    public void initializeKeyGenerator() throws Exception {
        runtimeBridge().initializeKeyGenerator();
    }

    public void logKeyGeneratorFailure(Exception e) {
        log.error("Failed initializing Key Generator. Reason: " + e.getMessage(), e);
    }

    public void loadGameServers() {
        runtimeBridge().loadGameServers();
    }

    public void startBannedIpController() {
        runtimeBridge().startBannedIpController();
    }

    public void cleanExpiredMacBans() {
        runtimeBridge().cleanExpiredMacBans();
    }

    public void connectNetwork() {
        runtimeBridge().connectNetwork();
    }

    public void initializePlayerTransferService() {
        playerTransferService();
    }

    public void initializeTaskManager() {
        runtimeBridge().initializeTaskManager();
    }

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    public void registerShutdownHook() {
        runtimeBridge().registerShutdownHook();
    }

    public void printInfos() {
        runtimeBridge().printInfos();
    }

    public void initializePremiumController() {
        runtimeBridge().initializePremiumController();
    }

    public void exitWithError() {
        runtimeBridge().exitWithError();
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

    private LoginStartupRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new LoginStartupRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(LoginStartupRuntimeBridge::new);
    }
}
