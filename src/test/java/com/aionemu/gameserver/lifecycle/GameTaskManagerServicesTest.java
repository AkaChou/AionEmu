package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameTaskManagerServicesTest {

    @Test
    void gameServerCodeUsesExpireTimerTaskBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/ExpireTimerTask.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameTaskManagerServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("ExpireTimerTask.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesTaskManagerBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/TeamEffectUpdater.java")))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/TeamMoveUpdater.java")))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/TemporaryTradeTimeTask.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameTaskManagerServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("TeamEffectUpdater.getInstance()"), source.toString());
            assertFalse(content.contains("TeamMoveUpdater.getInstance()"), source.toString());
            assertFalse(content.contains("TemporaryTradeTimeTask.getInstance()"), source.toString());
        }
    }
}
