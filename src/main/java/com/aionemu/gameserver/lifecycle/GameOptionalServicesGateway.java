package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 可选服务网关：按配置启动玩家限制、NPC 喊话与攻城护盾等可选服务。
 * Optional-services gateway: starts player limits, NPC shouts and siege shields by config.
 */
@Component
public class GameOptionalServicesGateway {

    /**
     * 玩家限制服务提供者。
     * Player-limit service provider.
     */
    private ObjectProvider<PlayerLimitService> playerLimitServiceProvider;

    /**
     * NPC 喊话服务提供者。
     * NPC-shouts service provider.
     */
    private ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;

    /**
     * 护盾服务提供者。
     * Shield service provider.
     */
    private ObjectProvider<ShieldService> shieldServiceProvider;

    /**
     * 功能服务运行时桥提供者。
     * Feature-services runtime-bridge provider.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入玩家限制服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of player-limit service.
     *
     * @param playerLimitServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setPlayerLimitServiceProvider(ObjectProvider<PlayerLimitService> playerLimitServiceProvider) {
        this.playerLimitServiceProvider = playerLimitServiceProvider;
    }

    /**
     * 可选注入 NPC 喊话服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of NPC-shouts service.
     *
     * @param npcShoutsServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setNpcShoutsServiceProvider(ObjectProvider<NpcShoutsService> npcShoutsServiceProvider) {
        this.npcShoutsServiceProvider = npcShoutsServiceProvider;
    }

    /**
     * 可选注入护盾服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of shield service.
     *
     * @param shieldServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setShieldServiceProvider(ObjectProvider<ShieldService> shieldServiceProvider) {
        this.shieldServiceProvider = shieldServiceProvider;
    }

    /**
     * 可选注入功能服务运行时桥 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of feature-services runtime bridge.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动可选服务：按配置调度玩家限制、解析 NPC 喊话并生成护盾。
     * Start optional services: schedule player limits, resolve NPC shouts and spawn shields by config.
     */
    public void start() {
        if (CustomConfig.LIMITS_ENABLED) {
            playerLimitService().scheduleUpdate();
        }
        if (AIConfig.SHOUTS_ENABLE) {
            npcShoutsService();
        }
        if (SiegeConfig.SIEGE_SHIELD_ENABLED) {
            shieldService().spawnAll();
        }
    }

    /**
     * 解析玩家限制服务：优先 Spring，否则经运行时桥回退。
     * Resolve player-limit service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private PlayerLimitService playerLimitService() {
        if (playerLimitServiceProvider == null) {
            return runtimeBridge().playerLimitService();
        }
        return playerLimitServiceProvider.getIfAvailable(() -> runtimeBridge().playerLimitService());
    }

    /**
     * 解析 NPC 喊话服务：优先 Spring，否则经运行时桥回退。
     * Resolve NPC-shouts service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private NpcShoutsService npcShoutsService() {
        if (npcShoutsServiceProvider == null) {
            return runtimeBridge().npcShoutsService();
        }
        return npcShoutsServiceProvider.getIfAvailable(() -> runtimeBridge().npcShoutsService());
    }

    /**
     * 解析护盾服务：优先 Spring，否则经运行时桥回退。
     * Resolve shield service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private ShieldService shieldService() {
        if (shieldServiceProvider == null) {
            return runtimeBridge().shieldService();
        }
        return shieldServiceProvider.getIfAvailable(() -> runtimeBridge().shieldService());
    }

    /**
     * 解析功能服务运行时桥：优先 Spring，否则新建。
     * Resolve feature-services runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥实例 / Runtime-bridge instance
     */
    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
