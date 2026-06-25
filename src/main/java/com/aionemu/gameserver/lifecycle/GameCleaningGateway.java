package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameCleaningGateway {

    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;

    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }

    public void clean() {
        databaseCleaningService();
        abyssRankCleaningService();
    }

    private DatabaseCleaningService databaseCleaningService() {
        if (databaseCleaningServiceProvider == null) {
            return DatabaseCleaningService.getInstance();
        }
        return databaseCleaningServiceProvider.getIfAvailable(DatabaseCleaningService::getInstance);
    }

    private AbyssRankCleaningService abyssRankCleaningService() {
        if (abyssRankCleaningServiceProvider == null) {
            return AbyssRankCleaningService.getInstance();
        }
        return abyssRankCleaningServiceProvider.getIfAvailable(AbyssRankCleaningService::getInstance);
    }
}
