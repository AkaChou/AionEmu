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
     * 可选注入挖掘场服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Dredgion service.
     * @param dredgionServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setDredgionServiceProvider(ObjectProvider<DredgionService2> dredgionServiceProvider) {
        this.dredgionServiceProvider = dredgionServiceProvider;
    }

    /**
     * 可选注入 Asyunatar 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Asyunatar service.
     * @param asyunatarServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setAsyunatarServiceProvider(ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        this.asyunatarServiceProvider = asyunatarServiceProvider;
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
     * @return 服务实例 / Service instance
     */
    private DredgionService2 dredgionService() {
        return dredgionServiceProvider.getObject();
    }

    /**
     * 解析 Asyunatar 服务。
     * Resolve Asyunatar service.
     * @return 服务实例 / Service instance
     */
    private AsyunatarService asyunatarService() {
        return asyunatarServiceProvider.getObject();
    }

}
