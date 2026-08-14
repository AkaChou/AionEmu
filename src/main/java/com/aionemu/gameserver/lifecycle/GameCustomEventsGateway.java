package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自定义活动网关：按配置启动 FFA、天梯/BG 与强盗活动服务。
 * Custom-events gateway: starts FFA, ladder/BG and bandit event services by configuration.
 */
@Component
public class GameCustomEventsGateway {

    /**
     * FFA 服务提供者。
     * FFA service provider.
     */
    private ObjectProvider<FFAService> ffaServiceProvider;

    /**
     * 天梯服务提供者。
     * Ladder service provider.
     */
    private ObjectProvider<LadderService> ladderServiceProvider;

    /**
     * 战场（BG）服务提供者。
     * Battleground (BG) service provider.
     */
    private ObjectProvider<BGService> bgServiceProvider;

    /**
     * 强盗活动服务提供者。
     * Bandit event service provider.
     */
    private ObjectProvider<BanditService> banditServiceProvider;

    /**
     * 功能服务运行时桥提供者。
     * Feature-services runtime-bridge provider.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入 FFA 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of FFA service.
     *
     * @param ffaServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setFfaServiceProvider(ObjectProvider<FFAService> ffaServiceProvider) {
        this.ffaServiceProvider = ffaServiceProvider;
    }

    /**
     * 可选注入天梯服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of ladder service.
     *
     * @param ladderServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setLadderServiceProvider(ObjectProvider<LadderService> ladderServiceProvider) {
        this.ladderServiceProvider = ladderServiceProvider;
    }

    /**
     * 可选注入 BG 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of BG service.
     *
     * @param bgServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setBgServiceProvider(ObjectProvider<BGService> bgServiceProvider) {
        this.bgServiceProvider = bgServiceProvider;
    }

    /**
     * 可选注入强盗活动服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of bandit service.
     *
     * @param banditServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setBanditServiceProvider(ObjectProvider<BanditService> banditServiceProvider) {
        this.banditServiceProvider = banditServiceProvider;
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
     * 启动自定义活动：打印分区并按开关初始化 FFA、天梯/BG 与强盗服务。
     * Start custom events: print section and init FFA, ladder/BG and bandit services by switches.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.custom_events"));
        if (FFAConfig.FFA_ENABLED) {
            ffaService();
        }
        if (PvPModConfig.BG_ENABLED) {
            ladderService();
            bgService();
        }
        banditService().onInit();
    }

    /**
     * 解析 FFA 服务：优先 Spring，否则经运行时桥回退。
     * Resolve FFA service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private FFAService ffaService() {
        if (ffaServiceProvider == null) {
            return runtimeBridge().ffaService();
        }
        return ffaServiceProvider.getIfAvailable(() -> runtimeBridge().ffaService());
    }

    /**
     * 解析天梯服务。
     * Resolve ladder service.
     *
     * @return 服务实例 / Service instance
     */
    private LadderService ladderService() {
        if (ladderServiceProvider == null) {
            return runtimeBridge().ladderService();
        }
        return ladderServiceProvider.getIfAvailable(() -> runtimeBridge().ladderService());
    }

    /**
     * 解析 BG 服务。
     * Resolve BG service.
     *
     * @return 服务实例 / Service instance
     */
    private BGService bgService() {
        if (bgServiceProvider == null) {
            return runtimeBridge().bgService();
        }
        return bgServiceProvider.getIfAvailable(() -> runtimeBridge().bgService());
    }

    /**
     * 解析强盗活动服务。
     * Resolve bandit service.
     *
     * @return 服务实例 / Service instance
     */
    private BanditService banditService() {
        if (banditServiceProvider == null) {
            return runtimeBridge().banditService();
        }
        return banditServiceProvider.getIfAvailable(() -> runtimeBridge().banditService());
    }

    /**
     * 解析功能服务运行时桥：优先 Spring，否则新建。
     * Resolve feature-services runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥 / Runtime bridge
     */
    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
