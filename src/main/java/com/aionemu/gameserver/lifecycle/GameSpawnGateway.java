package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameSpawnGateway {

    public void spawn() {
        Util.printSection(" *** Spawns *** ");
        SpawnEngine.spawnAll();
    }
}
