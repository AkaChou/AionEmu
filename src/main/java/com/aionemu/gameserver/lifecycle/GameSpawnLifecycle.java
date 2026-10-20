package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 刷怪/生成生命周期：打印 spawns 分区、调用 {@link SpawnEngine#spawnAll()}，并在刷怪完成后
 * 延迟预热地理碰撞树。
 * Spawn lifecycle: prints the spawns section, invokes {@link SpawnEngine#spawnAll()}, and
 * schedules geo collision warm-up after spawning; load state is hosted by the base class.
 */
@Component
@Slf4j
public class GameSpawnLifecycle extends GameStartupStage {

    /**
     * 地理服务提供者。
     * Geo-service provider.
     */
    private ObjectProvider<GeoService> geoServiceProvider;

    /**
     * 可选注入地理服务提供者。
     * Optionally inject the geo-service provider.
     * @param geoServiceProvider 地理服务提供者 / Geo-service provider
     */
    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    /**
     * 启动本阶段：打印刷怪分区、生成全部刷怪点，最后安排碰撞树延迟预热。
     * Start this stage: print the spawns section, spawn all entities, and schedule delayed collision warm-up.
     */
    @Override
    protected void runStage() {
        printSection();
        spawnAll();
        scheduleCollisionWarmUp();
    }

    /**
     * 打印刷怪分区标题。
     * Print the spawns section header.
     */
    protected void printSection() {
        Util.printSection(I18n.get("console.section.spawns"));
    }

    /**
     * 生成全部启动刷怪点。
     * Spawn all startup spawns.
     */
    protected void spawnAll() {
        SpawnEngine.spawnAll();
    }

    /**
     * 刷怪完成后延迟到后台预热碰撞树，避免与启动尾段和刷怪任务争抢 CPU。
     * Schedule collision warm-up after spawning so it does not compete with spawn and
     * startup-tail work.
     */
    private void scheduleCollisionWarmUp() {
        if (!GeoDataConfig.GEO_COLLISION_PREBUILD_ENABLE) {
            log.debug(I18n.get("log.a71f3c6e92d4"));
            return;
        }
        GeoService geoService = geoService();
        if (geoService == null) {
            return;
        }
        long delay = Math.max(0, GeoDataConfig.GEO_COLLISION_PREBUILD_DELAY_MS);
        submitDelayed(() -> warmCollisionData(geoService), delay);
    }

    /**
     * 解析地理服务；容器未提供时返回 null。
     * Resolve the geo service, returning null when the container does not provide one.
     * @return 地理服务或 null / geo service or null
     */
    protected GeoService geoService() {
        return geoServiceProvider == null ? null : geoServiceProvider.getIfAvailable();
    }

    /**
     * 触发地理碰撞树预热。
     * Trigger geo collision-tree warm-up.
     * @param geoService 地理服务 / geo service
     */
    protected void warmCollisionData(GeoService geoService) {
        geoService.warmCollisionDataAsync();
    }

    /**
     * 提交延迟任务；独立方法便于测试替换调度器。
     * Submit a delayed task; isolated so tests can replace the scheduler.
     * @param task 待执行任务 / task to execute
     * @param delayMs 延迟毫秒 / delay in milliseconds
     */
    protected void submitDelayed(Runnable task, long delayMs) {
        ThreadPoolManager.getInstance().schedule(task, delayMs);
    }
}
