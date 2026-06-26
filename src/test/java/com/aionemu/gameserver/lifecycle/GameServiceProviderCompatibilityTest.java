package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameServiceProviderCompatibilityTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void singletonAccessorsUseSpringProvidersBeforeLegacyFallbacks() {
        GeoService geoService = instance(GeoService.class);
        NavService navService = instance(NavService.class);
        DropRegistrationService dropRegistrationService = instance(DropRegistrationService.class);
        LandingUpdateService landingUpdateService = instance(LandingUpdateService.class);
        AbyssLandingSpecialService abyssLandingSpecialService = instance(AbyssLandingSpecialService.class);
        DisputeLandService disputeLandService = instance(DisputeLandService.class);
        DredgionService2 dredgionService = instance(DredgionService2.class);
        AsyunatarService asyunatarService = instance(AsyunatarService.class);
        ShugoImperialTombSpawnManager shugoImperialTombSpawnManager = instance(ShugoImperialTombSpawnManager.class);
        SeasonRankingUpdateService seasonRankingUpdateService = instance(SeasonRankingUpdateService.class);

        try {
            GeoService.setInstanceProvider(provider(GeoService.class, geoService));
            NavService.setInstanceProvider(provider(NavService.class, navService));
            DropRegistrationService.setInstanceProvider(provider(DropRegistrationService.class, dropRegistrationService));
            LandingUpdateService.setInstanceProvider(provider(LandingUpdateService.class, landingUpdateService));
            AbyssLandingSpecialService.setInstanceProvider(provider(AbyssLandingSpecialService.class, abyssLandingSpecialService));
            DisputeLandService.setInstanceProvider(provider(DisputeLandService.class, disputeLandService));
            DredgionService2.setInstanceProvider(provider(DredgionService2.class, dredgionService));
            AsyunatarService.setInstanceProvider(provider(AsyunatarService.class, asyunatarService));
            ShugoImperialTombSpawnManager.setInstanceProvider(provider(ShugoImperialTombSpawnManager.class, shugoImperialTombSpawnManager));
            SeasonRankingUpdateService.setInstanceProvider(provider(SeasonRankingUpdateService.class, seasonRankingUpdateService));

            assertSame(geoService, GeoService.getInstance());
            assertSame(navService, NavService.getInstance());
            assertSame(dropRegistrationService, DropRegistrationService.getInstance());
            assertSame(landingUpdateService, LandingUpdateService.getInstance());
            assertSame(abyssLandingSpecialService, AbyssLandingSpecialService.getInstance());
            assertSame(disputeLandService, DisputeLandService.getInstance());
            assertSame(dredgionService, DredgionService2.getInstance());
            assertSame(asyunatarService, AsyunatarService.getInstance());
            assertSame(shugoImperialTombSpawnManager, ShugoImperialTombSpawnManager.getInstance());
            assertSame(seasonRankingUpdateService, SeasonRankingUpdateService.getInstance());
        } finally {
            GeoService.setInstanceProvider(null);
            NavService.setInstanceProvider(null);
            DropRegistrationService.setInstanceProvider(null);
            LandingUpdateService.setInstanceProvider(null);
            AbyssLandingSpecialService.setInstanceProvider(null);
            DisputeLandService.setInstanceProvider(null);
            DredgionService2.setInstanceProvider(null);
            AsyunatarService.setInstanceProvider(null);
            ShugoImperialTombSpawnManager.setInstanceProvider(null);
            SeasonRankingUpdateService.setInstanceProvider(null);
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
}
