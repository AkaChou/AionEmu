package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 守护者/征服者系统生命周期：打印分区并初始化守护者/征服者服务，加载状态由基类承载。
 * Protector/Conqueror lifecycle: prints the section and initializes the protector/conqueror
 * service; load state is hosted by the base class.
 */
@Component
public class GameProtectorConquerorLifecycle extends GameStartupStage {

    /**
     * 守护者/征服者服务提供者。
     * Protector/Conqueror service provider.
     */
    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;


    /**
     * 可选注入守护者/征服者服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of protector/conqueror service.
     * @param protectorConquerorServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
    }


    /**
     * 启动本阶段：打印分区并初始化守护者/征服者服务。
     * Start this stage: print the section and initialize the service.
     */
    @Override
    protected void runStage() {
        Util.printSectionIfVerbose(I18n.get("console.section.protector_conqueror"));
        protectorConquerorService().initSystem();
    }

    /**
     * 解析守护者/征服者服务：优先 Spring，否则经运行时桥回退。
     * Resolve protector/conqueror service: prefer Spring, otherwise fall back via runtime bridge.
     * @return 服务实例 / Service instance
     */
    private ProtectorConquerorService protectorConquerorService() {
        return protectorConquerorServiceProvider.getObject();
    }
}
