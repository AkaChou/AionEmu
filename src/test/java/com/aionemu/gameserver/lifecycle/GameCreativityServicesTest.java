package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCreativityServicesTest {

    @Test
    void gameServerCodeUsesCreativityBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCreativityServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("CreativityEssenceService.getInstance()"), source.toString());
            assertFalse(content.contains("CreativitySkillService.getInstance()"), source.toString());
            assertFalse(content.contains("CreativityStatsService.getInstance()"), source.toString());
            assertFalse(content.contains("CreativityTransfoService.getInstance()"), source.toString());
            assertFalse(content.contains("Accuracy.getInstance()"), source.toString());
            assertFalse(content.contains("Agility.getInstance()"), source.toString());
            assertFalse(content.contains("Health.getInstance()"), source.toString());
            assertFalse(content.contains("Knowledge.getInstance()"), source.toString());
            assertFalse(content.contains("Power.getInstance()"), source.toString());
            assertFalse(content.contains("Precision.getInstance()"), source.toString());
            assertFalse(content.contains("Will.getInstance()"), source.toString());
        }
    }
}
