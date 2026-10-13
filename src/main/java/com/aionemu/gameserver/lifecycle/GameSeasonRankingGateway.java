package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 赛季排名启动网关：打印分区并触发 {@link SeasonRankingUpdateService#onStart()}。
 * Gateway that starts season ranking: prints the section and invokes {@link SeasonRankingUpdateService#onStart()}.
 */
@Component
public class GameSeasonRankingGateway {

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
     * 打印赛季排名分区并启动更新服务。
     * Print the season-ranking section and start the update service.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.season_ranking"));
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
