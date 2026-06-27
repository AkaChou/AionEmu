package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.configs.Config;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestartServiceTest {

    private String restartFrequency;
    private String restartTime;

    @BeforeEach
    void forceRestartDisabled() {
        restartFrequency = Config.CHATSERVER_RESTART_FREQUENCY;
        restartTime = Config.CHATSERVER_RESTART_TIME;
        Config.CHATSERVER_RESTART_FREQUENCY = "NEVER";
        Config.CHATSERVER_RESTART_TIME = "5:00";
    }

    @AfterEach
    void restoreConfig() {
        Config.CHATSERVER_RESTART_FREQUENCY = restartFrequency;
        Config.CHATSERVER_RESTART_TIME = restartTime;
    }

    @Test
    void canBeConstructedAsSpringBeanWithoutUsingLegacySingleton() throws Exception {
        RestartService restartService = new RestartService();

        assertTrue(Modifier.isPublic(RestartService.class.getDeclaredConstructor().getModifiers()));
        assertTrue(RestartService.class.getMethod("getInstance").isAnnotationPresent(Deprecated.class));

        restartService.shutdown();
    }
}
