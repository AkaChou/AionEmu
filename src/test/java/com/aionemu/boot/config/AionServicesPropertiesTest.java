package com.aionemu.boot.config;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AionServicesPropertiesTest {

    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    @BeforeEach
    void clearConfigOverrides() {
        System.clearProperty("aion.services.chat.enabled");
    }

    @Test
    void defaultsToSingleBootNettyRuntimeWithOptionalChat() {
        AionServicesProperties properties = new AionServicesProperties();

        assertTrue(properties.getLogin().isEnabled());
        assertTrue(properties.getGame().isEnabled());
        assertFalse(properties.getChat().isEnabled());
        assertSame(TransportMode.NETTY, properties.getTransport().getMode());
    }

    @Test
    void applicationDefaultsKeepChatOptional() throws IOException {
        AionServicesProperties properties = bindFromYaml("application.yml");

        assertTrue(properties.getLogin().isEnabled());
        assertTrue(properties.getGame().isEnabled());
        assertFalse(properties.getChat().isEnabled());
        assertSame(TransportMode.NETTY, properties.getTransport().getMode());
    }

    @Test
    void chatProfileEnablesChatService() throws IOException {
        AionServicesProperties properties = bindFromYaml("application.yml");

        assertTrue(properties.getLogin().isEnabled());
        assertTrue(properties.getGame().isEnabled());
        assertTrue(properties.getChat().isEnabled());
        assertSame(TransportMode.NETTY, properties.getTransport().getMode());
    }

    @Test
    void legacyNioModeIsRejectedAfterFallbackRemoval() {
        AionServicesProperties properties = new AionServicesProperties();

        properties.getTransport().setMode("LEGACY_NIO");

        assertThrows(IllegalArgumentException.class, () -> properties.getTransport().getMode());
    }

    private AionServicesProperties bindFromYaml(String... resources) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        for (int i = resources.length - 1; i >= 0; i--) {
            for (PropertySource<?> propertySource : yamlLoader.load(resources[i], new ClassPathResource(resources[i]))) {
                propertySources.addFirst(propertySource);
            }
        }

        AionServicesProperties properties = new AionServicesProperties();
        Binder.get(environment).bind("aion.services", Bindable.ofInstance(properties));
        return properties;
    }
}
