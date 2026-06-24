package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import org.springframework.stereotype.Component;

@Component
public class GameCleaningGateway {

    public void clean() {
        DatabaseCleaningService.getInstance();
        AbyssRankCleaningService.getInstance();
    }
}
