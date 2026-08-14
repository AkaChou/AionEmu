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
 * 功能服务集合 Spring 门面 / 静态访问桥：注册并解析大量玩法功能服务实例。
 * static access bridge: registers and resolves many gameplay feature service instances.
 */
@Component
public final class GameFeatureServices implements DisposableBean {

    /** NPC 喊话服务提供者 / NPC-shouts service provider. */
    private static volatile ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;
    /** 争议之地服务提供者 / Dispute-land service provider. */
    private static volatile ObjectProvider<DisputeLandService> disputeLandServiceProvider;
    /** 钢铁之战服务提供者 / Dredgion service provider. */
    private static volatile ObjectProvider<DredgionService2> dredgionServiceProvider;
    /** 阿修那塔服务提供者 / Asyunatar service provider. */
    private static volatile ObjectProvider<AsyunatarService> asyunatarServiceProvider;
    /** 护盾服务提供者 / Shield service provider. */
    private static volatile ObjectProvider<ShieldService> shieldServiceProvider;
    /** 奖励服务提供者 / Reward service provider. */
    private static volatile ObjectProvider<RewardService> rewardServiceProvider;
    /** 保护者征服者服务提供者 / Protector-conqueror service provider. */
    private static volatile ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;
    /** FFA 服务提供者 / FFA service provider */
    private static volatile ObjectProvider<FFAService> ffaServiceProvider;
    /** 天梯服务提供者 / Ladder service provider. */
    private static volatile ObjectProvider<LadderService> ladderServiceProvider;
    /** 攻城服务提供者 / Siege service provider. */
    private static volatile ObjectProvider<SiegeService> siegeServiceProvider;
    private static volatile SiegeService resolvedSiegeService;
    /** 基地服务提供者 / Base service provider. */
    private static volatile ObjectProvider<BaseService> baseServiceProvider;
    /** A-Station 服务提供者 / A-Station service provider */
    private static volatile ObjectProvider<AStationService> aStationServiceProvider;
    /** F2P 服务提供者 / F2P service provider. */
    private static volatile ObjectProvider<F2pService> f2pServiceProvider;
    /** 风之峡谷服务提供者 / Windy-gorge service provider. */
    private static volatile ObjectProvider<WindyGorgeService> windyGorgeServiceProvider;
    /** 动作日志服务提供者 / Motion-logging service provider. */
    private static volatile ObjectProvider<MotionLoggingService> motionLoggingServiceProvider;
    /** 基斯克服务提供者 / Kisk service provider. */
    private static volatile ObjectProvider<KiskService> kiskServiceProvider;
    /** 回购服务提供者 / Repurchase service provider. */
    private static volatile ObjectProvider<RepurchaseService> repurchaseServiceProvider;
    /** 掉落分配服务提供者 / Drop-distribution service provider. */
    private static volatile ObjectProvider<DropDistributionService> dropDistributionServiceProvider;
    /** 系统邮件服务提供者 / System-mail service provider. */
    private static volatile ObjectProvider<SystemMailService> systemMailServiceProvider;
    /** 加成服务提供者 / Bonus service provider. */
    private static volatile ObjectProvider<BonusService> bonusServiceProvider;
    /** 土匪服务提供者 / Bandit service provider. */
    private static volatile ObjectProvider<BanditService> banditServiceProvider;
    /** 静态门服务提供者 / Static-door service provider. */
    private static volatile ObjectProvider<StaticDoorService> staticDoorServiceProvider;
    /** 宠物服务提供者 / Pet service provider. */
    private static volatile ObjectProvider<PetService> petServiceProvider;
    /** 街机升级服务提供者 / Arcade-upgrade service provider. */
    private static volatile ObjectProvider<ArcadeUpgradeService> arcadeUpgradeServiceProvider;
    /** 阿特里亚图鉴服务提供者 / Atreian-bestiary service provider. */
    private static volatile ObjectProvider<AtreianBestiaryService> atreianBestiaryServiceProvider;
    /** 融合服务提供者 / Coalescence service provider. */
    private static volatile ObjectProvider<CoalescenceService> coalescenceServiceProvider;
    /** 成长能量提供者 / Growth-energy provider. */
    private static volatile ObjectProvider<GrowthEnergy> growthEnergyProvider;

