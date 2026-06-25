package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameFeatureServicesRuntimeBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException playerLimitProviderUsed = new ProviderUsedException();
        ProviderUsedException npcShoutsProviderUsed = new ProviderUsedException();
        ProviderUsedException shieldProviderUsed = new ProviderUsedException();
        ProviderUsedException rewardProviderUsed = new ProviderUsedException();
        ProviderUsedException weddingProviderUsed = new ProviderUsedException();
        ProviderUsedException veteranRewardsProviderUsed = new ProviderUsedException();
        ProviderUsedException disputeLandProviderUsed = new ProviderUsedException();
        ProviderUsedException outpostProviderUsed = new ProviderUsedException();
        ProviderUsedException protectorConquerorProviderUsed = new ProviderUsedException();
        ProviderUsedException dredgionProviderUsed = new ProviderUsedException();
        ProviderUsedException asyunatarProviderUsed = new ProviderUsedException();
        ProviderUsedException ffaProviderUsed = new ProviderUsedException();
        ProviderUsedException ladderProviderUsed = new ProviderUsedException();
        ProviderUsedException bgProviderUsed = new ProviderUsedException();
        ProviderUsedException banditProviderUsed = new ProviderUsedException();
        ProviderUsedException siegeProviderUsed = new ProviderUsedException();
        ProviderUsedException baseProviderUsed = new ProviderUsedException();
        GameFeatureServicesRuntimeBridge runtimeBridge = new GameFeatureServicesRuntimeBridge();

        runtimeBridge.setPlayerLimitServiceProvider(throwingProvider(playerLimitProviderUsed));
        runtimeBridge.setNpcShoutsServiceProvider(throwingProvider(npcShoutsProviderUsed));
        runtimeBridge.setShieldServiceProvider(throwingProvider(shieldProviderUsed));
        runtimeBridge.setRewardServiceProvider(throwingProvider(rewardProviderUsed));
        runtimeBridge.setWeddingServiceProvider(throwingProvider(weddingProviderUsed));
        runtimeBridge.setVeteranRewardsServiceProvider(throwingProvider(veteranRewardsProviderUsed));
        runtimeBridge.setDisputeLandServiceProvider(throwingProvider(disputeLandProviderUsed));
        runtimeBridge.setOutpostServiceProvider(throwingProvider(outpostProviderUsed));
        runtimeBridge.setProtectorConquerorServiceProvider(throwingProvider(protectorConquerorProviderUsed));
        runtimeBridge.setDredgionServiceProvider(throwingProvider(dredgionProviderUsed));
        runtimeBridge.setAsyunatarServiceProvider(throwingProvider(asyunatarProviderUsed));
        runtimeBridge.setFfaServiceProvider(throwingProvider(ffaProviderUsed));
        runtimeBridge.setLadderServiceProvider(throwingProvider(ladderProviderUsed));
        runtimeBridge.setBgServiceProvider(throwingProvider(bgProviderUsed));
        runtimeBridge.setBanditServiceProvider(throwingProvider(banditProviderUsed));
        runtimeBridge.setSiegeServiceProvider(throwingProvider(siegeProviderUsed));
        runtimeBridge.setBaseServiceProvider(throwingProvider(baseProviderUsed));

        assertSame(playerLimitProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::playerLimitService));
        assertSame(npcShoutsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::npcShoutsService));
        assertSame(shieldProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::shieldService));
        assertSame(rewardProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::rewardService));
        assertSame(weddingProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::weddingService));
        assertSame(veteranRewardsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::veteranRewardsService));
        assertSame(disputeLandProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::disputeLandService));
        assertSame(outpostProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::outpostService));
        assertSame(protectorConquerorProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::protectorConquerorService));
        assertSame(dredgionProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::dredgionService));
        assertSame(asyunatarProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::asyunatarService));
        assertSame(ffaProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::ffaService));
        assertSame(ladderProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::ladderService));
        assertSame(bgProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::bgService));
        assertSame(banditProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::banditService));
        assertSame(siegeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::siegeService));
        assertSame(baseProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::baseService));
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
