package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginTaskManagerServicesTest {

    @Test
    void usesSpringProviderBeforeLocalFallback() {
        TaskFromDBManager taskFromDBManager = instance(TaskFromDBManager.class);
        LoginTaskManagerServices services = new LoginTaskManagerServices(
            provider(TaskFromDBManager.class, taskFromDBManager)
        );

        try {
            assertSame(taskFromDBManager, LoginTaskManagerServices.taskFromDBManager());
        } finally {
            services.destroy();
        }
    }

    @Test
    void taskManagerBridgeUsesLocalFallbackInsteadOfDirectLegacySingleton() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginTaskManagerServices.java"));

        assertFalse(source.contains("TaskFromDBManager.getInstance()"));
        assertTrue(source.contains("Fallbacks.TASK_FROM_DB_MANAGER"));
    }

    @Test
    void startupRuntimeBridgeUsesTaskManagerBridgeInsteadOfDirectSingleton() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));

        assertFalse(source.contains("TaskFromDBManager.getInstance()"));
        assertFalse(source.contains("taskFromDBManagerProvider"));
        assertTrue(source.contains("LoginTaskManagerServices.taskFromDBManager()"));
    }

    private static <T> T instance(Class<T> type) {
        return new ObjenesisStd().newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