    /**
     * 构造并注册各功能服务实例提供者。
     * Construct and register instance providers for feature services.
     *
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
    public GameFeatureServices(ObjectProvider<DisputeLandService> disputeLandServiceProvider,
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
        GameFeatureServices.npcShoutsServiceProvider = npcShoutsServiceProvider;
        GameFeatureServices.disputeLandServiceProvider = disputeLandServiceProvider;
        GameFeatureServices.dredgionServiceProvider = dredgionServiceProvider;
        GameFeatureServices.asyunatarServiceProvider = asyunatarServiceProvider;
        GameFeatureServices.shieldServiceProvider = shieldServiceProvider;
        GameFeatureServices.rewardServiceProvider = rewardServiceProvider;
        GameFeatureServices.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
        GameFeatureServices.ffaServiceProvider = ffaServiceProvider;
        GameFeatureServices.ladderServiceProvider = ladderServiceProvider;
        GameFeatureServices.siegeServiceProvider = siegeServiceProvider;
        resolvedSiegeService = null;
        GameFeatureServices.baseServiceProvider = baseServiceProvider;
        GameFeatureServices.aStationServiceProvider = aStationServiceProvider;
        GameFeatureServices.f2pServiceProvider = f2pServiceProvider;
        GameFeatureServices.windyGorgeServiceProvider = windyGorgeServiceProvider;
        GameFeatureServices.motionLoggingServiceProvider = motionLoggingServiceProvider;
        GameFeatureServices.kiskServiceProvider = kiskServiceProvider;
        GameFeatureServices.repurchaseServiceProvider = repurchaseServiceProvider;
        GameFeatureServices.dropDistributionServiceProvider = dropDistributionServiceProvider;
        GameFeatureServices.systemMailServiceProvider = systemMailServiceProvider;
        GameFeatureServices.bonusServiceProvider = bonusServiceProvider;
        GameFeatureServices.banditServiceProvider = banditServiceProvider;
        GameFeatureServices.staticDoorServiceProvider = staticDoorServiceProvider;
        GameFeatureServices.petServiceProvider = petServiceProvider;
        GameFeatureServices.arcadeUpgradeServiceProvider = arcadeUpgradeServiceProvider;
        GameFeatureServices.atreianBestiaryServiceProvider = atreianBestiaryServiceProvider;
        GameFeatureServices.coalescenceServiceProvider = coalescenceServiceProvider;
        GameFeatureServices.growthEnergyProvider = growthEnergyProvider;
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
     * 解析 NPC 喊话服务。
     * Resolve the NPC-shouts service.
     *
     * @return NPC 喊话服务 / NPC-shouts service
     */
    public static NpcShoutsService npcShoutsService() {
        ObjectProvider<NpcShoutsService> provider = npcShoutsServiceProvider;
        if (provider == null) {
            return NpcShoutsService.getInstance();
        }
        return provider.getIfAvailable(NpcShoutsService::getInstance);
    }

    /**
     * 解析钢铁之战服务。
     * Resolve the Dredgion service.
     *
     * @return 钢铁之战服务 / Dredgion service
     */
    public static DredgionService2 dredgionService() {
        return getIfAvailable(dredgionServiceProvider, DredgionService2::getInstance);
    }

    /**
     * 解析争议之地服务。
     * Resolve the dispute-land service.
     *
     * @return 争议之地服务 / Dispute-land service
     */
    public static DisputeLandService disputeLandService() {
        return getIfAvailable(disputeLandServiceProvider, DisputeLandService::getInstance);
    }

    /**
     * 解析阿修那塔服务。
     * Resolve the Asyunatar service.
     *
     * @return 阿修那塔服务 / Asyunatar service
     */
    public static AsyunatarService asyunatarService() {
        return getIfAvailable(asyunatarServiceProvider, AsyunatarService::getInstance);
    }

    /**
     * 解析护盾服务。
     * Resolve the shield service.
     *
     * @return 护盾服务 / Shield service
     */
    public static ShieldService shieldService() {
        return getIfAvailable(shieldServiceProvider, ShieldService::getInstance);
    }

