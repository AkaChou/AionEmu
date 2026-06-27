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

        assertTrue(servicesSource.contains("CronService.shutdownCurrentIfInitialized()"));
        assertFalse(servicesSource.contains("CronService.getInstance().shutdown()"));
    }
}
