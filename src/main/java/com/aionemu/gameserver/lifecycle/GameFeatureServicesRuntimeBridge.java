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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameFeatureServicesRuntimeBridge {

    public PlayerLimitService playerLimitService() {
        return PlayerLimitService.getInstance();
    }

    public NpcShoutsService npcShoutsService() {
        return NpcShoutsService.getInstance();
    }

    public ShieldService shieldService() {
        return ShieldService.getInstance();
    }

    public RewardService rewardService() {
        return RewardService.getInstance();
    }

    public WeddingService weddingService() {
        return WeddingService.getInstance();
    }

    public VeteranRewardsService veteranRewardsService() {
        return VeteranRewardsService.getInstance();
    }

    public DisputeLandService disputeLandService() {
        return DisputeLandService.getInstance();
    }

    public OutpostService outpostService() {
        return OutpostService.getInstance();
    }

    public ProtectorConquerorService protectorConquerorService() {
        return ProtectorConquerorService.getInstance();
    }

    public DredgionService2 dredgionService() {
        return DredgionService2.getInstance();
    }

    public AsyunatarService asyunatarService() {
        return AsyunatarService.getInstance();
    }

    public FFAService ffaService() {
        return FFAService.getInstance();
    }

    public LadderService ladderService() {
        return LadderService.getInstance();
    }

    public BGService bgService() {
        return BGService.getInstance();
    }

    public BanditService banditService() {
        return BanditService.getInstance();
    }

    public SiegeService siegeService() {
        return SiegeService.getInstance();
    }

    public BaseService baseService() {
        return BaseService.getInstance();
    }
}
