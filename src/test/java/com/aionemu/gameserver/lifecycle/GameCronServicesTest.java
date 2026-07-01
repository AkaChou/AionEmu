package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GameCronServicesTest {

    @Test
    void shutdownFallbackUsesCronServiceHelperInsteadOfFetchingSingleton() throws IOException {
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameCronServices.java"));

        assertTrue(servicesSource.contains("resolvedCronService = CronService.initSingleton(ThreadPoolManagerRunnableRunner.class)"));
        assertFalse(servicesSource.contains("resolvedCronService = CronService.getInstance()"));
        assertTrue(servicesSource.contains("CronService.shutdownCurrentIfInitialized()"));
        assertFalse(servicesSource.contains("CronService.getInstance().shutdown()"));
    }

    @Test
    void cronServiceAccessorUsesResolvedServiceBeforeRequireCurrentFallback() throws IOException {
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameCronServices.java"));

        assertTrue(servicesSource.contains("public static CronService cronService()"));
        assertTrue(servicesSource.contains("return CronService.requireCurrent()"));
        assertFalse(servicesSource.contains("return CronService.getInstance()"));
    }
}
