package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 功能服务运行时桥接：解析启动阶段所需的功能服务实例。
 * Feature-services runtime bridge: resolves feature service instances needed during startup.
 */
@Component
public class GameFeatureServicesRuntimeBridge {

    /** 玩家限制服务提供者 / Player-limit service provider. */
    private ObjectProvider<PlayerLimitService> playerLimitServiceProvider;
    /** NPC 喊话服务提供者 / NPC-shouts service provider. */
    private ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;
    /** 护盾服务提供者 / Shield service provider. */
    private ObjectProvider<ShieldService> shieldServiceProvider;
    /** 奖励服务提供者 / Reward service provider. */
    private ObjectProvider<RewardService> rewardServiceProvider;
    /** 老兵奖励服务提供者 / Veteran-rewards service provider. */
    private ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider;
    /** 争议之地服务提供者 / Dispute-land service provider. */
    private ObjectProvider<DisputeLandService> disputeLandServiceProvider;
    /** 前哨服务提供者 / Outpost service provider. */
    private ObjectProvider<OutpostService> outpostServiceProvider;
    /** 保护者征服者服务提供者 / Protector-conqueror service provider. */
    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;
    /** 钢铁之战服务提供者 / Dredgion service provider. */
    private ObjectProvider<DredgionService2> dredgionServiceProvider;
    /** 阿修那塔服务提供者 / Asyunatar service provider. */
    private ObjectProvider<AsyunatarService> asyunatarServiceProvider;
    /** FFA 服务提供者 / FFA service provider */
    private ObjectProvider<FFAService> ffaServiceProvider;
    /** 天梯服务提供者 / Ladder service provider. */
    private ObjectProvider<LadderService> ladderServiceProvider;
    /** 战场服务提供者 / BG service provider. */
    private ObjectProvider<BGService> bgServiceProvider;
    /** 土匪服务提供者 / Bandit service provider. */
    private ObjectProvider<BanditService> banditServiceProvider;
    /** 攻城服务提供者 / Siege service provider. */
    private ObjectProvider<SiegeService> siegeServiceProvider;
    /** 基地服务提供者 / Base service provider. */
    private ObjectProvider<BaseService> baseServiceProvider;

    /**
     * 可选注入玩家限制服务提供者。
     * Optionally inject the player-limit service provider.
     *
     * @param playerLimitServiceProvider 玩家限制服务提供者 / Player-limit service provider
     */
    @Autowired(required = false)
    void setPlayerLimitServiceProvider(ObjectProvider<PlayerLimitService> playerLimitServiceProvider) {
        this.playerLimitServiceProvider = playerLimitServiceProvider;
    }

    /**
     * 可选注入 NPC 喊话服务提供者。
     * Optionally inject the NPC-shouts service provider.
     *
     * @param npcShoutsServiceProvider NPC 喊话服务提供者 / NPC-shouts service provider
     */
    @Autowired(required = false)
    void setNpcShoutsServiceProvider(ObjectProvider<NpcShoutsService> npcShoutsServiceProvider) {
        this.npcShoutsServiceProvider = npcShoutsServiceProvider;
    }

    /**
     * 可选注入护盾服务提供者。
     * Optionally inject the shield service provider.
     *
     * @param shieldServiceProvider 护盾服务提供者 / Shield service provider
     */
    @Autowired(required = false)
    void setShieldServiceProvider(ObjectProvider<ShieldService> shieldServiceProvider) {
        this.shieldServiceProvider = shieldServiceProvider;
    }

    /**
     * 可选注入奖励服务提供者。
     * Optionally inject the reward service provider.
     *
     * @param rewardServiceProvider 奖励服务提供者 / Reward service provider
     */
    @Autowired(required = false)
    void setRewardServiceProvider(ObjectProvider<RewardService> rewardServiceProvider) {
        this.rewardServiceProvider = rewardServiceProvider;
    }

    /**
     * 可选注入老兵奖励服务提供者。
     * Optionally inject the veteran-rewards service provider.
     *
     * @param veteranRewardsServiceProvider 老兵奖励服务提供者 / Veteran-rewards service provider
     */
    @Autowired(required = false)
    void setVeteranRewardsServiceProvider(ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider) {
        this.veteranRewardsServiceProvider = veteranRewardsServiceProvider;
    }

