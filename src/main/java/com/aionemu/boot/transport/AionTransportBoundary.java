package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class AionTransportBoundary implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AionTransportBoundary.class);

    private final AionServicesProperties services;
    private final Map<TransportMode, AionTransportLifecycle> transportsByMode;
    private AionTransportLifecycle activeTransport;

    @Autowired
    public AionTransportBoundary(
        AionServicesProperties services,
        List<AionTransportLifecycle> transportLifecycles
    ) {
        this.services = services;
        this.transportsByMode = indexTransportLifecycles(transportLifecycles);
    }

    public AionTransportBoundary(
        AionServicesProperties services,
        NettyTransportLifecycle nettyTransport
    ) {
        this(services, List.of(nettyTransport));
    }

    public void prepare() {
        TransportMode mode = services.getTransport().getMode();
        AionTransportLifecycle transport = transport(mode);
        System.setProperty("aion.transport.netty", Boolean.toString(transport.nettyEnabled()));
        activeTransport = transport;
        transport.start();
        if (mode == TransportMode.NETTY) {
            log.info("Using Netty transport mode for migrated server endpoints.");
        }
    }

    @Override
    public void destroy() {
        if (activeTransport != null) {
            activeTransport.stop();
        }
        activeTransport = null;
    }

    private AionTransportLifecycle transport(TransportMode mode) {
        AionTransportLifecycle transport = transportsByMode.get(mode);
        if (transport == null) {
            throw new IllegalStateException("No transport lifecycle registered for mode " + mode);
        }
        return transport;
    }

    private static Map<TransportMode, AionTransportLifecycle> indexTransportLifecycles(List<AionTransportLifecycle> transportLifecycles) {
        Map<TransportMode, AionTransportLifecycle> indexed = new EnumMap<>(TransportMode.class);
        for (AionTransportLifecycle transportLifecycle : transportLifecycles) {
            AionTransportLifecycle previous = indexed.put(transportLifecycle.mode(), transportLifecycle);
            if (previous != null) {
                throw new IllegalStateException("Duplicate transport lifecycle registered for mode " + transportLifecycle.mode());
            }
        }
        return indexed;
    }
}