    /**
     * 解析奖励服务。
     * Resolve the reward service.
     *
     * @return 奖励服务 / Reward service
     */
    public static RewardService rewardService() {
        return getIfAvailable(rewardServiceProvider, RewardService::getInstance);
    }

    /**
     * 解析保护者征服者服务。
     * Resolve the protector-conqueror service.
     *
     * @return 保护者征服者服务 / Protector-conqueror service
     */
    public static ProtectorConquerorService protectorConquerorService() {
        return getIfAvailable(protectorConquerorServiceProvider, ProtectorConquerorService::getInstance);
    }

    /**
     * 解析 FFA 服务。
     * Resolve the FFA service.
     *
     * @return FFA 服务 / FFA service
     */
    public static FFAService ffaService() {
        ObjectProvider<FFAService> provider = ffaServiceProvider;
        if (provider == null) {
            return FFAService.getInstance();
        }
        return provider.getIfAvailable(FFAService::getInstance);
    }

    /**
     * 解析天梯服务。
     * Resolve the ladder service.
     *
     * @return 天梯服务 / Ladder service
     */
    public static LadderService ladderService() {
        ObjectProvider<LadderService> provider = ladderServiceProvider;
        if (provider == null) {
            return LadderService.getInstance();
        }
        return provider.getIfAvailable(LadderService::getInstance);
    }

    /**
     * 解析攻城服务。
     * Resolve the siege service.
     *
     * @return 攻城服务 / Siege service
     */
    public static SiegeService siegeService() {
        SiegeService resolved = resolvedSiegeService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<SiegeService> provider = siegeServiceProvider;
        resolved = provider == null ? SiegeService.getInstance() : provider.getIfAvailable(SiegeService::getInstance);
        resolvedSiegeService = resolved;
        return resolved;
    }

    /**
     * 解析基地服务。
     * Resolve the base service.
     *
     * @return 基地服务 / Base service
     */
    public static BaseService baseService() {
        ObjectProvider<BaseService> provider = baseServiceProvider;
        if (provider == null) {
            return BaseService.getInstance();
        }
        return provider.getIfAvailable(BaseService::getInstance);
    }

    /**
     * 解析 A 站服务。
     * Resolve the A-Station service.
     *
     * @return A 站服务 / A-Station service
     */
    public static AStationService aStationService() {
        return getIfAvailable(aStationServiceProvider, AStationService::getInstance);
    }

    /**
     * 解析 F2P 服务。
     * Resolve the F2P service.
     *
     * @return F2P 服务 / F2P service
     */
    public static F2pService f2pService() {
        return getIfAvailable(f2pServiceProvider, F2pService::getInstance);
    }

    /**
     * 解析风之峡谷服务。
     * Resolve the windy-gorge service.
     *
     * @return 风之峡谷服务 / Windy-gorge service
     */
    public static WindyGorgeService windyGorgeService() {
        return getIfAvailable(windyGorgeServiceProvider, WindyGorgeService::getInstance);
    }

    /**
     * 解析动作日志服务。
     * Resolve the motion-logging service.
     *
     * @return 动作日志服务 / Motion-logging service
     */
    public static MotionLoggingService motionLoggingService() {
        return getIfAvailable(motionLoggingServiceProvider, MotionLoggingService::getInstance);
    }

    /**
     * 解析基斯克服务。
     * Resolve the Kisk service.
     *
     * @return 基斯克服务 / Kisk service
     */
    public static KiskService kiskService() {
        ObjectProvider<KiskService> provider = kiskServiceProvider;
        if (provider == null) {
            return KiskService.getInstance();
        }
        return provider.getIfAvailable(KiskService::getInstance);
    }

    /**
     * 解析回购服务。
     * Resolve the repurchase service.
     *
     * @return 回购服务 / Repurchase service
     */
    public static RepurchaseService repurchaseService() {
        return getIfAvailable(repurchaseServiceProvider, RepurchaseService::getInstance);
    }

    /**
     * 解析掉落分配服务。
     * Resolve the drop-distribution service.
     *
     * @return 掉落分配服务 / Drop-distribution service
     */
    public static DropDistributionService dropDistributionService() {
        return getIfAvailable(dropDistributionServiceProvider, DropDistributionService::getInstance);
    }

