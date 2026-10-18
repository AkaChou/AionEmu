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
 * 自定义活动生命周期：按配置启动 FFA、天梯/BG 与强盗活动服务，加载状态由基类承载。
 * Custom-events lifecycle: starts FFA, ladder/BG and bandit event services by configuration;
 * load state is hosted by the base class.
 */
@Component
public class GameCustomEventsLifecycle extends GameStartupStage {

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
     * 可选注入 FFA 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of FFA service.
     * @param ffaServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setFfaServiceProvider(ObjectProvider<FFAService> ffaServiceProvider) {
        this.ffaServiceProvider = ffaServiceProvider;
    }

    /**
     * 可选注入天梯服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of ladder service.
     * @param ladderServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setLadderServiceProvider(ObjectProvider<LadderService> ladderServiceProvider) {
        this.ladderServiceProvider = ladderServiceProvider;
    }

    /**
     * 可选注入 BG 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of BG service.
     * @param bgServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setBgServiceProvider(ObjectProvider<BGService> bgServiceProvider) {
        this.bgServiceProvider = bgServiceProvider;
    }

    /**
     * 可选注入强盗活动服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of bandit service.
     * @param banditServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setBanditServiceProvider(ObjectProvider<BanditService> banditServiceProvider) {
        this.banditServiceProvider = banditServiceProvider;
    }


    /**
     * 启动本阶段：打印分区并按开关初始化 FFA、天梯/BG 与强盗服务。
     * Start this stage: print section and init FFA, ladder/BG and bandit services by switches.
     */
    @Override
    protected void runStage() {
        Util.printSectionIfVerbose(I18n.get("console.section.custom_events"));
        if (FFAConfig.FFA_ENABLED) {
            ffaServiceProvider.getObject();
        }
        if (PvPModConfig.BG_ENABLED) {
            ladderServiceProvider.getObject();
            bgServiceProvider.getObject();
        }
        banditService().onInit();
    }

    /**
     * 解析强盗活动服务。
     * Resolve bandit service.
     * @return 服务实例 / Service instance
     */
    private BanditService banditService() {
        return banditServiceProvider.getObject();
    }
}
