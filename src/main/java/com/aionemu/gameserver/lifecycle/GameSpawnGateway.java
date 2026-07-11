package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

/**
 * 刷怪/生成网关：打印 spawns 分区并调用 {@link SpawnEngine#spawnAll()}。
 * Spawn gateway: prints the spawns section and invokes {@link SpawnEngine#spawnAll()}.
 */
@Component
public class GameSpawnGateway {

    /**
     * 打印刷怪分区并生成全部刷怪点。
     * Print the spawns section and spawn all entities.
     */
    public void spawn() {
        Util.printSection(I18n.get("console.section.spawns"));
        SpawnEngine.spawnAll();
    }
}
