package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameRuntimeServiceBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException periodicSaveProviderUsed = new ProviderUsedException();
        ProviderUsedException adminProviderUsed = new ProviderUsedException();
        ProviderUsedException playerTransferProviderUsed = new ProviderUsedException();
        ProviderUsedException territoryProviderUsed = new ProviderUsedException();
        ProviderUsedException gameTimeProviderUsed = new ProviderUsedException();
        ProviderUsedException announcementProviderUsed = new ProviderUsedException();
        ProviderUsedException debugProviderUsed = new ProviderUsedException();
        ProviderUsedException weatherProviderUsed = new ProviderUsedException();
        ProviderUsedException brokerProviderUsed = new ProviderUsedException();
        ProviderUsedException influenceProviderUsed = new ProviderUsedException();
        ProviderUsedException exchangeProviderUsed = new ProviderUsedException();
        ProviderUsedException petitionProviderUsed = new ProviderUsedException();
        ProviderUsedException flyRingProviderUsed = new ProviderUsedException();
        ProviderUsedException curingZoneProviderUsed = new ProviderUsedException();
        ProviderUsedException springZoneProviderUsed = new ProviderUsedException();
        ProviderUsedException boostEventProviderUsed = new ProviderUsedException();
        ProviderUsedException taskManagerProviderUsed = new ProviderUsedException();
        ProviderUsedException limitedItemTradeProviderUsed = new ProviderUsedException();
        GameRuntimeServiceBridge runtimeBridge = new GameRuntimeServiceBridge();

        runtimeBridge.setPeriodicSaveServiceProvider(throwingProvider(periodicSaveProviderUsed));
        runtimeBridge.setAdminServiceProvider(throwingProvider(adminProviderUsed));
        runtimeBridge.setPlayerTransferServiceProvider(throwingProvider(playerTransferProviderUsed));
        runtimeBridge.setTerritoryServiceProvider(throwingProvider(territoryProviderUsed));
        runtimeBridge.setGameTimeServiceProvider(throwingProvider(gameTimeProviderUsed));
        runtimeBridge.setAnnouncementServiceProvider(throwingProvider(announcementProviderUsed));
        runtimeBridge.setDebugServiceProvider(throwingProvider(debugProviderUsed));
        runtimeBridge.setWeatherServiceProvider(throwingProvider(weatherProviderUsed));
        runtimeBridge.setBrokerServiceProvider(throwingProvider(brokerProviderUsed));
        runtimeBridge.setInfluenceProvider(throwingProvider(influenceProviderUsed));
        runtimeBridge.setExchangeServiceProvider(throwingProvider(exchangeProviderUsed));
        runtimeBridge.setPetitionServiceProvider(throwingProvider(petitionProviderUsed));
        runtimeBridge.setFlyRingServiceProvider(throwingProvider(flyRingProviderUsed));
        runtimeBridge.setCuringZoneServiceProvider(throwingProvider(curingZoneProviderUsed));
        runtimeBridge.setSpringZoneServiceProvider(throwingProvider(springZoneProviderUsed));
        runtimeBridge.setBoostEventServiceProvider(throwingProvider(boostEventProviderUsed));
        runtimeBridge.setTaskManagerFromDBProvider(throwingProvider(taskManagerProviderUsed));
        runtimeBridge.setLimitedItemTradeServiceProvider(throwingProvider(limitedItemTradeProviderUsed));

        assertSame(periodicSaveProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::periodicSaveService));
        assertSame(adminProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::adminService));
        assertSame(playerTransferProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::playerTransferService));
        assertSame(territoryProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::territoryService));
        assertSame(gameTimeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::gameTimeService));
        assertSame(announcementProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::announcementService));
        assertSame(debugProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::debugService));
        assertSame(weatherProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::weatherService));
        assertSame(brokerProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::brokerService));
        assertSame(influenceProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::influence));
        assertSame(exchangeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::exchangeService));
        assertSame(petitionProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::petitionService));
        assertSame(flyRingProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::flyRingService));
        assertSame(curingZoneProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::curingZoneService));
        assertSame(springZoneProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::springZoneService));
        assertSame(boostEventProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::boostEventService));
        assertSame(taskManagerProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::taskManagerFromDB));
        assertSame(limitedItemTradeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::limitedItemTradeService));
    }

    private static <T> ObjectProvider<T> throwingProvider(ProviderUsedException exception) {
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
