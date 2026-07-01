package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRuntimeServicesTest {

    @Test
    void gameServerCodeUsesRuntimeBridgeInsteadOfDirectRuntimeSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/BrokerService.java")))
                .filter(path -> !path.endsWith(Path.of("model/siege/Influence.java")))
                .filter(path -> !path.endsWith(Path.of("services/ExchangeService.java")))
                .filter(path -> !path.endsWith(Path.of("services/PetitionService.java")))
                .filter(path -> !path.endsWith(Path.of("services/AdminService.java")))
                .filter(path -> !path.endsWith(Path.of("services/transfers/PlayerTransferService.java")))
                .filter(path -> !path.endsWith(Path.of("services/territory/TerritoryService.java")))
                .filter(path -> !path.endsWith(Path.of("services/WeatherService.java")))
                .filter(path -> !path.endsWith(Path.of("services/LimitedItemTradeService.java")))
                .filter(path -> !path.endsWith(Path.of("services/SurveyService.java")))
                .filter(path -> !path.endsWith(Path.of("utils/audit/GMService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameRuntimeServices.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("BrokerService.getInstance()"), source.toString());
            assertFalse(content.contains("Influence.getInstance()"), source.toString());
            assertFalse(content.contains("ExchangeService.getInstance()"), source.toString());
            assertFalse(content.contains("PetitionService.getInstance()"), source.toString());
            assertFalse(content.contains("AdminService.getInstance()"), source.toString());
            assertFalse(content.contains("PlayerTransferService.getInstance()"), source.toString());
            assertFalse(content.contains("TerritoryService.getInstance()"), source.toString());
            assertFalse(content.contains("WeatherService.getInstance()"), source.toString());
            assertFalse(content.contains("LimitedItemTradeService.getInstance()"), source.toString());
            assertFalse(content.contains("SurveyService.getInstance()"), source.toString());
            assertFalse(content.contains("BoostEventService.getInstance()"), source.toString());
            assertFalse(content.contains("GMService.getInstance()"), source.toString());
        }
    }
}