    /**
     * 解析土匪服务。
     * Resolve the bandit service.
     *
     * @return 土匪服务 / Bandit service
     */
    public static BanditService banditService() {
        return getIfAvailable(banditServiceProvider, BanditService::getInstance);
    }

    /**
     * 解析静态门服务。
     * Resolve the static-door service.
     *
     * @return 静态门服务 / Static-door service
     */
    public static StaticDoorService staticDoorService() {
        return getIfAvailable(staticDoorServiceProvider, StaticDoorService::getInstance);
    }

    /**
     * 解析系统邮件服务。
     * Resolve the system-mail service.
     *
     * @return 系统邮件服务 / System-mail service
     */
    public static SystemMailService systemMailService() {
        ObjectProvider<SystemMailService> provider = systemMailServiceProvider;
        if (provider == null) {
            return SystemMailService.getInstance();
        }
        return provider.getIfAvailable(SystemMailService::getInstance);
    }

    /**
     * 解析加成服务。
     * Resolve the bonus service.
     *
     * @return 加成服务 / Bonus service
     */
    public static BonusService bonusService() {
        return getIfAvailable(bonusServiceProvider, BonusService::getInstance);
    }

    /**
     * 解析宠物服务。
     * Resolve the pet service.
     *
     * @return 宠物服务 / Pet service
     */
    public static PetService petService() {
        ObjectProvider<PetService> provider = petServiceProvider;
        if (provider == null) {
            return PetService.getInstance();
        }
        return provider.getIfAvailable(PetService::getInstance);
    }

    /**
     * 解析街机升级服务。
     * Resolve the arcade-upgrade service.
     *
     * @return 街机升级服务 / Arcade-upgrade service
     */
    public static ArcadeUpgradeService arcadeUpgradeService() {
        return getIfAvailable(arcadeUpgradeServiceProvider, ArcadeUpgradeService::getInstance);
    }

    /**
     * 解析阿特里亚图鉴服务。
     * Resolve the Atreian-bestiary service.
     *
     * @return 阿特里亚图鉴服务 / Atreian-bestiary service
     */
    public static AtreianBestiaryService atreianBestiaryService() {
        return getIfAvailable(atreianBestiaryServiceProvider, AtreianBestiaryService::getInstance);
    }

    /**
     * 解析融合服务。
     * Resolve the coalescence service.
     *
     * @return 融合服务 / Coalescence service
     */
    public static CoalescenceService coalescenceService() {
        return getIfAvailable(coalescenceServiceProvider, CoalescenceService::getInstance);
    }

    /**
     * 解析成长能量。
     * Resolve growth energy.
     *
     * @return 成长能量 / Growth energy
     */
    public static GrowthEnergy growthEnergy() {
        return getIfAvailable(growthEnergyProvider, GrowthEnergy::getInstance);
    }

    /**
     * 优先从 Spring 提供者取实例，否则使用回退供应器。
     * Prefer the Spring provider instance, otherwise use the fallback supplier.
     *
     * @param provider Spring 提供者 / Spring provider
     * @param fallback 回退供应器 / Fallback supplier
     * @param <T> 服务类型 / Service type
     * @return 服务实例 / Service instance
     */
    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
    @Override
    public void destroy() {
        npcShoutsServiceProvider = null;
        disputeLandServiceProvider = null;
        dredgionServiceProvider = null;
        asyunatarServiceProvider = null;
        shieldServiceProvider = null;
        rewardServiceProvider = null;
        protectorConquerorServiceProvider = null;
        ffaServiceProvider = null;
        ladderServiceProvider = null;
        siegeServiceProvider = null;
        resolvedSiegeService = null;
        baseServiceProvider = null;
        aStationServiceProvider = null;
        f2pServiceProvider = null;
        windyGorgeServiceProvider = null;
        motionLoggingServiceProvider = null;
        kiskServiceProvider = null;
        repurchaseServiceProvider = null;
        dropDistributionServiceProvider = null;
        systemMailServiceProvider = null;
        bonusServiceProvider = null;
        banditServiceProvider = null;
        staticDoorServiceProvider = null;
        petServiceProvider = null;
        arcadeUpgradeServiceProvider = null;
        atreianBestiaryServiceProvider = null;
        coalescenceServiceProvider = null;
        growthEnergyProvider = null;
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
