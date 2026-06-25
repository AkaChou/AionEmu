package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameUtilityServicesGateway {

    private ObjectProvider<GameUtilityServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameUtilityServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void initializeExceptionHandler() {
        runtimeBridge().initializeExceptionHandler();
    }

    public void reportCallbackSupport() {
        runtimeBridge().reportCallbackSupport();
    }

    public void initializeCronService() {
        runtimeBridge().initializeCronService();
    }

    public void printConfigSection() {
        Util.printSection(" *** Config *** ");
    }

    public void loadConfig() {
        runtimeBridge().loadConfig();
    }

    public void initializeDateTime() {
        runtimeBridge().initializeDateTime();
    }

    public void printDatabaseSection() {
        Util.printSection(" *** DataBase *** ");
    }

    public void initializeDatabaseFactory() {
        runtimeBridge().initializeDatabaseFactory();
    }

    public void initializeDaoManager() {
        runtimeBridge().initializeDaoManager();
    }

    public void loadThreadConfig() {
        runtimeBridge().loadThreadConfig();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private GameUtilityServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameUtilityServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameUtilityServicesRuntimeBridge::new);
    }
}
