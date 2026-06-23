package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import org.springframework.stereotype.Component;

@Component
public class GameOptionalServicesGateway {

    public void start() {
        if (CustomConfig.LIMITS_ENABLED) {
            PlayerLimitService.getInstance().scheduleUpdate();
        }
        if (AIConfig.SHOUTS_ENABLE) {
            NpcShoutsService.getInstance();
        }
        if (SiegeConfig.SIEGE_SHIELD_ENABLED) {
            ShieldService.getInstance().spawnAll();
        }
    }
}
