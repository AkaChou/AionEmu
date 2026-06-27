package com.aionemu.gameserver.lifecycle;

import java.util.function.Supplier;

import com.aionemu.gameserver.services.AStationService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.F2pService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RepurchaseService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.StaticDoorService;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.WindyGorgeService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.item.CoalescenceService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameFeatureServices implements DisposableBean {

    private static volatile ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;
    private static volatile ObjectProvider<DisputeLandService> disputeLandServiceProvider;
    private static volatile ObjectProvider<DredgionService2> dredgionServiceProvider;
    private static volatile ObjectProvider<AsyunatarService> asyunatarServiceProvider;
    private static volatile ObjectProvider<ShieldService> shieldServiceProvider;
    private static volatile ObjectProvider<WeddingService> weddingServiceProvider;
    private static volatile ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;
    private static volatile ObjectProvider<FFAService> ffaServiceProvider;
    private static volatile ObjectProvider<LadderService> ladderServiceProvider;
    private static volatile ObjectProvider<SiegeService> siegeServiceProvider;
    private static volatile ObjectProvider<BaseService> baseServiceProvider;
    private static volatile ObjectProvider<AStationService> aStationServiceProvider;
    private static volatile ObjectProvider<MotionLoggingService> motionLoggingServiceProvider;
    private static volatile ObjectProvider<KiskService> kiskServiceProvider;
    private static volatile ObjectProvider<RepurchaseService> repurchaseServiceProvider;
    private static volatile ObjectProvider<DropDistributionService> dropDistributionServiceProvider;
    private static volatile ObjectProvider<SystemMailService> systemMailServiceProvider;
    private static volatile ObjectProvider<BanditService> banditServiceProvider;
    private static volatile ObjectProvider<StaticDoorService> staticDoorServiceProvider;
    private static volatile ObjectProvider<PetService> petServiceProvider;
    private static volatile ObjectProvider<ArcadeUpgradeService> arcadeUpgradeServiceProvider;
    private static volatile ObjectProvider<AtreianBestiaryService> atreianBestiaryServiceProvider;

    public GameFeatureServices(ObjectProvider<DisputeLandService> disputeLandServiceProvider,
            ObjectProvider<DredgionService2> dredgionServiceProvider,
            ObjectProvider<AsyunatarService> asyunatarServiceProvider,
            ObjectProvider<PlayerLimitService> playerLimitServiceProvider,
            ObjectProvider<NpcShoutsService> npcShoutsServiceProvider,
            ObjectProvider<ShieldService> shieldServiceProvider,
            ObjectProvider<RewardService> rewardServiceProvider,
            ObjectProvider<WeddingService> weddingServiceProvider,
            ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider,
            ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider,
            ObjectProvider<FFAService> ffaServiceProvider,
            ObjectProvider<LadderService> ladderServiceProvider,
            ObjectProvider<BGService> bgServiceProvider,
            ObjectProvider<BanditService> banditServiceProvider,
            ObjectProvider<SiegeService> siegeServiceProvider,
            ObjectProvider<BaseService> baseServiceProvider,
            ObjectProvider<AStationService> aStationServiceProvider,
            ObjectProvider<F2pService> f2pServiceProvider,
            ObjectProvider<WindyGorgeService> windyGorgeServiceProvider,
            ObjectProvider<MotionLoggingService> motionLoggingServiceProvider,
            ObjectProvider<StaticDoorService> staticDoorServiceProvider,
            ObjectProvider<KiskService> kiskServiceProvider,
            ObjectProvider<RepurchaseService> repurchaseServiceProvider,
            ObjectProvider<DropDistributionService> dropDistributionServiceProvider,
            ObjectProvider<SystemMailService> systemMailServiceProvider,
            ObjectProvider<BonusService> bonusServiceProvider,
            ObjectProvider<PetService> petServiceProvider,
            ObjectProvider<ArcadeUpgradeService> arcadeUpgradeServiceProvider,
            ObjectProvider<AtreianBestiaryService> atreianBestiaryServiceProvider,
            ObjectProvider<CoalescenceService> coalescenceServiceProvider,
            ObjectProvider<GrowthEnergy> growthEnergyProvider) {
        GameFeatureServices.npcShoutsServiceProvider = npcShoutsServiceProvider;
        GameFeatureServices.disputeLandServiceProvider = disputeLandServiceProvider;
        GameFeatureServices.dredgionServiceProvider = dredgionServiceProvider;
        GameFeatureServices.asyunatarServiceProvider = asyunatarServiceProvider;
        GameFeatureServices.shieldServiceProvider = shieldServiceProvider;
        GameFeatureServices.weddingServiceProvider = weddingServiceProvider;
        GameFeatureServices.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
        GameFeatureServices.ffaServiceProvider = ffaServiceProvider;
        GameFeatureServices.ladderServiceProvider = ladderServiceProvider;
        GameFeatureServices.siegeServiceProvider = siegeServiceProvider;
        GameFeatureServices.baseServiceProvider = baseServiceProvider;
        GameFeatureServices.aStationServiceProvider = aStationServiceProvider;
        GameFeatureServices.motionLoggingServiceProvider = motionLoggingServiceProvider;
        GameFeatureServices.kiskServiceProvider = kiskServiceProvider;
        GameFeatureServices.repurchaseServiceProvider = repurchaseServiceProvider;
        GameFeatureServices.dropDistributionServiceProvider = dropDistributionServiceProvider;
        GameFeatureServices.systemMailServiceProvider = systemMailServiceProvider;
        GameFeatureServices.banditServiceProvider = banditServiceProvider;
        GameFeatureServices.staticDoorServiceProvider = staticDoorServiceProvider;
        GameFeatureServices.petServiceProvider = petServiceProvider;
        GameFeatureServices.arcadeUpgradeServiceProvider = arcadeUpgradeServiceProvider;
        GameFeatureServices.atreianBestiaryServiceProvider = atreianBestiaryServiceProvider;
        DisputeLandService.setInstanceProvider(disputeLandServiceProvider);
        DredgionService2.setInstanceProvider(dredgionServiceProvider);
        AsyunatarService.setInstanceProvider(asyunatarServiceProvider);
        PlayerLimitService.setInstanceProvider(playerLimitServiceProvider);
        NpcShoutsService.setInstanceProvider(npcShoutsServiceProvider);
        ShieldService.setInstanceProvider(shieldServiceProvider);
        RewardService.setInstanceProvider(rewardServiceProvider);
        WeddingService.setInstanceProvider(weddingServiceProvider);
        VeteranRewardsService.setInstanceProvider(veteranRewardsServiceProvider);
        ProtectorConquerorService.setInstanceProvider(protectorConquerorServiceProvider);
        FFAService.setInstanceProvider(ffaServiceProvider);
        LadderService.setInstanceProvider(ladderServiceProvider);
        BGService.setInstanceProvider(bgServiceProvider);
        BanditService.setInstanceProvider(banditServiceProvider);
        SiegeService.setInstanceProvider(siegeServiceProvider);
        BaseService.setInstanceProvider(baseServiceProvider);
        AStationService.setInstanceProvider(aStationServiceProvider);
        F2pService.setInstanceProvider(f2pServiceProvider);
        WindyGorgeService.setInstanceProvider(windyGorgeServiceProvider);
        MotionLoggingService.setInstanceProvider(motionLoggingServiceProvider);
        StaticDoorService.setInstanceProvider(staticDoorServiceProvider);
        KiskService.setInstanceProvider(kiskServiceProvider);
        RepurchaseService.setInstanceProvider(repurchaseServiceProvider);
        DropDistributionService.setInstanceProvider(dropDistributionServiceProvider);
        SystemMailService.setInstanceProvider(systemMailServiceProvider);
        BonusService.setInstanceProvider(bonusServiceProvider);
        PetService.setInstanceProvider(petServiceProvider);
        ArcadeUpgradeService.setInstanceProvider(arcadeUpgradeServiceProvider);
        AtreianBestiaryService.setInstanceProvider(atreianBestiaryServiceProvider);
        CoalescenceService.setInstanceProvider(coalescenceServiceProvider);
        GrowthEnergy.setInstanceProvider(growthEnergyProvider);
    }

    public static NpcShoutsService npcShoutsService() {
        ObjectProvider<NpcShoutsService> provider = npcShoutsServiceProvider;
        if (provider == null) {
            return NpcShoutsService.getInstance();
        }
        return provider.getIfAvailable(NpcShoutsService::getInstance);
    }

    public static DredgionService2 dredgionService() {
        return getIfAvailable(dredgionServiceProvider, DredgionService2::getInstance);
    }

    public static DisputeLandService disputeLandService() {
        return getIfAvailable(disputeLandServiceProvider, DisputeLandService::getInstance);
    }

    public static AsyunatarService asyunatarService() {
        return getIfAvailable(asyunatarServiceProvider, AsyunatarService::getInstance);
    }

    public static ShieldService shieldService() {
        return getIfAvailable(shieldServiceProvider, ShieldService::getInstance);
    }

    public static WeddingService weddingService() {
        return getIfAvailable(weddingServiceProvider, WeddingService::getInstance);
    }

    public static ProtectorConquerorService protectorConquerorService() {
        return getIfAvailable(protectorConquerorServiceProvider, ProtectorConquerorService::getInstance);
    }

    public static FFAService ffaService() {
        ObjectProvider<FFAService> provider = ffaServiceProvider;
        if (provider == null) {
            return FFAService.getInstance();
        }
        return provider.getIfAvailable(FFAService::getInstance);
    }

    public static LadderService ladderService() {
        ObjectProvider<LadderService> provider = ladderServiceProvider;
        if (provider == null) {
            return LadderService.getInstance();
        }
        return provider.getIfAvailable(LadderService::getInstance);
    }

    public static SiegeService siegeService() {
        ObjectProvider<SiegeService> provider = siegeServiceProvider;
        if (provider == null) {
            return SiegeService.getInstance();
        }
        return provider.getIfAvailable(SiegeService::getInstance);
    }

    public static BaseService baseService() {
        ObjectProvider<BaseService> provider = baseServiceProvider;
        if (provider == null) {
            return BaseService.getInstance();
        }
        return provider.getIfAvailable(BaseService::getInstance);
    }

    public static AStationService aStationService() {
        return getIfAvailable(aStationServiceProvider, AStationService::getInstance);
    }

    public static MotionLoggingService motionLoggingService() {
        return getIfAvailable(motionLoggingServiceProvider, MotionLoggingService::getInstance);
    }

    public static KiskService kiskService() {
        ObjectProvider<KiskService> provider = kiskServiceProvider;
        if (provider == null) {
            return KiskService.getInstance();
        }
        return provider.getIfAvailable(KiskService::getInstance);
    }

    public static RepurchaseService repurchaseService() {
        return getIfAvailable(repurchaseServiceProvider, RepurchaseService::getInstance);
    }

    public static DropDistributionService dropDistributionService() {
        return getIfAvailable(dropDistributionServiceProvider, DropDistributionService::getInstance);
    }

    public static BanditService banditService() {
        return getIfAvailable(banditServiceProvider, BanditService::getInstance);
    }

    public static StaticDoorService staticDoorService() {
        return getIfAvailable(staticDoorServiceProvider, StaticDoorService::getInstance);
    }

    public static SystemMailService systemMailService() {
        ObjectProvider<SystemMailService> provider = systemMailServiceProvider;
        if (provider == null) {
            return SystemMailService.getInstance();
        }
        return provider.getIfAvailable(SystemMailService::getInstance);
    }

    public static PetService petService() {
        ObjectProvider<PetService> provider = petServiceProvider;
        if (provider == null) {
            return PetService.getInstance();
        }
        return provider.getIfAvailable(PetService::getInstance);
    }

    public static ArcadeUpgradeService arcadeUpgradeService() {
        return getIfAvailable(arcadeUpgradeServiceProvider, ArcadeUpgradeService::getInstance);
    }

    public static AtreianBestiaryService atreianBestiaryService() {
        return getIfAvailable(atreianBestiaryServiceProvider, AtreianBestiaryService::getInstance);
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    @Override
    public void destroy() {
        npcShoutsServiceProvider = null;
        disputeLandServiceProvider = null;
        dredgionServiceProvider = null;
        asyunatarServiceProvider = null;
        shieldServiceProvider = null;
        weddingServiceProvider = null;
        protectorConquerorServiceProvider = null;
        ffaServiceProvider = null;
        ladderServiceProvider = null;
        siegeServiceProvider = null;
        baseServiceProvider = null;
        aStationServiceProvider = null;
        motionLoggingServiceProvider = null;
        kiskServiceProvider = null;
        repurchaseServiceProvider = null;
        dropDistributionServiceProvider = null;
        systemMailServiceProvider = null;
        banditServiceProvider = null;
        staticDoorServiceProvider = null;
        petServiceProvider = null;
        arcadeUpgradeServiceProvider = null;
        atreianBestiaryServiceProvider = null;
        DisputeLandService.setInstanceProvider(null);
        DredgionService2.setInstanceProvider(null);
        AsyunatarService.setInstanceProvider(null);
        PlayerLimitService.setInstanceProvider(null);
        NpcShoutsService.setInstanceProvider(null);
        ShieldService.setInstanceProvider(null);
        RewardService.setInstanceProvider(null);
        WeddingService.setInstanceProvider(null);
        VeteranRewardsService.setInstanceProvider(null);
        ProtectorConquerorService.setInstanceProvider(null);
        FFAService.setInstanceProvider(null);
        LadderService.setInstanceProvider(null);
        BGService.setInstanceProvider(null);
        BanditService.setInstanceProvider(null);
        SiegeService.setInstanceProvider(null);
        BaseService.setInstanceProvider(null);
        AStationService.setInstanceProvider(null);
        F2pService.setInstanceProvider(null);
        WindyGorgeService.setInstanceProvider(null);
        MotionLoggingService.setInstanceProvider(null);
        StaticDoorService.setInstanceProvider(null);
        KiskService.setInstanceProvider(null);
        RepurchaseService.setInstanceProvider(null);
        DropDistributionService.setInstanceProvider(null);
        SystemMailService.setInstanceProvider(null);
        BonusService.setInstanceProvider(null);
        PetService.setInstanceProvider(null);
        ArcadeUpgradeService.setInstanceProvider(null);
        AtreianBestiaryService.setInstanceProvider(null);
        CoalescenceService.setInstanceProvider(null);
        GrowthEnergy.setInstanceProvider(null);
    }
}
