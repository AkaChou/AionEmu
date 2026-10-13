package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 世界激活网关：初始化掉落注册、创建并激活 GameServer，标记玩家离线。
 * World-activation gateway: inits drop registration, creates/activates GameServer, marks players offline.
 */
@Component
public class GameWorldActivationGateway {

    /**
     * DropRegistrationService 的可选 Spring 提供者。
     * Optional Spring provider for {@link DropRegistrationService}.
     */
    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

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
     * 激活世界：解析掉落服务、创建并激活 GameServer、标记玩家离线。
     * Activate the world: resolve drop service, create/activate GameServer, mark players offline.
     * @return 激活后的 GameServer 实例 / Activated GameServer instance
     */
    public GameServer activate() {
        dropRegistrationService();
        GameServer server = createGameServer();
        activateGameServer(server);
        markPlayersOffline();
        return server;
    }

    /**
     * 解析掉落注册服务。
     * Resolve the drop-registration service.
     */
    private void dropRegistrationService() {
        dropRegistrationServiceProvider.getObject();
    }

    /**
     * 创建 GameServer 实例。
     * Create the GameServer instance.
     * @return 新的 GameServer / New GameServer
     */
    private GameServer createGameServer() {
        return new GameServer();
    }

    /**
     * 激活 GameServer。
     * Activate the GameServer.
     * @param server 待激活实例 / Instance to activate
     */
    private void activateGameServer(GameServer server) {
        GameServer.activateServer(server);
    }

    /**
     * 将全部玩家标记为离线。
     * Mark every player offline.
     */
    private void markPlayersOffline() {
        PlayerService.daos().playerDAO().setPlayersOffline(false);
    }
}
