package com.aionemu.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.boot.lifecycle.AionServiceLauncher;
import com.aionemu.boot.lifecycle.ChatServerLifecycleGateway;
import com.aionemu.boot.lifecycle.ChatServiceLifecycle;
import com.aionemu.boot.lifecycle.GameServiceLifecycle;
import com.aionemu.boot.lifecycle.LoginServerLifecycleGateway;
import com.aionemu.boot.lifecycle.LoginServiceLifecycle;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.boot.transport.LegacyNioTransportLifecycle;
import com.aionemu.boot.transport.NettyTransportLifecycle;
import com.aionemu.chatserver.ChatServer;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.lifecycle.GameAdminPanelLifecycle;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameLoggingLifecycle;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupLifecycle;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRatioLimitLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataGateway;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupHooksLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupLogLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import com.aionemu.loginserver.LoginServer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
        Path callbackBuildToolSource = mainSource.resolve("com/aionemu/commons/callbacks/weaver");
        List<Path> productionMainFiles;
        try (var paths = Files.walk(mainSource)) {
            productionMainFiles = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.startsWith(callbackBuildToolSource))
                .filter(AionBootApplicationTest::declaresPublicStaticMain)
                .sorted()
                .toList();
        }

        assertEquals(List.of(mainSource.resolve("com/aionemu/boot/AionBootApplication.java")), productionMainFiles);
    }

    @Test
    void legacyGameServerStartOverloadsAreDeprecated() {
        List<Method> startMethods = Arrays.stream(GameServer.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("start"))
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> Modifier.isStatic(method.getModifiers()))
            .toList();

        assertFalse(startMethods.isEmpty());
        assertTrue(startMethods.stream().allMatch(method -> method.isAnnotationPresent(Deprecated.class)));
    }

    @Test
    void serviceLauncherCanBeCreatedAsSpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AionServicesProperties.class);
            context.registerBean(LegacyNioTransportLifecycle.class);
            context.registerBean(NettyTransportLifecycle.class);
            context.registerBean(AionTransportBoundary.class);
            context.registerBean(LegacyConfigOverrides.class);
            context.registerBean(GameLoggingLifecycle.class);
            context.registerBean(GameStaticDataGateway.class);
            context.registerBean(GameStaticDataLifecycle.class);
            context.registerBean(GameWorldBootstrapLifecycle.class);
            context.registerBean(GameEventBootstrapLifecycle.class);
            context.registerBean(GameGeoNavLifecycle.class);
            context.registerBean(GameWorldActivationLifecycle.class);
            context.registerBean(GameEnginesLifecycle.class);
            context.registerBean(GameLocationBootstrapLifecycle.class);
            context.registerBean(GameSpawnLifecycle.class);
            context.registerBean(GameEventRuntimeLifecycle.class);
            context.registerBean(GameCleaningLifecycle.class);
            context.registerBean(GameScheduledServicesLifecycle.class);
            context.registerBean(GameCustomEventsLifecycle.class);
            context.registerBean(GameSiegeScheduleLifecycle.class);
            context.registerBean(GameDredgionLifecycle.class);
            context.registerBean(GameBattlefieldLifecycle.class);
            context.registerBean(GameProtectorConquerorLifecycle.class);
            context.registerBean(GameDisputeLandLifecycle.class);
            context.registerBean(GameHtmlLifecycle.class);
            context.registerBean(GameRewardServicesLifecycle.class);
            context.registerBean(GameRuntimeServicesLifecycle.class);
            context.registerBean(GameOptionalServicesLifecycle.class);
            context.registerBean(GameSeasonRankingLifecycle.class);
            context.registerBean(GameServerNetworkLifecycle.class);
            context.registerBean(GameHousingLifecycle.class);
            context.registerBean(GameSystemLifecycle.class);
            context.registerBean(GameNetworkStartupLifecycle.class);
            context.registerBean(GameRatioLimitLifecycle.class);
            context.registerBean(GameStartupHooksLifecycle.class);
            context.registerBean(GameStartupCompletionLifecycle.class);
            context.registerBean(GameStartupSequenceLifecycle.class);
            context.registerBean(GameUtilityServicesLifecycle.class);
            context.registerBean(GameAdminPanelLifecycle.class);
            context.registerBean(GameSystemPropertiesLifecycle.class);
            context.registerBean(GameStartupLogLifecycle.class);
            context.registerBean(GameChatServerOverrideLifecycle.class);
            context.registerBean(GameThreadPoolLifecycle.class);
            context.registerBean(LoginServerLifecycleGateway.class);
            context.registerBean(LoginServiceLifecycle.class);
            context.registerBean(ChatServerLifecycleGateway.class);
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
