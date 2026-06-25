package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.utils.ThreadPoolManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginThreadPoolServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        LoginThreadPoolServices services = new LoginThreadPoolServices(
            provider(ThreadPoolManager.class, threadPoolManager)
        );

        try {
            assertSame(threadPoolManager, LoginThreadPoolServices.threadPoolManager());
        } finally {
            services.destroy();
        }
    }

    @Test
    void threadPoolBridgeUsesLocalFallbackInsteadOfDirectLegacySingleton() throws IOException {
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginThreadPoolServices.java"));
        String managerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/ThreadPoolManager.java"));

        assertFalse(servicesSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(servicesSource.contains("fallbackThreadPoolManager()"));
        assertTrue(servicesSource.contains("new ThreadPoolManager()"));
        assertTrue(managerSource.contains("@Deprecated(since = \"boot-migration\")"));
    }

    @Test
    void gameServerConnectionCodeUsesThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String connectionSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/GsConnection.java"));
        String authPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_GS_AUTH.java"));

        assertFalse(connectionSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(connectionSource.contains("LoginThreadPoolServices.threadPoolManager().executeLsPacket"));
        assertTrue(connectionSource.contains("LoginThreadPoolServices.threadPoolManager().schedule"));

        assertFalse(authPacketSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(authPacketSource.contains("LoginThreadPoolServices.threadPoolManager().schedule"));
    }

    @Test
    void startupAndTransferSchedulingUseThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String startupBridgeSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));
        String transferServiceSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/PlayerTransferService.java"));

        assertFalse(startupBridgeSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(startupBridgeSource.contains("LoginThreadPoolServices.threadPoolManager()"));

        assertFalse(transferServiceSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(transferServiceSource.contains("LoginThreadPoolServices.threadPoolManager().scheduleAtFixedRate"));
    }

    @Test
    void cronAndTaskTriggersUseThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String cronRunnerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/cron/ThreadPoolManagerRunnableRunner.java"));
        String afterRestartTriggerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/taskmanager/trigger/implementations/AfterRestartTrigger.java"));
        String fixedInTimeTriggerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/taskmanager/trigger/implementations/FixedInTimeTrigger.java"));

        assertFalse(cronRunnerSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(cronRunnerSource.contains("LoginThreadPoolServices.threadPoolManager().execute(r)"));
        assertTrue(cronRunnerSource.contains("LoginThreadPoolServices.threadPoolManager().executeLongRunning(r)"));

        assertFalse(afterRestartTriggerSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(afterRestartTriggerSource.contains("LoginThreadPoolServices.threadPoolManager().schedule(this, 5000)"));

        assertFalse(fixedInTimeTriggerSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(fixedInTimeTriggerSource.contains("LoginThreadPoolServices.threadPoolManager().scheduleAtFixedRate"));
    }

    @Test
    void shutdownUsesThreadPoolBridgeForLoginThreadPoolCleanup() throws IOException {
        String shutdownSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/Shutdown.java"));

        assertFalse(shutdownSource.contains("import com.aionemu.loginserver.utils.ThreadPoolManager;"));
        assertFalse(shutdownSource.contains("            ThreadPoolManager.getInstance().shutdown();"));
        assertTrue(shutdownSource.contains("LoginThreadPoolServices.threadPoolManager().shutdown()"));
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
