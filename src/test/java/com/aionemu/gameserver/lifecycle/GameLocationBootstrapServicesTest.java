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
        Map<String, Path> serviceSources = Map.ofEntries(
            Map.entry("VortexService", Path.of("services/VortexService.java")),
            Map.entry("BeritraService", Path.of("services/BeritraService.java")),
            Map.entry("AgentService", Path.of("services/AgentService.java")),
            Map.entry("AnohaService", Path.of("services/AnohaService.java")),
            Map.entry("SvsService", Path.of("services/SvsService.java")),
            Map.entry("RvrService", Path.of("services/RvrService.java")),
            Map.entry("IuService", Path.of("services/IuService.java")),
            Map.entry("NightmareCircusService", Path.of("services/NightmareCircusService.java")),
            Map.entry("DynamicRiftService", Path.of("services/DynamicRiftService.java")),
            Map.entry("InstanceRiftService", Path.of("services/InstanceRiftService.java")),
            Map.entry("OutpostService", Path.of("services/OutpostService.java")),
            Map.entry("ZorshivDredgionService", Path.of("services/ZorshivDredgionService.java")),
            Map.entry("MoltenusService", Path.of("services/MoltenusService.java")),
            Map.entry("RiftService", Path.of("services/RiftService.java")),
            Map.entry("ConquestService", Path.of("services/ConquestService.java")),
            Map.entry("IdianDepthsService", Path.of("services/IdianDepthsService.java")),
            Map.entry("AbyssLandingSpecialService", Path.of("services/AbyssLandingSpecialService.java")));

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
