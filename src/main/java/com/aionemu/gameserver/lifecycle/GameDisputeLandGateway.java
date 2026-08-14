package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 争议之地网关：初始化争议之地与前哨服务。
 * Dispute-land gateway: initializes dispute-land and outpost services.
 */
@Component
public class GameDisputeLandGateway {

    /**
     * 争议之地服务提供者。
     * Dispute-land service provider.
     */
    private ObjectProvider<DisputeLandService> disputeLandServiceProvider;

    /**
     * 前哨服务提供者。
     * Outpost service provider.
     */
    private ObjectProvider<OutpostService> outpostServiceProvider;

    /**
     * 功能服务运行时桥提供者。
     * Feature-services runtime-bridge provider.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入争议之地服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of dispute-land service.
     *
     * @param disputeLandServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setDisputeLandServiceProvider(ObjectProvider<DisputeLandService> disputeLandServiceProvider) {
        this.disputeLandServiceProvider = disputeLandServiceProvider;
    }

    /**
     * 可选注入前哨服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of outpost service.
     *
     * @param outpostServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
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
     * 启动争议之地阶段：打印分区并初始化争议之地与前哨。
     * Start the dispute-land stage: print section and init dispute land and outposts.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.dispute_land"));
        disputeLandService().initDisputeLand();
        outpostService().initOutposts();
    }

    /**
     * 解析争议之地服务：优先 Spring，否则经运行时桥回退。
     * Resolve dispute-land service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private DisputeLandService disputeLandService() {
        if (disputeLandServiceProvider == null) {
            return runtimeBridge().disputeLandService();
        }
        return disputeLandServiceProvider.getIfAvailable(() -> runtimeBridge().disputeLandService());
    }

    /**
     * 解析前哨服务。
     * Resolve outpost service.
     *
     * @return 服务实例 / Service instance
     */
    private OutpostService outpostService() {
        if (outpostServiceProvider == null) {
            return runtimeBridge().outpostService();
        }
        return outpostServiceProvider.getIfAvailable(() -> runtimeBridge().outpostService());
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
