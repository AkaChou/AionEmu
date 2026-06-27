package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import com.aionemu.gameserver.utils.javaagent.JavaAgentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameUtilityServicesRuntimeBridge {

    public void initializeExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
    }

    public void reportCallbackSupport() {
        if (JavaAgentUtils.isConfigured()) {
            log.info("Callback support is configured.");
        } else {
            log.warn("Callback support is NOT configured. Gameplay callback behavior may be affected.");
        }
    }

    public void initializeCronService() {
        GameCronServices.initialize();
    }

    public void loadConfig() {
        Config.load();
    }

    public void initializeDateTime() {
        DateTimeUtil.init();
    }

    public void initializeDatabaseFactory() {
        DatabaseFactory.init();
    }

    public void initializeDaoManager() {
        DAOManager.init();
    }

    public void loadThreadConfig() {
        ThreadConfig.load();
    }
}
