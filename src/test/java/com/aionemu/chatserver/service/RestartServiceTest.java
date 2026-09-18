package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.testutil.ConfigSnapshot;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestartServiceTest {

    private final ConfigSnapshot configSnapshot = ConfigSnapshot.of(Config.class,
        "CHATSERVER_RESTART_FREQUENCY", "CHATSERVER_RESTART_TIME");

    @BeforeEach
    void forceRestartDisabled() {
        Config.CHATSERVER_RESTART_FREQUENCY = "NEVER";
        Config.CHATSERVER_RESTART_TIME = "5:00";
    }

    @AfterEach
    void restoreConfig() {
        configSnapshot.restore();
    }

    @Test
    void canBeConstructedAsSpringBeanWithoutUsingLegacySingleton() throws Exception {
        RestartService restartService = new RestartService();

        assertTrue(Modifier.isPublic(RestartService.class.getDeclaredConstructor().getModifiers()));
        assertTrue(RestartService.class.getMethod("getInstance").isAnnotationPresent(Deprecated.class));

        restartService.shutdown();
    }

    @Test
    void missingRestartConfigDefaultsToDisabledRestartInsteadOfFailingStartup() {
        Config.CHATSERVER_RESTART_FREQUENCY = null;
        Config.CHATSERVER_RESTART_TIME = null;

        RestartService restartService = assertDoesNotThrow(RestartService::new);

        restartService.shutdown();
    }
}
