package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AionServiceLauncher implements ApplicationRunner, DisposableBean, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(AionServiceLauncher.class);

    private final AionServicesProperties services;
    private final AionTransportBoundary transportBoundary;
    private final List<AionServiceLifecycle> serviceLifecycles;
    private final List<AionServiceLifecycle> startedServices = new ArrayList<>();
    private final AtomicBoolean handlingEmbeddedFailure = new AtomicBoolean(false);
    private final Consumer<RuntimeException> embeddedFailureHandler = this::handleEmbeddedFailure;
    private ConfigurableApplicationContext applicationContext;

    public AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles
    ) {
        this.services = services;
        this.transportBoundary = transportBoundary;
        this.serviceLifecycles = serviceLifecycles.stream()
            .sorted(Comparator.comparingInt(AionServiceLifecycle::getPhase))
            .toList();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AionRuntimeMode.enableBootEmbeddedMode();
        AionEmbeddedFailureHandler.register(embeddedFailureHandler);
        String[] sourceArgs = args.getSourceArgs();
        boolean loginEnabled = services.getLogin().isEnabled();
        boolean chatEnabled = services.getChat().isEnabled();
        boolean gameEnabled = services.getGame().isEnabled();

        log.info("Aion service startup: login={}, chat={}, game={}", loginEnabled, chatEnabled, gameEnabled);
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
        try (ServiceContext.Scope ignored = ServiceContext.use(name)) {
            serviceLifecycle.start(args);
        }
        startedServices.add(serviceLifecycle);
        log.info("{} service startup returned.", name);
    }

    @Override
    public void destroy() {
        AionEmbeddedFailureHandler.clear(embeddedFailureHandler);
        ListIterator<AionServiceLifecycle> iterator = startedServices.listIterator(startedServices.size());
        while (iterator.hasPrevious()) {
            AionServiceLifecycle serviceLifecycle = iterator.previous();
            stopService(serviceLifecycle);
            iterator.remove();
        }
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

    private void handleEmbeddedFailure(RuntimeException failure) {
        if (!handlingEmbeddedFailure.compareAndSet(false, true)) {
            return;
        }
        log.error("Aion embedded service failure detected; stopping services.", failure);
        destroy();
        if (applicationContext != null && applicationContext.isActive()) {
            applicationContext.close();
        }
    }
}
