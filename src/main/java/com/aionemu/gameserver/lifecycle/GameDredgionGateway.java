package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 挖掘场网关：在自动组队开启时初始化挖掘场与 Asyunatar 服务。
 * Dredgion gateway: initializes Dredgion and Asyunatar services when auto-group is enabled.
 */
@Component
public class GameDredgionGateway {

    /**
     * 挖掘场服务提供者。
     * Dredgion service provider.
     */
    private ObjectProvider<DredgionService2> dredgionServiceProvider;

    /**
     * Asyunatar 服务提供者。
     * Asyunatar service provider.
     */
    private ObjectProvider<AsyunatarService> asyunatarServiceProvider;

    /**
     * 功能服务运行时桥提供者。
     * Feature-services runtime-bridge provider.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入挖掘场服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Dredgion service.
     *
     * @param dredgionServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setDredgionServiceProvider(ObjectProvider<DredgionService2> dredgionServiceProvider) {
        this.dredgionServiceProvider = dredgionServiceProvider;
    }

    /**
     * 可选注入 Asyunatar 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Asyunatar service.
     *
     * @param asyunatarServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setAsyunatarServiceProvider(ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        this.asyunatarServiceProvider = asyunatarServiceProvider;
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
     * 启动挖掘场阶段：打印分区并在自动组队开启时初始化相关服务。
     * Start the dredgion stage: print section and init related services when auto-group is on.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.dredgion"));
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            dredgionService().initDredgion();
        }
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            asyunatarService().initAsyunatar();
        }
    }

    /**
     * 解析挖掘场服务：优先 Spring，否则经运行时桥回退。
     * Resolve Dredgion service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * @return 服务实例 / Service instance
     */
    private DredgionService2 dredgionService() {
        if (dredgionServiceProvider == null) {
            return runtimeBridge().dredgionService();
        }
        return dredgionServiceProvider.getIfAvailable(() -> runtimeBridge().dredgionService());
    }

    /**
     * 解析 Asyunatar 服务。
     * Resolve Asyunatar service.
     *
     * @return 服务实例 / Service instance
     */
    private AsyunatarService asyunatarService() {
        if (asyunatarServiceProvider == null) {
            return runtimeBridge().asyunatarService();
        }
        return asyunatarServiceProvider.getIfAvailable(() -> runtimeBridge().asyunatarService());
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