    /**
     * 可选注入争议之地服务提供者。
     * Optionally inject the dispute-land service provider.
     *
     * @param disputeLandServiceProvider 争议之地服务提供者 / Dispute-land service provider
     */
    @Autowired(required = false)
    void setDisputeLandServiceProvider(ObjectProvider<DisputeLandService> disputeLandServiceProvider) {
        this.disputeLandServiceProvider = disputeLandServiceProvider;
    }

    /**
     * 可选注入前哨服务提供者。
     * Optionally inject the outpost service provider.
     *
     * @param outpostServiceProvider 前哨服务提供者 / Outpost service provider
     */
    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
    }

    /**
     * 可选注入保护者征服者服务提供者。
     * Optionally inject the protector-conqueror service provider.
     *
     * @param protectorConquerorServiceProvider 保护者征服者服务提供者 / Protector-conqueror service provider
     */
    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
    }

    /**
     * 可选注入钢铁之战服务提供者。
     * Optionally inject the Dredgion service provider.
     *
     * @param dredgionServiceProvider 钢铁之战服务提供者 / Dredgion service provider
     */
    @Autowired(required = false)
    void setDredgionServiceProvider(ObjectProvider<DredgionService2> dredgionServiceProvider) {
        this.dredgionServiceProvider = dredgionServiceProvider;
    }

    /**
     * 可选注入阿修那塔服务提供者。
     * Optionally inject the Asyunatar service provider.
     *
     * @param asyunatarServiceProvider 阿修那塔服务提供者 / Asyunatar service provider
     */
    @Autowired(required = false)
    void setAsyunatarServiceProvider(ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        this.asyunatarServiceProvider = asyunatarServiceProvider;
    }

    /**
     * 可选注入 FFA 服务提供者。
     * Optionally inject the FFA service provider.
     *
     * @param ffaServiceProvider FFA 服务提供者 / FFA service provider
     */
    @Autowired(required = false)
    void setFfaServiceProvider(ObjectProvider<FFAService> ffaServiceProvider) {
        this.ffaServiceProvider = ffaServiceProvider;
    }

    /**
     * 可选注入天梯服务提供者。
     * Optionally inject the ladder service provider.
     *
     * @param ladderServiceProvider 天梯服务提供者 / Ladder service provider
     */
    @Autowired(required = false)
    void setLadderServiceProvider(ObjectProvider<LadderService> ladderServiceProvider) {
        this.ladderServiceProvider = ladderServiceProvider;
    }

    /**
     * 可选注入战场服务提供者。
     * Optionally inject the BG service provider.
     *
     * @param bgServiceProvider 战场服务提供者 / BG service provider
     */
    @Autowired(required = false)
    void setBgServiceProvider(ObjectProvider<BGService> bgServiceProvider) {
        this.bgServiceProvider = bgServiceProvider;
    }

    /**
     * 可选注入土匪服务提供者。
     * Optionally inject the bandit service provider.
     *
     * @param banditServiceProvider 土匪服务提供者 / Bandit service provider
     */
    @Autowired(required = false)
    void setBanditServiceProvider(ObjectProvider<BanditService> banditServiceProvider) {
        this.banditServiceProvider = banditServiceProvider;
    }

    /**
     * 可选注入攻城服务提供者。
     * Optionally inject the siege service provider.
     *
     * @param siegeServiceProvider 攻城服务提供者 / Siege service provider
     */
    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    /**
     * 可选注入基地服务提供者。
     * Optionally inject the base service provider.
     *
     * @param baseServiceProvider 基地服务提供者 / Base service provider
     */
    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    /**
     * 解析玩家限制服务。
     * Resolve the player-limit service.
     *
     * @return 玩家限制服务 / Player-limit service
     */
    public PlayerLimitService playerLimitService() {
        return getIfAvailable(playerLimitServiceProvider, PlayerLimitService::getInstance);
    }

    /**
     * 解析 NPC 喊话服务。
     * Resolve the NPC-shouts service.
     *
     * @return NPC 喊话服务 / NPC-shouts service
     */
    public NpcShoutsService npcShoutsService() {
        return getIfAvailable(npcShoutsServiceProvider, NpcShoutsService::getInstance);
    }

    /**
     * 解析护盾服务。
     * Resolve the shield service.
     *
     * @return 护盾服务 / Shield service
     */
    public ShieldService shieldService() {
        return getIfAvailable(shieldServiceProvider, ShieldService::getInstance);
    }

    /**
     * 解析奖励服务。
     * Resolve the reward service.
     *
     * @return 奖励服务 / Reward service
     */
    public RewardService rewardService() {
        return getIfAvailable(rewardServiceProvider, RewardService::getInstance);
    }

    /**
     * 解析老兵奖励服务。
     * Resolve the veteran-rewards service.
     *
     * @return 老兵奖励服务 / Veteran-rewards service
     */
    public VeteranRewardsService veteranRewardsService() {
        return getIfAvailable(veteranRewardsServiceProvider, VeteranRewardsService::getInstance);
    }

    /**
     * 解析争议之地服务。
     * Resolve the dispute-land service.
     *
     * @return 争议之地服务 / Dispute-land service
     */
    public DisputeLandService disputeLandService() {
        return getIfAvailable(disputeLandServiceProvider, DisputeLandService::getInstance);
    }

    /**
     * 解析前哨服务。
     * Resolve the outpost service.
     *
     * @return 前哨服务 / Outpost service
     */
    public OutpostService outpostService() {
        return getIfAvailable(outpostServiceProvider, OutpostService::getInstance);
    }

    /**
     * 解析保护者征服者服务。
     * Resolve the protector-conqueror service.
     *
     * @return 保护者征服者服务 / Protector-conqueror service
     */
    public ProtectorConquerorService protectorConquerorService() {
        return getIfAvailable(protectorConquerorServiceProvider, ProtectorConquerorService::getInstance);
    }

    /**
     * 解析钢铁之战服务。
     * Resolve the Dredgion service.
     *
     * @return 钢铁之战服务 / Dredgion service
     */
    public DredgionService2 dredgionService() {
        return getIfAvailable(dredgionServiceProvider, DredgionService2::getInstance);
    }

    /**
     * 解析阿修那塔服务。
     * Resolve the Asyunatar service.
     *
     * @return 阿修那塔服务 / Asyunatar service
     */
    public AsyunatarService asyunatarService() {
        return getIfAvailable(asyunatarServiceProvider, AsyunatarService::getInstance);
    }

    /**
     * 解析 FFA 服务。
     * Resolve the FFA service.
     *
     * @return FFA 服务 / FFA service
     */
    public FFAService ffaService() {
        return getIfAvailable(ffaServiceProvider, FFAService::getInstance);
    }

    /**
     * 解析天梯服务。
     * Resolve the ladder service.
     *
     * @return 天梯服务 / Ladder service
     */
    public LadderService ladderService() {
        return getIfAvailable(ladderServiceProvider, LadderService::getInstance);
    }

    /**
     * 解析战场服务。
     * Resolve the BG service.
     *
     * @return 战场服务 / BG service
     */
    public BGService bgService() {
        return getIfAvailable(bgServiceProvider, BGService::getInstance);
    }

    /**
     * 解析土匪服务。
     * Resolve the bandit service.
     *
     * @return 土匪服务 / Bandit service
     */
    public BanditService banditService() {
        return getIfAvailable(banditServiceProvider, BanditService::getInstance);
    }

    /**
     * 解析攻城服务。
     * Resolve the siege service.
     *
     * @return 攻城服务 / Siege service
     */
    public SiegeService siegeService() {
        return getIfAvailable(siegeServiceProvider, SiegeService::getInstance);
    }

    /**
     * 解析基地服务。
     * Resolve the base service.
     *
     * @return 基地服务 / Base service
     */
    public BaseService baseService() {
        return getIfAvailable(baseServiceProvider, BaseService::getInstance);
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
}
