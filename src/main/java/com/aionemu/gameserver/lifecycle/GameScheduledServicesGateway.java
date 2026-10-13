package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时服务启动网关：按配置调度小猪波比、欧比斯活动与术古皇陵刷怪。
 * Gateway that starts scheduled services: Pig/Poppy, Abyss events, and Imperial Tomb spawns by config.
 */
@Component
public class GameScheduledServicesGateway {

    /**
     * 术古皇陵刷怪管理器提供者（可选）。
     * Optional provider for the Shugo Imperial Tomb spawn manager.
     */
    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;


    /**
     * 注入术古皇陵刷怪管理器提供者。
     * Inject the Shugo Imperial Tomb spawn manager provider.
     * @param shugoImperialTombSpawnManagerProvider 刷怪管理器提供者 / Spawn manager provider
     */
    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }


    /**
     * 打印定时服务分区并按开关调度相关活动。
     * Print the scheduled-services section and schedule enabled events.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.scheduled_services"));
        if (EventsConfig.ENABLE_PIG_POPPY_EVENT) {
            PigPoppyEventService.ScheduleCron();
        }
        if (EventsConfig.ENABLE_ABYSS_EVENT) {
            TreasureAbyssService.ScheduleCron();
        }
        if (EventsConfig.IMPERIAL_TOMB_ENABLE) {
            shugoImperialTombSpawnManager().start();
        }
    }

    /**
     * 解析术古皇陵刷怪管理器：优先 Spring 提供，否则回退桥接。
     * Resolve the Imperial Tomb spawn manager.
     * @return 刷怪管理器 / Spawn manager
     */
    private ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return shugoImperialTombSpawnManagerProvider.getObject();
    }

}
