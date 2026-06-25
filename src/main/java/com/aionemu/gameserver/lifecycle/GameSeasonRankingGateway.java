package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameSeasonRankingGateway {

    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Season Ranking *** ");
        seasonRankingUpdateService().onStart();
    }

    private SeasonRankingUpdateService seasonRankingUpdateService() {
        if (seasonRankingUpdateServiceProvider == null) {
            return SeasonRankingUpdateService.getInstance();
        }
        return seasonRankingUpdateServiceProvider.getIfAvailable(SeasonRankingUpdateService::getInstance);
    }
}
