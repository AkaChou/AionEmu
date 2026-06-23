package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameBattlefieldLifecycle {

    private final BooleanSupplier autoGroupEnabled;
    private final List<Runnable> initializers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameBattlefieldLifecycle() {
        this(
            () -> AutoGroupConfig.AUTO_GROUP_ENABLED,
            List.of(
                () -> KamarBattlefieldService.getInstance().initKamarBattlefield(),
                () -> EngulfedOphidanBridgeService.getInstance().initEngulfedOphidan(),
                () -> SuspiciousOphidanBridgeService.getInstance().initSuspiciousOphidan(),
                () -> IronWallWarfrontService.getInstance().initIronWallWarfront(),
                () -> IdgelDomeService.getInstance().initIdgelDome(),
                () -> IdgelDomeLandmarkService.getInstance().initLandmark(),
                () -> HallOfTenacityService.getInstance().initHallOfTenacity(),
                () -> GrandArenaTrainingCampService.getInstance().initGrandArenaTrainingCamp(),
                () -> IDRunService.getInstance().initIDRun()
            )
        );
    }

    GameBattlefieldLifecycle(BooleanSupplier autoGroupEnabled, List<Runnable> initializers) {
        this.autoGroupEnabled = autoGroupEnabled;
        this.initializers = List.copyOf(initializers);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            for (Runnable initializer : initializers) {
                if (autoGroupEnabled.getAsBoolean()) {
                    initializer.run();
                }
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
