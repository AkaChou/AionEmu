package com.aionemu.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.AionBootApplication;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.boot.lifecycle.AionServiceLauncher;
import com.aionemu.boot.lifecycle.ChatServerLifecycleGateway;
import com.aionemu.boot.lifecycle.ChatServerRuntimeBridge;
import com.aionemu.boot.lifecycle.ChatServiceLifecycle;
import com.aionemu.boot.lifecycle.GameServiceLifecycle;
import com.aionemu.boot.lifecycle.LoginServerLifecycleGateway;
import com.aionemu.boot.lifecycle.LoginServerRuntimeBridge;
import com.aionemu.boot.lifecycle.LoginServiceLifecycle;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.boot.transport.LegacyNioTransportLifecycle;
import com.aionemu.boot.transport.NettyTransportLifecycle;
import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ChatServerRuntime;
import com.aionemu.chatserver.ChatServerStartupBridge;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.network.netty.pipeline.LoginToClientPipeLineFactory;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.lifecycle.GameAdminPanelLifecycle;
import com.aionemu.gameserver.lifecycle.GameAdminPanelGateway;
import com.aionemu.gameserver.lifecycle.GameBattlefieldGateway;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideGateway;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningGateway;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCoreServicesRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameCustomEventsGateway;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandGateway;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionGateway;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesGateway;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeGateway;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameFeatureServicesRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameGeoNavGateway;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlGateway;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingGateway;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameLoggingGateway;
import com.aionemu.gameserver.lifecycle.GameLoggingLifecycle;
import com.aionemu.gameserver.lifecycle.GameMaintenanceServicesRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupGateway;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupLifecycle;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesGateway;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorGateway;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRatioLimitGateway;
import com.aionemu.gameserver.lifecycle.GameRatioLimitLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingGateway;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
import com.aionemu.gameserver.lifecycle.GameServerNetworkGateway;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesGateway;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleGateway;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnGateway;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataGateway;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionGateway;
import com.aionemu.gameserver.lifecycle.GameStartupHooksGateway;
import com.aionemu.gameserver.lifecycle.GameStartupHooksLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupLogGateway;
import com.aionemu.gameserver.lifecycle.GameStartupLogLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemGateway;
import com.aionemu.gameserver.lifecycle.GameSystemLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesGateway;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolGateway;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesGateway;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameWorldActivationGateway;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldServicesRuntimeBridge;
import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.loginserver.LoginServer;
import com.aionemu.loginserver.lifecycle.LoginStartupRuntimeBridge;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class AionBootApplicationTest {

    @AfterEach
    void clearBootRuntimeFlags() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        System.clearProperty("aion.transport.netty");
    }

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

        assertEquals(List.of(mainSource.resolve("com/aionemu/AionBootApplication.java")), productionMainFiles);
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
    void bootApplicationScansGameLifecycleBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AionBootApplication.class)) {
            assertTrue(context.containsBean("aionServiceLauncher"));
            assertTrue(context.containsBean("gameStartupSequenceLifecycle"));
        }
    }

    @Test
    void bootApplicationScansGameLegacyBridgeBeansLazily() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AionBootApplication.class)) {
            assertEquals(AdminService.class, context.getType("adminService"));
            assertEquals(GameRuntimeServiceBridge.class, context.getType("gameRuntimeServiceBridge"));
            assertEquals(GameCoreServicesRuntimeBridge.class, context.getType("gameCoreServicesRuntimeBridge"));
            assertEquals(GameMaintenanceServicesRuntimeBridge.class, context.getType("gameMaintenanceServicesRuntimeBridge"));
            assertEquals(GameWorldServicesRuntimeBridge.class, context.getType("gameWorldServicesRuntimeBridge"));
            assertEquals(GameFeatureServicesRuntimeBridge.class, context.getType("gameFeatureServicesRuntimeBridge"));
            assertEquals(GameUtilityServicesRuntimeBridge.class, context.getType("gameUtilityServicesRuntimeBridge"));
            assertEquals(GameNetworkStartupRuntimeBridge.class, context.getType("gameNetworkStartupRuntimeBridge"));
            assertLazy(context.getBeanFactory(), "adminService");
            assertLazy(context.getBeanFactory(), "gameRuntimeServiceBridge");
            assertLazy(context.getBeanFactory(), "gameCoreServicesRuntimeBridge");
            assertLazy(context.getBeanFactory(), "gameMaintenanceServicesRuntimeBridge");
            assertLazy(context.getBeanFactory(), "gameWorldServicesRuntimeBridge");
            assertLazy(context.getBeanFactory(), "gameFeatureServicesRuntimeBridge");
            assertLazy(context.getBeanFactory(), "gameUtilityServicesRuntimeBridge");
            assertLazy(context.getBeanFactory(), "gameNetworkStartupRuntimeBridge");
        }
    }

    @Test
    void bootApplicationScansServerRuntimeBridgeBeansLazily() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AionBootApplication.class)) {
            assertEquals(LoginServerRuntimeBridge.class, context.getType("loginServerRuntimeBridge"));
            assertEquals(ChatServerRuntimeBridge.class, context.getType("chatServerRuntimeBridge"));
            assertEquals(LoginStartupRuntimeBridge.class, context.getType("loginStartupRuntimeBridge"));
            assertLazy(context.getBeanFactory(), "loginServerRuntimeBridge");
            assertLazy(context.getBeanFactory(), "chatServerRuntimeBridge");
            assertLazy(context.getBeanFactory(), "loginStartupRuntimeBridge");
        }
    }

    @Test
    void springApplicationStartsWithServicesDisabled() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AionBootApplication.class)
            .web(WebApplicationType.NONE)
            .bannerMode(org.springframework.boot.Banner.Mode.OFF)
            .run(
                "--boot-smoke=true",
                "--aion.services.login.enabled=false",
                "--aion.services.chat.enabled=false",
                "--aion.services.game.enabled=false"
            )) {
            assertTrue(context.isActive());
            assertTrue(AionRuntimeMode.isBootEmbedded());
            assertEquals("true", System.getProperty("aion.transport.netty"));
        }
    }

    @Test
    void chatRuntimeBeansStayOptionalByDefault() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AionBootApplication.class)) {
            assertHasBean(context, ChatServiceLifecycle.class);
            assertHasBean(context, ChatServerLifecycleGateway.class);
            assertDoesNotHaveBean(context, ChatServerRuntime.class);
            assertDoesNotHaveBean(context, ChatServerStartupBridge.class);
            assertDoesNotHaveBean(context, IdFactory.class);
            assertDoesNotHaveBean(context, ClientPacketHandler.class);
            assertDoesNotHaveBean(context, LoginToClientPipeLineFactory.class);
            assertDoesNotHaveBean(context, NettyServer.class);
            assertDoesNotHaveBean(context, GameServerService.class);
            assertDoesNotHaveBean(context, BroadcastService.class);
            assertDoesNotHaveBean(context, ChatService.class);
            assertDoesNotHaveBean(context, RestartService.class);
        }
    }

    @Test
    void chatServerGuiceBindingsAreSpringBeansWhenChatIsEnabled() {
        try (AnnotationConfigApplicationContext context = chatEnabledBootContext()) {
            assertHasBean(context, ChatServerRuntime.class);
            assertHasBean(context, ChatServerStartupBridge.class);
            assertHasBean(context, IdFactory.class);
            assertHasBean(context, ClientPacketHandler.class);
            assertHasBean(context, LoginToClientPipeLineFactory.class);
            assertHasBean(context, NettyServer.class);
            assertHasBean(context, GameServerService.class);
            assertHasBean(context, BroadcastService.class);
            assertHasBean(context, ChatService.class);
            assertHasBean(context, RestartService.class);

            assertTrue(context.getBeanFactory().getBeanDefinition("nettyServer").isLazyInit());
            assertTrue(context.getBeanFactory().getBeanDefinition("restartService").isLazyInit());
            assertTrue(context.getBeanFactory().getBeanDefinition("chatServerStartupBridge").isLazyInit());
        }
    }

    @Test
    void productionSourcesDoNotUseGuice() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        assertFalse(pom.contains("<guice.version>"), "pom.xml must not declare guice.version");
        assertFalse(pom.contains("com.google.inject"), "pom.xml must not depend on com.google.inject");
        assertFalse(pom.contains("<artifactId>guice</artifactId>"), "pom.xml must not depend on guice");

        Path mainSource = Path.of("src/main/java");
        try (var paths = Files.walk(mainSource)) {
            List<Path> guicePaths = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.toString().toLowerCase().contains("guice"))
                .sorted()
                .toList();
            assertEquals(List.of(), guicePaths);
        }

        List<Path> guiceImports;
        try (var paths = Files.walk(mainSource)) {
            guiceImports = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> sourceContains(path, "com.google.inject"))
                .sorted()
                .toList();
        }
        assertEquals(List.of(), guiceImports);
    }

    @Test
    void serviceLauncherCanBeCreatedAsSpringBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AionServicesProperties.class);
            context.registerBean(LegacyNioTransportLifecycle.class);
            context.registerBean(NettyTransportLifecycle.class);
            context.registerBean(AionTransportBoundary.class);
            context.registerBean(LegacyConfigOverrides.class);
            context.registerBean(GameCoreServicesRuntimeBridge.class);
            context.registerBean(GameMaintenanceServicesRuntimeBridge.class);
            context.registerBean(GameWorldServicesRuntimeBridge.class);
            context.registerBean(GameFeatureServicesRuntimeBridge.class);
            context.registerBean(GameLoggingGateway.class);
            context.registerBean(GameLoggingLifecycle.class);
            context.registerBean(GameStaticDataGateway.class);
            context.registerBean(GameStaticDataLifecycle.class);
            context.registerBean(GameWorldBootstrapGateway.class);
            context.registerBean(GameWorldBootstrapLifecycle.class);
            context.registerBean(GameEventBootstrapGateway.class);
            context.registerBean(GameEventBootstrapLifecycle.class);
            context.registerBean(GameGeoNavGateway.class);
            context.registerBean(GameGeoNavLifecycle.class);
            context.registerBean(GameWorldActivationGateway.class);
            context.registerBean(GameWorldActivationLifecycle.class);
            context.registerBean(GameEnginesGateway.class);
            context.registerBean(GameEnginesLifecycle.class);
            context.registerBean(GameLocationBootstrapGateway.class);
            context.registerBean(GameLocationBootstrapLifecycle.class);
            context.registerBean(GameSpawnGateway.class);
            context.registerBean(GameSpawnLifecycle.class);
            context.registerBean(GameEventRuntimeGateway.class);
            context.registerBean(GameEventRuntimeLifecycle.class);
            context.registerBean(GameCleaningGateway.class);
            context.registerBean(GameCleaningLifecycle.class);
            context.registerBean(GameScheduledServicesGateway.class);
            context.registerBean(GameScheduledServicesLifecycle.class);
            context.registerBean(GameCustomEventsGateway.class);
            context.registerBean(GameCustomEventsLifecycle.class);
            context.registerBean(GameSiegeScheduleGateway.class);
            context.registerBean(GameSiegeScheduleLifecycle.class);
            context.registerBean(GameDredgionGateway.class);
            context.registerBean(GameDredgionLifecycle.class);
            context.registerBean(GameBattlefieldGateway.class);
            context.registerBean(GameBattlefieldLifecycle.class);
            context.registerBean(GameProtectorConquerorGateway.class);
            context.registerBean(GameProtectorConquerorLifecycle.class);
            context.registerBean(GameDisputeLandGateway.class);
            context.registerBean(GameDisputeLandLifecycle.class);
            context.registerBean(GameHtmlGateway.class);
            context.registerBean(GameHtmlLifecycle.class);
            context.registerBean(GameRewardServicesGateway.class);
            context.registerBean(GameRewardServicesLifecycle.class);
            context.registerBean(GameRuntimeServicesGateway.class);
            context.registerBean(GameRuntimeServicesLifecycle.class);
            context.registerBean(GameOptionalServicesGateway.class);
            context.registerBean(GameOptionalServicesLifecycle.class);
            context.registerBean(GameSeasonRankingGateway.class);
            context.registerBean(GameSeasonRankingLifecycle.class);
            context.registerBean(GameServerNetworkGateway.class);
            context.registerBean(GameServerNetworkLifecycle.class);
            context.registerBean(GameHousingGateway.class);
            context.registerBean(GameHousingLifecycle.class);
            context.registerBean(GameSystemGateway.class);
            context.registerBean(GameSystemLifecycle.class);
            context.registerBean(GameNetworkStartupGateway.class);
            context.registerBean(GameNetworkStartupRuntimeBridge.class);
            context.registerBean(GameNetworkStartupLifecycle.class);
            context.registerBean(GameRatioLimitGateway.class);
            context.registerBean(GameRatioLimitLifecycle.class);
            context.registerBean(GameStartupHooksGateway.class);
            context.registerBean(GameStartupHooksLifecycle.class);
            context.registerBean(GameStartupCompletionGateway.class);
            context.registerBean(GameStartupCompletionLifecycle.class);
            context.registerBean(GameStartupSequenceLifecycle.class);
            context.registerBean(GameUtilityServicesGateway.class);
            context.registerBean(GameUtilityServicesLifecycle.class);
            context.registerBean(GameAdminPanelGateway.class);
            context.registerBean(GameAdminPanelLifecycle.class);
            context.registerBean(GameSystemPropertiesGateway.class);
            context.registerBean(GameSystemPropertiesLifecycle.class);
            context.registerBean(GameStartupLogGateway.class);
            context.registerBean(GameStartupLogLifecycle.class);
            context.registerBean(GameChatServerOverrideGateway.class);
            context.registerBean(GameChatServerOverrideLifecycle.class);
            context.registerBean(GameThreadPoolGateway.class);
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

    private static void assertHasBean(AnnotationConfigApplicationContext context, Class<?> type) {
        Assertions.assertTrue(
            context.getBeanNamesForType(type, true, false).length > 0,
            () -> "Missing Spring bean for " + type.getName()
        );
    }

    private static void assertDoesNotHaveBean(AnnotationConfigApplicationContext context, Class<?> type) {
        Assertions.assertEquals(
            0,
            context.getBeanNamesForType(type, true, false).length,
            () -> "Unexpected Spring bean for " + type.getName()
        );
    }

    private static void assertLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
        Assertions.assertTrue(beanFactory.getBeanDefinition(beanName).isLazyInit(), beanName + " should be lazy");
    }

    private static AnnotationConfigApplicationContext chatEnabledBootContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
            "chatEnabledTestProperties",
            Map.of("aion.services.chat.enabled", "true")
        ));
        context.register(AionBootApplication.class);
        context.refresh();
        return context;
    }

    private static boolean sourceContains(Path path, String needle) {
        try {
            return Files.readString(path).contains(needle);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

}
