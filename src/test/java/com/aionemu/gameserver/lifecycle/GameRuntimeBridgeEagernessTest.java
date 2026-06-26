package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GameRuntimeBridgeEagernessTest {

    @Test
    void runtimeBridgeComponentsAreEagerSpringWiring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(
                    GameBattlefieldRuntimeBridge.class,
                    GameCoreServicesRuntimeBridge.class,
                    GameEnginesRuntimeBridge.class,
                    GameEventBootstrapRuntimeBridge.class,
                    GameEventRuntimeBridge.class,
                    GameFeatureServicesRuntimeBridge.class,
                    GameHousingRuntimeBridge.class,
                    GameLocationBootstrapRuntimeBridge.class,
                    GameMaintenanceServicesRuntimeBridge.class,
                    GameNetworkStartupRuntimeBridge.class,
                    GameServerNetworkRuntimeBridge.class,
                    GameUtilityServicesRuntimeBridge.class,
                    GameWorldBootstrapRuntimeBridge.class,
                    GameWorldServicesRuntimeBridge.class);
            context.refresh();

            assertEager(context, "gameBattlefieldRuntimeBridge");
            assertEager(context, "gameCoreServicesRuntimeBridge");
            assertEager(context, "gameEnginesRuntimeBridge");
            assertEager(context, "gameEventBootstrapRuntimeBridge");
            assertEager(context, "gameEventRuntimeBridge");
            assertEager(context, "gameFeatureServicesRuntimeBridge");
            assertEager(context, "gameHousingRuntimeBridge");
            assertEager(context, "gameLocationBootstrapRuntimeBridge");
            assertEager(context, "gameMaintenanceServicesRuntimeBridge");
            assertEager(context, "gameNetworkStartupRuntimeBridge");
            assertEager(context, "gameServerNetworkRuntimeBridge");
            assertEager(context, "gameUtilityServicesRuntimeBridge");
            assertEager(context, "gameWorldBootstrapRuntimeBridge");
            assertEager(context, "gameWorldServicesRuntimeBridge");
        }
    }

    private static void assertEager(AnnotationConfigApplicationContext context, String beanName) {
        assertFalse(context.getBeanFactory().getBeanDefinition(beanName).isLazyInit());
    }
}
