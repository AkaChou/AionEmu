package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.spawnengine.TemporarySpawnEngine;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameEventRuntimeGateway {

    public void start() {
        Util.printSection(" *** Events *** ");
        if (EventsConfig.ENABLE_EVENT_SERVICE) {
            EventService.getInstance().start();
        }
        if (EventsConfig.EVENT_ENABLED) {
            PlayerEventService.getInstance();
        }
        if (EventsConfig.ENABLE_CRAZY) {
            CrazyDaevaService.getInstance().startTimer();
        }
        if (RankingConfig.TOP_RANKING_UPDATE_SETTING) {
            AbyssRankUpdateService.getInstance().scheduleUpdateHour();
        } else {
            AbyssRankUpdateService.getInstance().scheduleUpdateMinute();
        }
        AbyssRankUpdateService.getInstance().initRewardWeeklyManager();
        PacketBroadcaster.getInstance();
        TemporarySpawnEngine.spawnAll();
    }
}
