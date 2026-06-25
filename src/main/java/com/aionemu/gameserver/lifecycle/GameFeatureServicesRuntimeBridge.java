package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.WeddingService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameFeatureServicesRuntimeBridge {

    private ObjectProvider<PlayerLimitService> playerLimitServiceProvider;
    private ObjectProvider<NpcShoutsService> npcShoutsServiceProvider;
    private ObjectProvider<ShieldService> shieldServiceProvider;
    private ObjectProvider<RewardService> rewardServiceProvider;
    private ObjectProvider<WeddingService> weddingServiceProvider;
    private ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider;
    private ObjectProvider<DisputeLandService> disputeLandServiceProvider;
    private ObjectProvider<OutpostService> outpostServiceProvider;
    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;
    private ObjectProvider<DredgionService2> dredgionServiceProvider;
    private ObjectProvider<AsyunatarService> asyunatarServiceProvider;
    private ObjectProvider<FFAService> ffaServiceProvider;
    private ObjectProvider<LadderService> ladderServiceProvider;
    private ObjectProvider<BGService> bgServiceProvider;
    private ObjectProvider<BanditService> banditServiceProvider;
    private ObjectProvider<SiegeService> siegeServiceProvider;
    private ObjectProvider<BaseService> baseServiceProvider;

    @Autowired(required = false)
    void setPlayerLimitServiceProvider(ObjectProvider<PlayerLimitService> playerLimitServiceProvider) {
        this.playerLimitServiceProvider = playerLimitServiceProvider;
    }

    @Autowired(required = false)
    void setNpcShoutsServiceProvider(ObjectProvider<NpcShoutsService> npcShoutsServiceProvider) {
        this.npcShoutsServiceProvider = npcShoutsServiceProvider;
    }

    @Autowired(required = false)
    void setShieldServiceProvider(ObjectProvider<ShieldService> shieldServiceProvider) {
        this.shieldServiceProvider = shieldServiceProvider;
    }

    @Autowired(required = false)
    void setRewardServiceProvider(ObjectProvider<RewardService> rewardServiceProvider) {
        this.rewardServiceProvider = rewardServiceProvider;
    }

    @Autowired(required = false)
    void setWeddingServiceProvider(ObjectProvider<WeddingService> weddingServiceProvider) {
        this.weddingServiceProvider = weddingServiceProvider;
    }

    @Autowired(required = false)
    void setVeteranRewardsServiceProvider(ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider) {
        this.veteranRewardsServiceProvider = veteranRewardsServiceProvider;
    }

    @Autowired(required = false)
    void setDisputeLandServiceProvider(ObjectProvider<DisputeLandService> disputeLandServiceProvider) {
        this.disputeLandServiceProvider = disputeLandServiceProvider;
    }

    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
    }

    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
    }

    @Autowired(required = false)
    void setDredgionServiceProvider(ObjectProvider<DredgionService2> dredgionServiceProvider) {
        this.dredgionServiceProvider = dredgionServiceProvider;
    }

    @Autowired(required = false)
    void setAsyunatarServiceProvider(ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        this.asyunatarServiceProvider = asyunatarServiceProvider;
    }

    @Autowired(required = false)
    void setFfaServiceProvider(ObjectProvider<FFAService> ffaServiceProvider) {
        this.ffaServiceProvider = ffaServiceProvider;
    }

    @Autowired(required = false)
    void setLadderServiceProvider(ObjectProvider<LadderService> ladderServiceProvider) {
        this.ladderServiceProvider = ladderServiceProvider;
    }

    @Autowired(required = false)
    void setBgServiceProvider(ObjectProvider<BGService> bgServiceProvider) {
        this.bgServiceProvider = bgServiceProvider;
    }

    @Autowired(required = false)
    void setBanditServiceProvider(ObjectProvider<BanditService> banditServiceProvider) {
        this.banditServiceProvider = banditServiceProvider;
    }

    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    public PlayerLimitService playerLimitService() {
        return getIfAvailable(playerLimitServiceProvider, PlayerLimitService::getInstance);
    }

    public NpcShoutsService npcShoutsService() {
        return getIfAvailable(npcShoutsServiceProvider, NpcShoutsService::getInstance);
    }

    public ShieldService shieldService() {
        return getIfAvailable(shieldServiceProvider, ShieldService::getInstance);
    }

    public RewardService rewardService() {
        return getIfAvailable(rewardServiceProvider, RewardService::getInstance);
    }

    public WeddingService weddingService() {
        return getIfAvailable(weddingServiceProvider, WeddingService::getInstance);
    }

    public VeteranRewardsService veteranRewardsService() {
        return getIfAvailable(veteranRewardsServiceProvider, VeteranRewardsService::getInstance);
    }

    public DisputeLandService disputeLandService() {
        return getIfAvailable(disputeLandServiceProvider, DisputeLandService::getInstance);
    }

    public OutpostService outpostService() {
        return getIfAvailable(outpostServiceProvider, OutpostService::getInstance);
    }

    public ProtectorConquerorService protectorConquerorService() {
        return getIfAvailable(protectorConquerorServiceProvider, ProtectorConquerorService::getInstance);
    }

    public DredgionService2 dredgionService() {
        return getIfAvailable(dredgionServiceProvider, DredgionService2::getInstance);
    }

    public AsyunatarService asyunatarService() {
        return getIfAvailable(asyunatarServiceProvider, AsyunatarService::getInstance);
    }

    public FFAService ffaService() {
        return getIfAvailable(ffaServiceProvider, FFAService::getInstance);
    }

    public LadderService ladderService() {
        return getIfAvailable(ladderServiceProvider, LadderService::getInstance);
    }

    public BGService bgService() {
        return getIfAvailable(bgServiceProvider, BGService::getInstance);
    }

    public BanditService banditService() {
        return getIfAvailable(banditServiceProvider, BanditService::getInstance);
    }

    public SiegeService siegeService() {
        return getIfAvailable(siegeServiceProvider, SiegeService::getInstance);
    }

    public BaseService baseService() {
        return getIfAvailable(baseServiceProvider, BaseService::getInstance);
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }
}
