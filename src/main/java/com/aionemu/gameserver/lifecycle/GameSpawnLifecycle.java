package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

/**
 * 刷怪/生成生命周期：打印 spawns 分区并调用 {@link SpawnEngine#spawnAll()}，加载状态由基类承载。
 * Spawn lifecycle: prints the spawns section and invokes {@link SpawnEngine#spawnAll()};
 * load state is hosted by the base class.
 */
@Component
public class GameSpawnLifecycle extends GameStartupStage {

    /**
     * 启动本阶段：打印刷怪分区并生成全部刷怪点。
     * Start this stage: print the spawns section and spawn all entities.
     */
    @Override
    protected void runStage() {
        Util.printSection(I18n.get("console.section.spawns"));
        SpawnEngine.spawnAll();
    }
}
