package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameLocationBootstrapServicesTest {

    @Test
    void gameServerCodeUsesLocationAbyssLandingBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/AbyssLandingService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameLocationBootstrapServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("AbyssLandingService.getInstance()"), source.toString());
        }
    }
}
