package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class GameSystemPropertiesGateway {

    public void start() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }
}
