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
}
