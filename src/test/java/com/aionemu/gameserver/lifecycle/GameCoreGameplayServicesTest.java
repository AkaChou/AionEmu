package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCoreGameplayServicesTest {

    @Test
    void gameServerCodeUsesCoreLegionBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/LegionService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCoreGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("LegionService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesCoreMailAndBattlefieldUnionBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/mail/MailService.java")))
                .filter(path -> !path.endsWith(Path.of("services/siegeservice/BattlefieldUnionService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCoreGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.matches("(?s).*\\bMailService\\.getInstance\\(\\).*"), source.toString());
            assertFalse(content.matches("(?s).*\\bBattlefieldUnionService\\.getInstance\\(\\).*"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesCoreAutoGroupBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/AutoGroupService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCoreGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("AutoGroupService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesCoreDropBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/drop/DropService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameCoreGameplayServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("DropService.getInstance()"), source.toString());
        }
    }
}
