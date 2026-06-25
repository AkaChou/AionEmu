package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;

public interface AionTransportLifecycle {

    TransportMode mode();

    boolean nettyEnabled();

    void start();

    void stop();
}
