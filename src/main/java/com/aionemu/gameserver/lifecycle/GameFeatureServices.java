package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.ShieldService;
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
            ObjectProvider<BanditService> banditServiceProvider) {
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
    }
}
