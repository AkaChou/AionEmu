package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameServiceSelfSingletonCleanupTest {

    @Test
    void servicesDoNotCallTheirOwnSingletonAccessors() throws IOException {
        Map<String, Path> serviceSources = Map.of(
            "FindGroupService", Path.of("src/main/java/com/aionemu/gameserver/services/FindGroupService.java"),
            "LegionService", Path.of("src/main/java/com/aionemu/gameserver/services/LegionService.java"),
            "RiftService", Path.of("src/main/java/com/aionemu/gameserver/services/RiftService.java"));

        for (Map.Entry<String, Path> serviceSource : serviceSources.entrySet()) {
            String source = Files.readString(serviceSource.getValue());

            assertFalse(source.contains(serviceSource.getKey() + ".getInstance()"), serviceSource.getValue().toString());
        }
    }
}
