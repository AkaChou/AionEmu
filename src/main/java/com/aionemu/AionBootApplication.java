package com.aionemu;

import com.aionemu.boot.callback.CallbackWeavingBootstrap;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyGameProperties;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ AionServicesProperties.class, LegacyGameProperties.class })
public class AionBootApplication {

    public static void main(String[] args) {
        AionRuntimeMode.enableBootEmbeddedMode();
        CallbackWeavingBootstrap.weaveExplodedClassesIfNeeded(AionBootApplication.class);
        new SpringApplicationBuilder(AionBootApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }
}
