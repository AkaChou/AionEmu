package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameWorldServicesTest {

    @Test
    void gameServerCodeUsesWorldGeoBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("world/geo/GeoService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldServiceFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameWorldServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("GeoService.getInstance()"), source.toString());
        }
    }
}
