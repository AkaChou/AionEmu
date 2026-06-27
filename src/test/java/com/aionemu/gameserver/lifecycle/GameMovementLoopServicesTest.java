package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameMovementLoopServicesTest {

    @Test
    void gameServerCodeUsesMovementLoopBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/MovementNotifyTask.java")))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/MoveTaskManager.java")))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/PlayerMoveTaskManager.java")))
                .filter(path -> !path.endsWith(Path.of("world/zone/ZoneUpdateService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMovementLoopServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMovementLoopGateway.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("MovementNotifyTask.getInstance()"), source.toString());
            assertFalse(content.contains("MoveTaskManager.getInstance()"), source.toString());
            assertFalse(content.contains("PlayerMoveTaskManager.getInstance()"), source.toString());
            assertFalse(content.contains("ZoneUpdateService.getInstance()"), source.toString());
        }
    }
}
