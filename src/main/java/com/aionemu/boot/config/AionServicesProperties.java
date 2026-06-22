package com.aionemu.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aion.services")
public class AionServicesProperties {

    private final Service game = new Service(true);
    private final Service login = new Service(true);
    private final Service chat = new Service(false);
    private final Transport transport = new Transport();

    public Service getGame() {
        return game;
    }

    public Service getLogin() {
        return login;
    }

    public Service getChat() {
        return chat;
    }

    public Transport getTransport() {
        return transport;
    }

    public static class Service {
        private boolean enabled;

        public Service() {
        }

        public Service(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Transport {
        private TransportMode mode = TransportMode.NETTY;

        public TransportMode getMode() {
            return mode;
        }

        public void setMode(TransportMode mode) {
            this.mode = mode;
        }
    }

    public enum TransportMode {
        LEGACY_NIO,
        NETTY
    }
}
