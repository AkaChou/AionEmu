package com.aionemu.gameserver.lifecycle;


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

/**
 * 功能服务静态绑定：把 Spring ObjectProvider 喂入 30 个玩法功能域服务自持的 fail-fast 单例通道。
 * Feature service static bindings: feeds the Spring ObjectProviders into the fail-fast singleton
 * accessors held by the thirty gameplay feature services.
 * <p>本组件取代已退役的功能服务静态定位器（R3 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired feature-service static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class FeatureServiceBindings implements DisposableBean {

    /**
     * 构造并注册各功能服务实例提供者。
     * Construct and register instance providers for feature services.
     * @param disputeLandServiceProvider 争议之地服务提供者 / Dispute-land service provider
     * @param dredgionServiceProvider 钢铁之战服务提供者 / Dredgion service provider
     * @param asyunatarServiceProvider 阿修那塔服务提供者 / Asyunatar service provider
     * @param playerLimitServiceProvider 玩家限制服务提供者 / Player-limit service provider
     * @param npcShoutsServiceProvider NPC 喊话服务提供者 / NPC-shouts service provider
     * @param shieldServiceProvider 护盾服务提供者 / Shield service provider
     * @param rewardServiceProvider 奖励服务提供者 / Reward service provider
     * @param veteranRewardsServiceProvider 老兵奖励服务提供者 / Veteran-rewards service provider
     * @param protectorConquerorServiceProvider 保护者征服者服务提供者 / Protector-conqueror service provider
     * @param ffaServiceProvider FFA 服务提供者 / FFA service provider
     * @param ladderServiceProvider 天梯服务提供者 / Ladder service provider
     * @param bgServiceProvider 战场服务提供者 / BG service provider
     * @param banditServiceProvider 土匪服务提供者 / Bandit service provider
     * @param siegeServiceProvider 攻城服务提供者 / Siege service provider
     * @param baseServiceProvider 基地服务提供者 / Base service provider
     * @param aStationServiceProvider A 站服务提供者 / A-Station service provider
     * @param f2pServiceProvider F2P 服务提供者 / F2P service provider
     * @param windyGorgeServiceProvider 风之峡谷服务提供者 / Windy-gorge service provider
     * @param motionLoggingServiceProvider 动作日志服务提供者 / Motion-logging service provider
     * @param staticDoorServiceProvider 静态门服务提供者 / Static-door service provider
     * @param kiskServiceProvider 基斯克服务提供者 / Kisk service provider
     * @param repurchaseServiceProvider 回购服务提供者 / Repurchase service provider
     * @param dropDistributionServiceProvider 掉落分配服务提供者 / Drop-distribution service provider
     * @param systemMailServiceProvider 系统邮件服务提供者 / System-mail service provider
     * @param bonusServiceProvider 加成服务提供者 / Bonus service provider
     * @param petServiceProvider 宠物服务提供者 / Pet service provider
     * @param arcadeUpgradeServiceProvider 街机升级服务提供者 / Arcade-upgrade service provider
     * @param atreianBestiaryServiceProvider 阿特里亚图鉴服务提供者 / Atreian-bestiary service provider
     * @param coalescenceServiceProvider 融合服务提供者 / Coalescence service provider
     * @param growthEnergyProvider 成长能量提供者 / Growth-energy provider
     */
    public FeatureServiceBindings(ObjectProvider<DisputeLandService> disputeLandServiceProvider,
            ObjectProvider<DredgionService2> dredgionServiceProvider,
            ObjectProvider<AsyunatarService> asyunatarServiceProvider,
            ObjectProvider<PlayerLimitService> playerLimitServiceProvider,
            ObjectProvider<NpcShoutsService> npcShoutsServiceProvider,
            ObjectProvider<ShieldService> shieldServiceProvider,
            ObjectProvider<RewardService> rewardServiceProvider,
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
        DisputeLandService.setInstanceProvider(disputeLandServiceProvider);
        DredgionService2.setInstanceProvider(dredgionServiceProvider);
        AsyunatarService.setInstanceProvider(asyunatarServiceProvider);
        PlayerLimitService.setInstanceProvider(playerLimitServiceProvider);
        NpcShoutsService.setInstanceProvider(npcShoutsServiceProvider);
        ShieldService.setInstanceProvider(shieldServiceProvider);
        RewardService.setInstanceProvider(rewardServiceProvider);
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

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        DisputeLandService.setInstanceProvider(null);
        DredgionService2.setInstanceProvider(null);
        AsyunatarService.setInstanceProvider(null);
        PlayerLimitService.setInstanceProvider(null);
        NpcShoutsService.setInstanceProvider(null);
        ShieldService.setInstanceProvider(null);
        RewardService.setInstanceProvider(null);
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
