package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WeddingsConfig;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameRewardServicesLifecycle {

    private final BooleanSupplier rewardEnabled;
    private final Runnable rewardInitializer;
    private final BooleanSupplier weddingsEnabled;
    private final Runnable weddingInitializer;
    private final BooleanSupplier veteranRewardsEnabled;
    private final Runnable veteranRewardsInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameRewardServicesLifecycle() {
        this(
            () -> CustomConfig.ENABLE_REWARD_SERVICE,
            RewardService::getInstance,
            () -> WeddingsConfig.WEDDINGS_ENABLE,
            WeddingService::getInstance,
            () -> VeteranRewardConfig.VETERANREWARDS_ENABLED,
            VeteranRewardsService::getInstance
        );
    }

    GameRewardServicesLifecycle(
        BooleanSupplier rewardEnabled,
        Runnable rewardInitializer,
        BooleanSupplier weddingsEnabled,
        Runnable weddingInitializer,
        BooleanSupplier veteranRewardsEnabled,
        Runnable veteranRewardsInitializer
    ) {
        this.rewardEnabled = rewardEnabled;
        this.rewardInitializer = rewardInitializer;
        this.weddingsEnabled = weddingsEnabled;
        this.weddingInitializer = weddingInitializer;
        this.veteranRewardsEnabled = veteranRewardsEnabled;
        this.veteranRewardsInitializer = veteranRewardsInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (rewardEnabled.getAsBoolean()) {
                rewardInitializer.run();
            }
            if (weddingsEnabled.getAsBoolean()) {
                weddingInitializer.run();
            }
            if (veteranRewardsEnabled.getAsBoolean()) {
                veteranRewardsInitializer.run();
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
