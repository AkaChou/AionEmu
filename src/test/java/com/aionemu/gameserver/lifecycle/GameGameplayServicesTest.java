package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameGameplayServicesTest {

    @Test
    void gameServerCodeUsesDuelBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/DuelService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("DuelService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesGameplayBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/LifeStatsRestoreService.java")))
                .filter(path -> !path.endsWith(Path.of("services/ranking/SeasonRankingService.java")))
                .filter(path -> !path.endsWith(Path.of("services/rift/RiftManager.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("LifeStatsRestoreService.getInstance()"), source.toString());
            assertFalse(content.contains("SeasonRankingService.getInstance()"), source.toString());
            assertFalse(content.contains("RiftManager.getInstance()"), source.toString());
        }
    }
}
