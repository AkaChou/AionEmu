package com.aionemu.boot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "aion.game")
public class AionGameProperties {

    private final Network network = new Network();
    private final Startup startup = new Startup();

    @Getter
    @Setter
    public static class Network {

        private String externalIp;
    }

    @Getter
    public static class Startup {

        private final Progress progress = new Progress();
    }

    @Getter
    @Setter
    public static class Progress {

        private Boolean enabled;
    }
}
