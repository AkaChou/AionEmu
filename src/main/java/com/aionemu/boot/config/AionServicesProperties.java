package com.aionemu.boot.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "aion.services")
public class AionServicesProperties {

    private final Service game = new Service(true);
    private final Service login = new Service(true);
    private final Service chat = new Service(false);
    private final Transport transport = new Transport();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Service {
        private boolean enabled;
    }

    @Getter
    public static class Transport {
        private String mode = TransportMode.NETTY.name();

        public TransportMode getMode() {
            if (mode == null || mode.isBlank() || "LEGACY_NIO".equalsIgnoreCase(mode)) {
                return TransportMode.NETTY;
            }
            return TransportMode.valueOf(mode.toUpperCase().replace('-', '_'));
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public enum TransportMode {
        NETTY
    }
}
