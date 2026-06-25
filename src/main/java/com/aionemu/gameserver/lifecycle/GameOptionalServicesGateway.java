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

@Component
public class GameOptionalServicesGateway {

    private ObjectProvider<PlayerLimitService> playerLimitServiceProvider;
    private ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;
    private ObjectProvider<ShieldService> shieldServiceProvider;
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setPlayerLimitServiceProvider(ObjectProvider<PlayerLimitService> playerLimitServiceProvider) {
        this.playerLimitServiceProvider = playerLimitServiceProvider;
    }

    @Autowired(required = false)
    void setNpcShoutsServiceProvider(ObjectProvider<NpcShoutsService> npcShoutsServiceProvider) {
        this.npcShoutsServiceProvider = npcShoutsServiceProvider;
    }

    @Autowired(required = false)
    void setShieldServiceProvider(ObjectProvider<ShieldService> shieldServiceProvider) {
        this.shieldServiceProvider = shieldServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

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

    private PlayerLimitService playerLimitService() {
        if (playerLimitServiceProvider == null) {
            return runtimeBridge().playerLimitService();
        }
        return playerLimitServiceProvider.getIfAvailable(() -> runtimeBridge().playerLimitService());
    }

    private NpcShoutsService npcShoutsService() {
        if (npcShoutsServiceProvider == null) {
            return runtimeBridge().npcShoutsService();
        }
        return npcShoutsServiceProvider.getIfAvailable(() -> runtimeBridge().npcShoutsService());
    }

    private ShieldService shieldService() {
        if (shieldServiceProvider == null) {
            return runtimeBridge().shieldService();
        }
        return shieldServiceProvider.getIfAvailable(() -> runtimeBridge().shieldService());
    }

    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
