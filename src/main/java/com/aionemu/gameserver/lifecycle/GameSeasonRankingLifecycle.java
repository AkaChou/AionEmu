package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 赛季排名生命周期：打印分区并触发 {@link SeasonRankingUpdateService#onStart()}，加载状态由基类承载。
 * Season-ranking lifecycle: prints the section and invokes {@link SeasonRankingUpdateService#onStart()};
 * load state is hosted by the base class.
 */
@Component
public class GameSeasonRankingLifecycle extends GameStartupStage {

    /**
     * 赛季排名更新服务提供者（可选）。
     * Optional provider for the season ranking update service.
     */
    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;


    /**
     * 注入赛季排名更新服务提供者。
     * Inject the season ranking update service provider.
     * @param seasonRankingUpdateServiceProvider 更新服务提供者 / Update service provider
     */
    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }


    /**
     * 启动本阶段：打印赛季排名分区并启动更新服务。
     * Start this stage: print the season-ranking section and start the update service.
     */
    @Override
    protected void runStage() {
        Util.printSectionIfVerbose(I18n.get("console.section.season_ranking"));
        seasonRankingUpdateService().onStart();
    }

    /**
     * 解析赛季排名更新服务。
     * Resolve the season ranking update service.
     * @return 更新服务 / Update service
     */
    private SeasonRankingUpdateService seasonRankingUpdateService() {
        return seasonRankingUpdateServiceProvider.getObject();
    }
}
