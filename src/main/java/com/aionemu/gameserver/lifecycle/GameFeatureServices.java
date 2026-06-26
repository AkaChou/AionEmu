package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AStationService;
import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.F2pService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.MotionLoggingService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RepurchaseService;
import com.aionemu.gameserver.services.ShieldService;
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
            ObjectProvider<CoalescenceService> coalescenceServiceProvider) {
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
    }

    @Override
    public void destroy() {
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
    }
}
