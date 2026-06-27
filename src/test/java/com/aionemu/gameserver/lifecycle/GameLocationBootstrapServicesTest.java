package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameLocationBootstrapServicesTest {

    @Test
    void gameServerCodeUsesLocationAbyssLandingBridgeInsteadOfDirectSingleton() throws IOException {
        assertNoDirectSingletonAccess("AbyssLandingService", Path.of("services/AbyssLandingService.java"));
    }

    @Test
    void gameServerCodeUsesLocationBootstrapBridgeInsteadOfDirectSingletons() throws IOException {
        Map<String, Path> serviceSources = Map.of(
            "VortexService", Path.of("services/VortexService.java"),
            "BeritraService", Path.of("services/BeritraService.java"),
            "AgentService", Path.of("services/AgentService.java"),
            "AnohaService", Path.of("services/AnohaService.java"),
            "RvrService", Path.of("services/RvrService.java"),
            "ZorshivDredgionService", Path.of("services/ZorshivDredgionService.java"),
            "MoltenusService", Path.of("services/MoltenusService.java"),
            "ConquestService", Path.of("services/ConquestService.java"));

        for (Map.Entry<String, Path> serviceSource : serviceSources.entrySet()) {
            assertNoDirectSingletonAccess(serviceSource.getKey(), serviceSource.getValue());
        }
    }

    private void assertNoDirectSingletonAccess(String serviceName, Path serviceSource) throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(serviceSource))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameLocationBootstrapServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains(serviceName + ".getInstance()"), source.toString());
        }
    }
}
