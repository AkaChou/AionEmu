package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameEventServices implements DisposableBean {

    public GameEventServices(ObjectProvider<EventService> eventServiceProvider,
            ObjectProvider<PlayerEventService> playerEventServiceProvider,
            ObjectProvider<CrazyDaevaService> crazyDaevaServiceProvider,
            ObjectProvider<AbyssRankUpdateService> abyssRankUpdateServiceProvider,
            ObjectProvider<PacketBroadcaster> packetBroadcasterProvider) {
        EventService.setInstanceProvider(eventServiceProvider);
        PlayerEventService.setInstanceProvider(playerEventServiceProvider);
        CrazyDaevaService.setInstanceProvider(crazyDaevaServiceProvider);
        AbyssRankUpdateService.setInstanceProvider(abyssRankUpdateServiceProvider);
        PacketBroadcaster.setInstanceProvider(packetBroadcasterProvider);
    }

    @Override
    public void destroy() {
        EventService.setInstanceProvider(null);
        PlayerEventService.setInstanceProvider(null);
        CrazyDaevaService.setInstanceProvider(null);
        AbyssRankUpdateService.setInstanceProvider(null);
        PacketBroadcaster.setInstanceProvider(null);
    }
}
