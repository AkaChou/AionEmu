package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
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
     * 世界服务运行时桥的可选提供者。
     * Optional provider for the world-services runtime bridge.
     */
    private ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入掉落注册服务提供者。
     * Inject the drop-registration-service provider.
     *
     * @param dropRegistrationServiceProvider 掉落注册服务提供者 / Drop-registration-service provider
     */
    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    /**
     * 注入世界服务运行时桥提供者。
     * Inject the world-services runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 激活世界：解析掉落服务、创建并激活 GameServer、标记玩家离线。
     * Activate the world: resolve drop service, create/activate GameServer, mark players offline.
     *
     * @return 激活后的 GameServer 实例 / Activated GameServer instance
     */
    public GameServer activate() {
        dropRegistrationService();
        GameWorldServicesRuntimeBridge runtimeBridge = runtimeBridge();
        GameServer server = runtimeBridge.createGameServer();
        runtimeBridge.activateGameServer(server);
        runtimeBridge.markPlayersOffline();
        return server;
    }

    /**
     * 解析掉落注册服务：优先 Spring，否则运行时桥。
     * Resolve drop-registration service: prefer Spring, otherwise runtime bridge.
     *
     * @return DropRegistrationService 实例 / DropRegistrationService instance
     */
    private DropRegistrationService dropRegistrationService() {
        if (dropRegistrationServiceProvider == null) {
            return runtimeBridge().dropRegistrationService();
        }
        return dropRegistrationServiceProvider.getIfAvailable(() -> runtimeBridge().dropRegistrationService());
    }

    /**
     * 解析世界服务运行时桥：优先 Spring，否则新建。
     * Resolve the world-services runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥 / Runtime bridge
     */
    private GameWorldServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldServicesRuntimeBridge::new);
    }
}
