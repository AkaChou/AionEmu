package com.aionemu.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.lifecycle.AionServiceLauncher;
import com.aionemu.boot.lifecycle.ChatServiceLifecycle;
import com.aionemu.boot.lifecycle.GameServiceLifecycle;
import com.aionemu.boot.lifecycle.LoginServiceLifecycle;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.boot.transport.LegacyNioTransportLifecycle;
import com.aionemu.boot.transport.NettyTransportLifecycle;
import com.aionemu.chatserver.ChatServer;
import com.aionemu.gameserver.GameServer;
import com.aionemu.loginserver.LoginServer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AionBootApplicationTest {

    @Test
    void bootApplicationIsTheOnlyServerEntrypoint() {
        assertTrue(hasPublicStaticMain(AionBootApplication.class));
        assertFalse(hasPublicStaticMain(LoginServer.class));
        assertFalse(hasPublicStaticMain(ChatServer.class));
        assertFalse(hasPublicStaticMain(GameServer.class));
    }

    @Test
    void productionSourcesExposeOnlyBootMain() throws IOException {
        Path mainSource = Path.of("src/main/java");
        List<Path> productionMainFiles;
        try (var paths = Files.walk(mainSource)) {
            productionMainFiles = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(AionBootApplicationTest::declaresPublicStaticMain)
                .sorted()
                .toList();
        }

        assertEquals(List.of(mainSource.resolve("com/aionemu/boot/AionBootApplication.java")), productionMainFiles);
    }

    @Test
    void serviceLauncherCanBeCreatedAsSpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AionServicesProperties.class);
            context.registerBean(LegacyNioTransportLifecycle.class);
            context.registerBean(NettyTransportLifecycle.class);
            context.registerBean(AionTransportBoundary.class);
            context.registerBean(LoginServiceLifecycle.class);
            context.registerBean(ChatServiceLifecycle.class);
            context.registerBean(GameServiceLifecycle.class);
            context.registerBean(AionServiceLauncher.class);

            context.refresh();

            assertTrue(context.containsBean("aionServiceLauncher"));
        }
    }

    private static boolean declaresPublicStaticMain(Path path) {
        try {
            String source = Files.readString(path);
            return source.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(\\s*(?:final\\s+)?String\\s*\\[\\]\\s+\\w+\\s*\\).*");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static boolean hasPublicStaticMain(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (method.getName().equals("main")
                && Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && method.getReturnType() == Void.TYPE
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String[].class) {
                return true;
            }
        }
        return false;
    }

}
