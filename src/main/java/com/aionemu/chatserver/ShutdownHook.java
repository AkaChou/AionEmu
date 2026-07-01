/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


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
     * Indicates wether the loginserver should shut dpwn or only restart
     */
    private static boolean restartOnly = false;

    public ShutdownHook() {
    }

    public ShutdownHook(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        configure(processBridge, restartService, gameServerService);
    }

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
     * get the shutdown-hook instance the shutdown-hook instance is created by
     * the first call of this function, but it has to be registrered externaly.
     *
     * @return instance of Shutdown, to be used as shutdown hook
     */
    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance() {
        return instance;
    }

    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge) {
        instance.configure(processBridge, null, null);
        return instance;
    }

    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge, RestartService restartService) {
        return getInstance(processBridge, restartService, null);
    }

    @Deprecated(since = "boot-migration")
    public static ShutdownHook getInstance(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        instance.configure(processBridge, restartService, gameServerService);
        return instance;
    }

    void configure(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        setProcessBridge(processBridge);
        setRestartService(restartService);
        setGameServerService(gameServerService);
    }

    private void setProcessBridge(ChatProcessRuntimeBridge processBridge) {
        if (processBridge != null) {
            this.processBridge = processBridge;
        }
    }

    private void setRestartService(RestartService restartService) {
        this.restartService = restartService;
    }

    private void setGameServerService(GameServerService gameServerService) {
        this.gameServerService = gameServerService;
    }

    private void setRestartServiceProvider(ObjectProvider<RestartService> restartServiceProvider) {
        this.restartServiceProvider = restartServiceProvider;
    }

    private void setGameServerServiceProvider(ObjectProvider<GameServerService> gameServerServiceProvider) {
        this.gameServerServiceProvider = gameServerServiceProvider;
    }

    /**
     * Set's restartOnly attribute
     *
     * @param restartOnly Indicates wether the loginserver should shut dpwn or
     * only restart
     */
    public static void setRestartOnly(boolean restartOnly) {
        ShutdownHook.restartOnly = restartOnly;
    }

    @Override
    public void run() {
        shutdown(true);
    }

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

        // Do system exit
        if (restartOnly) {
            processBridge.halt(ExitCode.CODE_RESTART);
        } else {
            processBridge.halt(ExitCode.CODE_NORMAL);
        }
    }

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
