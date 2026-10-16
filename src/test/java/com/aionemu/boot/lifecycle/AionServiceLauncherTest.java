package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AionServiceLauncherTest {

    @AfterEach
    void clearEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedFailureHandler.clear();
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void startsEnabledServicesInPhaseOrderAndStopsStartedServicesInReverseOrder() throws Exception {
        assertFalse(AionRuntimeMode.isBootEmbedded());

        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        RecordingTransportBoundary transportBoundary = new RecordingTransportBoundary(events);
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            transportBoundary,
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments("--example=true"));
        launcher.destroy();

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
        assertTrue(AionRuntimeMode.isBootEmbedded());
    }

    @Test
    void startsChatBetweenLoginAndGameWhenChatIsEnabled() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        properties.getChat().setEnabled(true);
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, true, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());

        assertEquals(List.of("prepare", "start:login", "start:chat", "start:game"), events);
    }

    @Test
    void stopsAServiceWhoseStartupFailed() {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new RecordingTransportBoundary(events),
            List.of(new FailingLifecycle(events))
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> launcher.run(new DefaultApplicationArguments())
        );
        assertTrue(thrown.getMessage().contains("boom"));

        assertEquals(List.of("prepare", "start:failing", "stop:failing", "stop:transport"), events);
    }

    @Test
    void stopsTransportBoundaryWhenTransportPreparationFailed() {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new FailingTransportBoundary(events),
            List.of(new RecordingLifecycle("login", 100, true, events))
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> launcher.run(new DefaultApplicationArguments())
        );
        assertTrue(thrown.getMessage().contains("transport failed"));

        assertEquals(List.of("prepare", "stop:transport"), events);
    }

    @Test
    void embeddedFailureStopsStartedServicesInReverseOrder() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        AionEmbeddedFailureHandler.fail(new IllegalStateException("auth failed"));

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    @Test
    void embeddedShutdownRequestStopsStartedServicesInReverseOrder() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("chat", 200, false, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        assertTrue(AionEmbeddedShutdownHandler.requestShutdown());

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    @Test
    void embeddedRestartRequestStopsServicesAndUsesRestartExitCode() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AtomicInteger exitCode = new AtomicInteger();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("login", 100, true, events)
            ),
            exitCode::set
        );

        launcher.run(new DefaultApplicationArguments());
        assertTrue(AionEmbeddedShutdownHandler.requestShutdown(AionEmbeddedShutdownMode.RESTART));

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
        assertEquals(ExitCode.CODE_RESTART, exitCode.get());
    }

    @Test
    void destroyStopsServicesAndTransportOnlyOnceWhenCalledRepeatedly() throws Exception {
        AionServicesProperties properties = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            properties,
            new RecordingTransportBoundary(events),
            List.of(
                new RecordingLifecycle("game", 300, true, events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        launcher.destroy();
        launcher.destroy();

        assertEquals(List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    /**
     * 停服要写库，必须早于容器销毁 Bean（数据源在销毁阶段已经关闭）。
     * Service shutdown writes to the database and must happen before the container destroys its beans (the
     * DataSource is already closed during destruction).
     */
    @Test
    void servicesStopBeforeTheContainerDestroysItsBeans() throws Exception {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new RecordingTransportBoundary(events),
            List.of(new RecordingLifecycle("login", 100, true, events))
        );

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        // 连接池后注册：destroyBeans 逆序销毁时会先关掉它，这正是停服写库失败的原因。
        // The pool is registered second, so destroyBeans closes it first — the reason the shutdown writes failed.
        context.registerBean("serviceLauncher", AionServiceLauncher.class, () -> launcher);
        context.registerBean("pool", RecordingPool.class, () -> new RecordingPool(events));
        context.refresh();

        launcher.run(new DefaultApplicationArguments());
        context.close();

        assertEquals(List.of("prepare", "start:login", "stop:login", "stop:transport", "close:pool"), events);
    }

    @Test
    void stopErrorInOneServiceDoesNotStrandTheRemainingServices() throws Exception {
        List<String> events = new ArrayList<>();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new RecordingTransportBoundary(events),
            List.of(
                new ErrorOnStopLifecycle(events),
                new RecordingLifecycle("login", 100, true, events)
            )
        );

        launcher.run(new DefaultApplicationArguments());
        launcher.destroy();

        assertEquals(
            List.of("prepare", "start:login", "start:game", "stop:game", "stop:login", "stop:transport"), events);
    }

    @Test
    void gracefulShutdownRequestRunsOnlyOnce() {
        AtomicInteger requests = new AtomicInteger();
        AionServiceLauncher launcher = new AionServiceLauncher(
            new AionServicesProperties(),
            new RecordingTransportBoundary(new ArrayList<>()),
            List.of()
        );

        Runnable shutdown = () -> {
            requests.incrementAndGet();
        };
        launcher.requestGracefulShutdown(shutdown);
        launcher.requestGracefulShutdown(shutdown);

        assertEquals(1, requests.get());
    }

    @Test
    void contextCloseCompletesGameShutdownAfterWaitingForPlayers() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/boot/lifecycle/AionServiceLauncher.java"));
        int contextClose = source.indexOf("void onApplicationEvent(ContextClosedEvent event)");
        int gameContext = source.indexOf("ServiceContext.use(\"game\")", contextClose);
        int waitForPlayers = source.indexOf("GameShutdownRequest.waitForPlayersToLeave", contextClose);
        int completeShutdown = source.indexOf("GameShutdownRequest.completeShutdown", contextClose);

        assertTrue(gameContext > contextClose && waitForPlayers > gameContext && completeShutdown > waitForPlayers);
    }

    /** 模拟容器托管的连接池：容器销毁时关闭。 / Stands in for the container-managed pool, closed at destruction. */
    private static final class RecordingPool implements DisposableBean {
        private final List<String> events;

        private RecordingPool(List<String> events) {
            this.events = events;
        }

        @Override
        public void destroy() {
            events.add("close:pool");
        }
    }

    private static final class RecordingTransportBoundary extends AionTransportBoundary {
        private final List<String> events;

        private RecordingTransportBoundary(List<String> events) {
            super(new AionServicesProperties(), List.of());
            this.events = events;
        }

        @Override
        public void prepare() {
            events.add("prepare");
        }

        @Override
        public void destroy() {
            events.add("stop:transport");
        }
    }

    private static final class FailingTransportBoundary extends AionTransportBoundary {
        private final List<String> events;

        private FailingTransportBoundary(List<String> events) {
            super(new AionServicesProperties(), List.of());
            this.events = events;
        }

        @Override
        public void prepare() {
            events.add("prepare");
            throw new IllegalStateException("transport failed");
        }

        @Override
        public void destroy() {
            events.add("stop:transport");
        }
    }

    private static class RecordingLifecycle implements AionServiceLifecycle {
        private final String name;
        private final int phase;
        private final boolean enabled;
        private final List<String> events;

        private RecordingLifecycle(String name, int phase, boolean enabled, List<String> events) {
            this.name = name;
            this.phase = phase;
            this.enabled = enabled;
            this.events = events;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getPhase() {
            return phase;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void start(ApplicationArguments args) {
            events.add("start:" + name);
        }

        @Override
        public void stop() {
            events.add("stop:" + name);
        }
    }

    private static final class FailingLifecycle extends RecordingLifecycle {
        private FailingLifecycle(List<String> events) {
            super("failing", 100, true, events);
        }

        @Override
        public void start(ApplicationArguments args) {
            super.start(args);
            throw new IllegalStateException("boom");
        }
    }

    /** 关服期才首次加载的类缺失：单点 Error 不得连累其余服务与传输边界的收尾。 */
    private static final class ErrorOnStopLifecycle extends RecordingLifecycle {
        private ErrorOnStopLifecycle(List<String> events) {
            super("game", 300, true, events);
        }

        @Override
        public void stop() {
            super.stop();
            throw new NoClassDefFoundError("com/aionemu/OnlyLoadedAtShutdown");
        }
    }
}
