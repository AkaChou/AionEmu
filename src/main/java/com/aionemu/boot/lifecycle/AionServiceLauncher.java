package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AionServiceLauncher implements ApplicationRunner, DisposableBean, ApplicationContextAware {

    private final AionServicesProperties services;
    private final AionTransportBoundary transportBoundary;
    private final List<AionServiceLifecycle> serviceLifecycles;
    private final IntConsumer haltAction;
    private final List<AionServiceLifecycle> startedServices = new ArrayList<>();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final Consumer<RuntimeException> embeddedFailureHandler = this::handleEmbeddedFailure;
    private final Consumer<AionEmbeddedShutdownMode> embeddedShutdownHandler = this::handleEmbeddedShutdown;
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    public AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles,
        AionProcessRuntimeBridge runtimeBridge
    ) {
        this(services, transportBoundary, serviceLifecycles, runtimeBridge::halt);
    }

    AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles
    ) {
        this(services, transportBoundary, serviceLifecycles, new AionProcessRuntimeBridge()::halt);
    }

    AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles,
        IntConsumer haltAction
    ) {
        this.services = services;
        this.transportBoundary = transportBoundary;
        this.haltAction = haltAction;
        this.serviceLifecycles = serviceLifecycles.stream()
            .sorted(Comparator.comparingInt(AionServiceLifecycle::getPhase))
            .toList();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AionRuntimeMode.enableBootEmbeddedMode();
        AionEmbeddedFailureHandler.register(embeddedFailureHandler);
        AionEmbeddedShutdownHandler.register(embeddedShutdownHandler);
        String[] sourceArgs = args.getSourceArgs();
        boolean loginEnabled = services.getLogin().isEnabled();
        boolean chatEnabled = services.getChat().isEnabled();
        boolean gameEnabled = services.getGame().isEnabled();

        log.info("Aion service startup: login={}, chat={}, game={}", loginEnabled, chatEnabled, gameEnabled);
        try {
            transportBoundary.prepare();
            if (gameEnabled && !loginEnabled) {
                log.warn("Game service is enabled while login service is disabled; game will still use its configured login-server connector.");
            }
            if (!chatEnabled) {
                log.info("Chat service is disabled by boot configuration; game chat connector will also be disabled.");
            }

            for (AionServiceLifecycle serviceLifecycle : serviceLifecycles) {
                if (serviceLifecycle.isEnabled()) {
                    startService(serviceLifecycle, args);
                } else {
                    log.info("{} service is disabled by boot configuration.", serviceLifecycle.getName());
                }
            }
        } catch (Exception | Error e) {
            log.error("Aion service startup failed; stopping services.", e);
            destroy();
            throw e;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext) {
            this.applicationContext = configurableApplicationContext;
        }
    }

    private void startService(AionServiceLifecycle serviceLifecycle, ApplicationArguments args) throws Exception {
        String name = serviceLifecycle.getName();
        log.info("Starting {} service...", name);
        startedServices.add(serviceLifecycle);
        try (ServiceContext.Scope ignored = ServiceContext.use(name)) {
            serviceLifecycle.start(args);
        }
        log.info("{} service startup returned.", name);
    }

    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        AionEmbeddedFailureHandler.clear(embeddedFailureHandler);
        AionEmbeddedShutdownHandler.clear(embeddedShutdownHandler);
        ListIterator<AionServiceLifecycle> iterator = startedServices.listIterator(startedServices.size());
        while (iterator.hasPrevious()) {
            AionServiceLifecycle serviceLifecycle = iterator.previous();
            stopService(serviceLifecycle);
            iterator.remove();
        }
        stopTransportBoundary();
    }

    private void stopService(AionServiceLifecycle serviceLifecycle) {
        String name = serviceLifecycle.getName();
        log.info("Stopping {} service...", name);
        try (ServiceContext.Scope ignored = ServiceContext.use(name)) {
            serviceLifecycle.stop();
        } catch (Exception e) {
            log.warn("Failed to stop {} service cleanly.", name, e);
        }
    }

    private void stopTransportBoundary() {
        try {
            transportBoundary.destroy();
        } catch (Exception e) {
            log.warn("Failed to stop transport boundary cleanly.", e);
        }
    }

    private void handleEmbeddedFailure(RuntimeException failure) {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }
        log.error("Aion embedded service failure detected; stopping services.", failure);
        stopServicesAndCloseContext();
    }

    private void handleEmbeddedShutdown(AionEmbeddedShutdownMode mode) {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }
        log.info("Aion embedded {} requested; stopping services.", mode.name().toLowerCase());
        stopServicesAndCloseContext();
        if (mode == AionEmbeddedShutdownMode.RESTART) {
            haltAction.accept(mode.exitCode());
        }
    }

    private void stopServicesAndCloseContext() {
        destroy();
        if (applicationContext != null && applicationContext.isActive()) {
            applicationContext.close();
        }
    }
}
