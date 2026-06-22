package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class AionTransportBoundary implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AionTransportBoundary.class);

    private final AionServicesProperties services;
    private final LegacyNioTransportLifecycle legacyNioTransport;
    private final NettyTransportLifecycle nettyTransport;

    public AionTransportBoundary(
        AionServicesProperties services,
        LegacyNioTransportLifecycle legacyNioTransport,
        NettyTransportLifecycle nettyTransport
    ) {
        this.services = services;
        this.legacyNioTransport = legacyNioTransport;
        this.nettyTransport = nettyTransport;
    }

    public void prepare() {
        TransportMode mode = services.getTransport().getMode();
        if (mode == TransportMode.NETTY) {
            throw new IllegalStateException("Netty transport mode is not active yet. Endpoint adapters must be migrated before setting aion.services.transport.mode=netty.");
        }

        legacyNioTransport.start();
    }

    @Override
    public void destroy() {
        nettyTransport.stop();
    }
}
