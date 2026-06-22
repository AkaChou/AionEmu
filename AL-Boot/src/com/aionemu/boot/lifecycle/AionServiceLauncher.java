package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AionServiceLauncher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AionServiceLauncher.class);

    private final AionServicesProperties services;
    private final AionTransportBoundary transportBoundary;
    private final List<AionServiceLifecycle> serviceLifecycles;

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

    private void startService(AionServiceLifecycle serviceLifecycle, ApplicationArguments args) throws Exception {
        String name = serviceLifecycle.getName();
        log.info("Starting {} service...", name);
        serviceLifecycle.start(args);
        log.info("{} service startup returned.", name);
    }
}
