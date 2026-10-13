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
 * 可选服务生命周期：按配置启动玩家限制、NPC 喊话与攻城护盾等可选服务，加载状态由基类承载。
 * Optional-services lifecycle: starts player limits, NPC shouts and siege shields by config;
 * load state is hosted by the base class.
 */
@Component
public class GameOptionalServicesLifecycle extends GameStartupStage {

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
     * 可选注入玩家限制服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of player-limit service.
     * @param playerLimitServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setPlayerLimitServiceProvider(ObjectProvider<PlayerLimitService> playerLimitServiceProvider) {
        this.playerLimitServiceProvider = playerLimitServiceProvider;
    }

    /**
     * 可选注入 NPC 喊话服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of NPC-shouts service.
     * @param npcShoutsServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setNpcShoutsServiceProvider(ObjectProvider<NpcShoutsService> npcShoutsServiceProvider) {
        this.npcShoutsServiceProvider = npcShoutsServiceProvider;
    }

    /**
     * 可选注入护盾服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of shield service.
     * @param shieldServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setShieldServiceProvider(ObjectProvider<ShieldService> shieldServiceProvider) {
        this.shieldServiceProvider = shieldServiceProvider;
    }


    /**
     * 启动本阶段：按配置调度玩家限制、解析 NPC 喊话并生成护盾。
     * Start this stage: schedule player limits, resolve NPC shouts and spawn shields by config.
     */
    @Override
    protected void runStage() {
        if (isLimitsEnabled()) {
            playerLimitService().scheduleUpdate();
        }
        if (isNpcShoutsEnabled()) {
            npcShoutsServiceProvider.getObject();
        }
        if (isSiegeShieldEnabled()) {
            shieldService().spawnAll();
        }
    }

    /**
     * 玩家限制是否已启用。
     * Whether player limits are enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isLimitsEnabled() {
        return CustomConfig.LIMITS_ENABLED;
    }

    /**
     * NPC 喊话是否已启用。
     * Whether NPC shouts are enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isNpcShoutsEnabled() {
        return AIConfig.SHOUTS_ENABLE;
    }

    /**
     * 攻城护盾是否已启用。
     * Whether siege shields are enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isSiegeShieldEnabled() {
        return SiegeConfig.SIEGE_SHIELD_ENABLED;
    }

    /**
     * 解析玩家限制服务：优先 Spring，否则经运行时桥回退。
     * Resolve player-limit service: prefer Spring, otherwise fall back via runtime bridge.
     * @return 服务实例 / Service instance
     */
    private PlayerLimitService playerLimitService() {
        return playerLimitServiceProvider.getObject();
    }

    /**
     * 解析护盾服务：优先 Spring，否则经运行时桥回退。
     * Resolve shield service: prefer Spring, otherwise fall back via runtime bridge.
     * @return 服务实例 / Service instance
     */
    private ShieldService shieldService() {
        return shieldServiceProvider.getObject();
    }
}
