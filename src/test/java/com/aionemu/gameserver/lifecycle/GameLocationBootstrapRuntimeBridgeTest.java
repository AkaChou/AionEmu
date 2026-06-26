package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.VortexService;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameLocationBootstrapRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException siegeProviderUsed = new ProviderUsedException();
        ProviderUsedException baseProviderUsed = new ProviderUsedException();
        ProviderUsedException outpostProviderUsed = new ProviderUsedException();
        ProviderUsedException vortexProviderUsed = new ProviderUsedException();
        ProviderUsedException beritraProviderUsed = new ProviderUsedException();
        ProviderUsedException agentProviderUsed = new ProviderUsedException();
        ProviderUsedException anohaProviderUsed = new ProviderUsedException();
        ProviderUsedException svsProviderUsed = new ProviderUsedException();
        ProviderUsedException rvrProviderUsed = new ProviderUsedException();
        ProviderUsedException iuProviderUsed = new ProviderUsedException();
        ProviderUsedException nightmareCircusProviderUsed = new ProviderUsedException();
        ProviderUsedException dynamicRiftProviderUsed = new ProviderUsedException();
        ProviderUsedException instanceRiftProviderUsed = new ProviderUsedException();
        ProviderUsedException zorshivDredgionProviderUsed = new ProviderUsedException();
        ProviderUsedException moltenusProviderUsed = new ProviderUsedException();
        ProviderUsedException riftProviderUsed = new ProviderUsedException();
        ProviderUsedException conquestProviderUsed = new ProviderUsedException();
        ProviderUsedException idianDepthsProviderUsed = new ProviderUsedException();
        ProviderUsedException towerOfEternityProviderUsed = new ProviderUsedException();
        ProviderUsedException abyssLandingProviderUsed = new ProviderUsedException();
        ProviderUsedException landingUpdateProviderUsed = new ProviderUsedException();
        ProviderUsedException abyssLandingSpecialProviderUsed = new ProviderUsedException();
        GameLocationBootstrapRuntimeBridge runtimeBridge = new GameLocationBootstrapRuntimeBridge();

        runtimeBridge.setSiegeServiceProvider(throwingProvider(siegeProviderUsed));
        runtimeBridge.setBaseServiceProvider(throwingProvider(baseProviderUsed));
        runtimeBridge.setOutpostServiceProvider(throwingProvider(outpostProviderUsed));
        runtimeBridge.setVortexServiceProvider(throwingProvider(vortexProviderUsed));
        runtimeBridge.setBeritraServiceProvider(throwingProvider(beritraProviderUsed));
        runtimeBridge.setAgentServiceProvider(throwingProvider(agentProviderUsed));
        runtimeBridge.setAnohaServiceProvider(throwingProvider(anohaProviderUsed));
        runtimeBridge.setSvsServiceProvider(throwingProvider(svsProviderUsed));
        runtimeBridge.setRvrServiceProvider(throwingProvider(rvrProviderUsed));
        runtimeBridge.setIuServiceProvider(throwingProvider(iuProviderUsed));
        runtimeBridge.setNightmareCircusServiceProvider(throwingProvider(nightmareCircusProviderUsed));
        runtimeBridge.setDynamicRiftServiceProvider(throwingProvider(dynamicRiftProviderUsed));
        runtimeBridge.setInstanceRiftServiceProvider(throwingProvider(instanceRiftProviderUsed));
        runtimeBridge.setZorshivDredgionServiceProvider(throwingProvider(zorshivDredgionProviderUsed));
        runtimeBridge.setMoltenusServiceProvider(throwingProvider(moltenusProviderUsed));
        runtimeBridge.setRiftServiceProvider(throwingProvider(riftProviderUsed));
        runtimeBridge.setConquestServiceProvider(throwingProvider(conquestProviderUsed));
        runtimeBridge.setIdianDepthsServiceProvider(throwingProvider(idianDepthsProviderUsed));
        runtimeBridge.setTowerOfEternityServiceProvider(throwingProvider(towerOfEternityProviderUsed));
        runtimeBridge.setAbyssLandingServiceProvider(throwingProvider(abyssLandingProviderUsed));
        runtimeBridge.setLandingUpdateServiceProvider(throwingProvider(landingUpdateProviderUsed));
        runtimeBridge.setAbyssLandingSpecialServiceProvider(throwingProvider(abyssLandingSpecialProviderUsed));

        assertSame(siegeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::siegeService));
        assertSame(baseProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::baseService));
        assertSame(outpostProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::outpostService));
        assertSame(vortexProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::vortexService));
        assertSame(beritraProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::beritraService));
        assertSame(agentProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::agentService));
        assertSame(anohaProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::anohaService));
        assertSame(svsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::svsService));
        assertSame(rvrProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::rvrService));
        assertSame(iuProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::iuService));
        assertSame(nightmareCircusProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::nightmareCircusService));
        assertSame(dynamicRiftProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::dynamicRiftService));
        assertSame(instanceRiftProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::instanceRiftService));
        assertSame(zorshivDredgionProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::zorshivDredgionService));
        assertSame(moltenusProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::moltenusService));
        assertSame(riftProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::riftService));
        assertSame(conquestProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::conquestService));
        assertSame(idianDepthsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::idianDepthsService));
        assertSame(towerOfEternityProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::towerOfEternityService));
        assertSame(abyssLandingProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::abyssLandingService));
        assertSame(landingUpdateProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::landingUpdateService));
        assertSame(abyssLandingSpecialProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::abyssLandingSpecialService));
    }

    @Test
    void locationBootstrapSingletonAccessorsUseSpringProvidersBeforeLegacyFallback() {
        VortexService vortexService = instance(VortexService.class);
        BeritraService beritraService = instance(BeritraService.class);
        AgentService agentService = instance(AgentService.class);
        AnohaService anohaService = instance(AnohaService.class);
        SvsService svsService = instance(SvsService.class);
        RvrService rvrService = instance(RvrService.class);
        IuService iuService = instance(IuService.class);
        NightmareCircusService nightmareCircusService = instance(NightmareCircusService.class);
        DynamicRiftService dynamicRiftService = instance(DynamicRiftService.class);
        InstanceRiftService instanceRiftService = instance(InstanceRiftService.class);

        try {
            VortexService.setInstanceProvider(provider(VortexService.class, vortexService));
            BeritraService.setInstanceProvider(provider(BeritraService.class, beritraService));
            AgentService.setInstanceProvider(provider(AgentService.class, agentService));
            AnohaService.setInstanceProvider(provider(AnohaService.class, anohaService));
            SvsService.setInstanceProvider(provider(SvsService.class, svsService));
            RvrService.setInstanceProvider(provider(RvrService.class, rvrService));
            IuService.setInstanceProvider(provider(IuService.class, iuService));
            NightmareCircusService.setInstanceProvider(provider(NightmareCircusService.class, nightmareCircusService));
            DynamicRiftService.setInstanceProvider(provider(DynamicRiftService.class, dynamicRiftService));
            InstanceRiftService.setInstanceProvider(provider(InstanceRiftService.class, instanceRiftService));

            assertSame(vortexService, VortexService.getInstance());
            assertSame(beritraService, BeritraService.getInstance());
            assertSame(agentService, AgentService.getInstance());
            assertSame(anohaService, AnohaService.getInstance());
            assertSame(svsService, SvsService.getInstance());
            assertSame(rvrService, RvrService.getInstance());
            assertSame(iuService, IuService.getInstance());
            assertSame(nightmareCircusService, NightmareCircusService.getInstance());
            assertSame(dynamicRiftService, DynamicRiftService.getInstance());
            assertSame(instanceRiftService, InstanceRiftService.getInstance());
        } finally {
            VortexService.setInstanceProvider(null);
            BeritraService.setInstanceProvider(null);
            AgentService.setInstanceProvider(null);
            AnohaService.setInstanceProvider(null);
            SvsService.setInstanceProvider(null);
            RvrService.setInstanceProvider(null);
            IuService.setInstanceProvider(null);
            NightmareCircusService.setInstanceProvider(null);
            DynamicRiftService.setInstanceProvider(null);
            InstanceRiftService.setInstanceProvider(null);
        }
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
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
