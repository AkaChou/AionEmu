package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GameCraftServicesTest {

    @Test
    void gameServerCodeUsesCraftBridgeInsteadOfDirectSingletons() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/craft/CraftSkillUpdateService.java")))
                .filter(path -> !path.endsWith(Path.of("services/craft/RelinquishCraftStatus.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCraftServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("CraftSkillUpdateService.getInstance()"), source.toString());
                assertFalse(content.contains("RelinquishCraftStatus.getInstance()"), source.toString());
            }
        }
    }
}
