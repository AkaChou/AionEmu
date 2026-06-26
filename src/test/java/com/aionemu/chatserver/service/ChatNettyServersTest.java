package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.network.netty.NettyServer;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ChatNettyServersTest {

    @Test
    void usesSpringProviderBeforeLocalFallback() {
        ProviderUsedException providerUsed = new ProviderUsedException();
        ChatNettyServers services = new ChatNettyServers(throwingProvider(providerUsed));

        try {
            assertSame(providerUsed, assertThrows(ProviderUsedException.class, ChatNettyServers::nettyServer));
        } finally {
            services.destroy();
        }
    }

    @Test
    void springAndLegacyPathsUseNettyBridgeInsteadOfDirectSingleton() throws IOException, NoSuchMethodException {
        String configurationSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/configs/ChatServerSpringConfiguration.java"));
        String dependenciesSource = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/ChatServerLegacyDependencies.java"));

        assertFalse(configurationSource.contains("NettyServer.getInstance("));
        assertFalse(dependenciesSource.contains("NettyServer.getInstance()"));
        assertTrue(configurationSource.contains("return new NettyServer(clientPacketHandler);"));
        assertTrue(dependenciesSource.contains("ChatNettyServers.nettyServer()"));
        assertTrue(NettyServer.class.getMethod("getInstance").isAnnotationPresent(Deprecated.class));
    }

    private static ObjectProvider<NettyServer> throwingProvider(ProviderUsedException exception) {
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "throwingProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                throw exception;
            }
        ));
    }

    private static final class ProviderUsedException extends RuntimeException {
    }
}
