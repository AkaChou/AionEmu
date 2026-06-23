package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameOptionalServicesLifecycle {

    private final BooleanSupplier limitsEnabled;
    private final Runnable playerLimitInitializer;
    private final BooleanSupplier npcShoutsEnabled;
    private final Runnable npcShoutsInitializer;
    private final BooleanSupplier siegeShieldEnabled;
    private final Runnable shieldInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameOptionalServicesLifecycle() {
        this(
            () -> CustomConfig.LIMITS_ENABLED,
            () -> PlayerLimitService.getInstance().scheduleUpdate(),
            () -> AIConfig.SHOUTS_ENABLE,
            NpcShoutsService::getInstance,
            () -> SiegeConfig.SIEGE_SHIELD_ENABLED,
            () -> ShieldService.getInstance().spawnAll()
        );
    }

    GameOptionalServicesLifecycle(
        BooleanSupplier limitsEnabled,
        Runnable playerLimitInitializer,
        BooleanSupplier npcShoutsEnabled,
        Runnable npcShoutsInitializer,
        BooleanSupplier siegeShieldEnabled,
        Runnable shieldInitializer
    ) {
        this.limitsEnabled = limitsEnabled;
        this.playerLimitInitializer = playerLimitInitializer;
        this.npcShoutsEnabled = npcShoutsEnabled;
        this.npcShoutsInitializer = npcShoutsInitializer;
        this.siegeShieldEnabled = siegeShieldEnabled;
        this.shieldInitializer = shieldInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (limitsEnabled.getAsBoolean()) {
                playerLimitInitializer.run();
            }
            if (npcShoutsEnabled.getAsBoolean()) {
                npcShoutsInitializer.run();
            }
            if (siegeShieldEnabled.getAsBoolean()) {
                shieldInitializer.run();
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
