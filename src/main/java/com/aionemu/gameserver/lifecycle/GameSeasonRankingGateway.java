package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameSeasonRankingGateway {

    public void start() {
        Util.printSection(" *** Season Ranking *** ");
        SeasonRankingUpdateService.getInstance().onStart();
    }
}
