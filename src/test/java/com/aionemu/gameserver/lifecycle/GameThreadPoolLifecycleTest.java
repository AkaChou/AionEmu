package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

class GameThreadPoolLifecycleTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void threadPoolSingletonAccessorUsesSpringProvider() {
        ThreadPoolManager threadPoolManager = objenesis.newInstance(ThreadPoolManager.class);

        try {
            ThreadPoolManager.setInstanceProvider(provider(ThreadPoolManager.class, threadPoolManager));

            assertSame(threadPoolManager, ThreadPoolManager.getInstance());
        } finally {
            ThreadPoolManager.setInstanceProvider(null);
        }
    }

    @Test
    void threadPoolManagerFailsFastOutsideSpringContainerInsteadOfLocalFallback() {
        ThreadPoolManager.setInstanceProvider(null);

        assertThrows(IllegalStateException.class, ThreadPoolManager::getInstance);
    }

    @Test
    void threadPoolServiceBindingsWireSpringManagerIntoFailFastAccessor() {
        ThreadPoolServiceBindings bindings = new ThreadPoolServiceBindings(prototypeProvider(ThreadPoolManager.class));

        try {
            assertSame(ThreadPoolManager.getInstance(), ThreadPoolManager.getInstance());
        } finally {
            bindings.destroy();
        }
    }

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new RecordingGameThreadPoolLifecycle(starts, stops);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isStarted());
        assertEquals(1, starts.get());

        lifecycle.stop();
        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(1, stops.get());
    }

    @Test
    void stopBeforeStartDoesNotRunShutdown() {
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new RecordingGameThreadPoolLifecycle(new AtomicInteger(), stops);

        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(0, stops.get());
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private <T> ObjectProvider<T> prototypeProvider(Class<T> type) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        RootBeanDefinition definition = new RootBeanDefinition(type);
        definition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        definition.setInstanceSupplier(() -> objenesis.newInstance(type));
        beanFactory.registerBeanDefinition(type.getName(), definition);
        return beanFactory.getBeanProvider(type);
    }

    private static final class RecordingGameThreadPoolLifecycle extends GameThreadPoolLifecycle {

        private final AtomicInteger starts;
        private final AtomicInteger stops;

        private RecordingGameThreadPoolLifecycle(AtomicInteger starts, AtomicInteger stops) {
            this.starts = starts;
            this.stops = stops;
        }

        @Override
        protected void startThreadPool() {
            starts.incrementAndGet();
        }

        @Override
        protected void stopThreadPool() {
            stops.incrementAndGet();
        }
    }
}
