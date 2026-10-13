package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 清理生命周期：触发数据库与欧比斯排名清理服务，加载状态由基类承载。
 * Cleaning lifecycle: triggers database and abyss-rank cleaning services; load state is hosted
 * by the base class.
 */
@Component
public class GameCleaningLifecycle extends GameStartupStage {

    /**
     * 数据库清理服务提供者。
     * Database cleaning service provider.
     */
    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;

    /**
     * 欧比斯排名清理服务提供者。
     * Abyss-rank cleaning service provider.
     */
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;


    /**
     * 可选注入数据库清理服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of database cleaning service.
     * @param databaseCleaningServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    /**
     * 可选注入欧比斯排名清理服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of abyss-rank cleaning service.
     * @param abyssRankCleaningServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }


    /**
     * 启动本阶段：解析并触发数据库与欧比斯排名清理服务。
     * Start this stage: resolve and trigger database and abyss-rank cleaning services.
     */
    @Override
    protected void runStage() {
        databaseCleaningServiceProvider.getObject();
        abyssRankCleaningServiceProvider.getObject();
    }
}
