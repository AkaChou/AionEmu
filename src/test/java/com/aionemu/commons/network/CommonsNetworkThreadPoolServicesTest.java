package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.util.ThreadPoolManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CommonsNetworkThreadPoolServicesTest {

    @Test
    void exposesCommonsNetworkThreadPoolManagerAsLazySpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CommonsNetworkSpringConfiguration.class)) {
            assertTrue(context.containsBeanDefinition("commonsNetworkThreadPoolManager"));
            assertEquals(ThreadPoolManager.class, context.getType("commonsNetworkThreadPoolManager"));
            assertTrue(context.getBeanFactory().getBeanDefinition("commonsNetworkThreadPoolManager").isLazyInit());
        }
    }

    @Test
    void springBeanCreatesBootManagedThreadPoolInsteadOfLegacySingleton() throws IOException {
        String configurationSource = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/CommonsNetworkSpringConfiguration.java"));
        String threadPoolSource = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/util/ThreadPoolManager.java"));

        assertFalse(configurationSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(configurationSource.contains("return new ThreadPoolManager();"));
        assertTrue(threadPoolSource.contains("public ThreadPoolManager()"));
    }

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        CommonsNetworkThreadPoolServices services = new CommonsNetworkThreadPoolServices(
            provider(ThreadPoolManager.class, threadPoolManager)
        );

        try {
            assertSame(threadPoolManager, CommonsNetworkThreadPoolServices.threadPoolManager());
        } finally {
            services.destroy();
        }
    }

    @Test
    void cachesResolvedSpringProviderForShutdownAfterDestroy() throws IOException {
        String servicesSource = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/CommonsNetworkThreadPoolServices.java"));

        assertTrue(servicesSource.contains("resolvedThreadPoolManager"));
        assertTrue(servicesSource.contains("return resolved;"));
        assertFalse(servicesSource.contains("return ThreadPoolManager.getInstance();"));
        assertFalse(servicesSource.contains("provider.getIfAvailable(ThreadPoolManager::getInstance)"));
    }

    @Test
    void nettyAndShutdownUseThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String handlerSource = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/NettyConnectionHandler.java"));
        String serverSource = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/NettyServer.java"));
        String shutdownSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/Shutdown.java"));

        assertFalse(handlerSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(handlerSource.contains("CommonsNetworkThreadPoolServices.threadPoolManager()"));

        assertFalse(serverSource.contains("new NettyConnectionHandler(cfg.factory())"));
        assertTrue(serverSource.contains("new NettyConnectionHandler(cfg.factory(), connectionExecutor.get())"));

        assertFalse(shutdownSource.contains("com.aionemu.commons.network.util.ThreadPoolManager.getInstance().shutdown()"));
        assertTrue(shutdownSource.contains("CommonsNetworkThreadPoolServices.threadPoolManager().shutdown()"));
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
