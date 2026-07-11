package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 守护者/征服者系统网关：打印分区并初始化守护者/征服者服务。
 * Protector/Conqueror gateway: prints the section and initializes the protector/conqueror service.
 */
@Component
public class GameProtectorConquerorGateway {

    /**
     * 守护者/征服者服务提供者。
     * Protector/Conqueror service provider.
     */
    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;

    /**
     * 功能服务运行时桥提供者。
     * Feature-services runtime-bridge provider.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入守护者/征服者服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of protector/conqueror service.
     *
     * @param protectorConquerorServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
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
     * 启动守护者/征服者系统：打印分区并初始化服务。
     * Start the protector/conqueror system: print the section and initialize the service.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.protector_conqueror"));
        protectorConquerorService().initSystem();
    }

    /**
     * 解析守护者/征服者服务：优先 Spring，否则经运行时桥回退。
     * Resolve protector/conqueror service: prefer Spring, otherwise fall back via runtime bridge.
     *
     * Service instance
     */
    private ProtectorConquerorService protectorConquerorService() {
        if (protectorConquerorServiceProvider == null) {
            return runtimeBridge().protectorConquerorService();
        }
        return protectorConquerorServiceProvider.getIfAvailable(() -> runtimeBridge().protectorConquerorService());
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
