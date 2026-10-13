package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 世界激活生命周期：初始化掉落注册、创建并激活 GameServer、标记玩家离线，并返回激活后的 GameServer。
 * World-activation lifecycle: inits drop registration, creates/activates GameServer, marks players
 * offline, and returns the activated GameServer.
 */
@Component
public class GameWorldActivationLifecycle {

    /**
     * DropRegistrationService 的可选 Spring 提供者。
     * Optional Spring provider for {@link DropRegistrationService}.
     */
    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    /**
     * 当前激活的 GameServer。
     * Currently activated GameServer.
     */
    private GameServer activeServer;

    /**
     * 是否已激活。
     * Whether the world has been activated.
     */
    private boolean activated;

    /**
     * 激活耗时毫秒；未启动前为 -1。
     * Activation time in milliseconds; {@code -1} before start.
     */
    private long activationTimeMillis = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 注入掉落注册服务提供者。
     * Inject the drop-registration-service provider.
     * @param dropRegistrationServiceProvider 掉落注册服务提供者 / Drop-registration-service provider
     */
    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    /**
     * 启动本阶段：激活世界并返回 GameServer。
     * Start this stage: activate the world and return the GameServer.
     * @return 激活后的 GameServer；已激活则返回缓存实例 / Activated GameServer; cached instance if already activated
     */
    public synchronized GameServer start() {
        if (activated) {
            return activeServer;
        }

        long start = System.currentTimeMillis();
        try {
            activeServer = activate();
            activated = true;
            lastFailure = null;
            return activeServer;
        } catch (RuntimeException | Error e) {
            activeServer = null;
            activated = false;
            lastFailure = e;
            throw e;
        } finally {
            activationTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 激活世界：解析掉落服务、创建并激活 GameServer、标记玩家离线。
     * Activate the world: resolve drop service, create/activate GameServer, mark players offline.
     * @return 激活后的 GameServer 实例 / Activated GameServer instance
     */
    protected GameServer activate() {
        dropRegistrationServiceProvider.getObject();
        GameServer server = new GameServer();
        GameServer.activateServer(server);
        markPlayersOffline();
        return server;
    }

    /**
     * 将全部玩家标记为离线。
     * Mark every player offline.
     */
    private void markPlayersOffline() {
        PlayerService.daos().playerDAO().setPlayersOffline(false);
    }

    /**
     * 是否已激活。
     * Whether the world has been activated.
     * @return 已激活为 {@code true} / {@code true} if activated
     */
    public synchronized boolean isActivated() {
        return activated;
    }

    /**
     * 激活耗时毫秒。
     * Activation time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getActivationTimeMillis() {
        return activationTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
