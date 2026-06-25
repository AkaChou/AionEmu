package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WeddingsConfig;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameRewardServicesGateway {

    private ObjectProvider<RewardService> rewardServiceProvider;
    private ObjectProvider<WeddingService> weddingServiceProvider;
    private ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider;

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

    public void start() {
        if (CustomConfig.ENABLE_REWARD_SERVICE) {
            rewardService();
        }
        if (WeddingsConfig.WEDDINGS_ENABLE) {
            weddingService();
        }
        if (VeteranRewardConfig.VETERANREWARDS_ENABLED) {
            veteranRewardsService();
        }
    }

    private RewardService rewardService() {
        if (rewardServiceProvider == null) {
            return RewardService.getInstance();
        }
        return rewardServiceProvider.getIfAvailable(RewardService::getInstance);
    }

    private WeddingService weddingService() {
        if (weddingServiceProvider == null) {
            return WeddingService.getInstance();
        }
        return weddingServiceProvider.getIfAvailable(WeddingService::getInstance);
    }

    private VeteranRewardsService veteranRewardsService() {
        if (veteranRewardsServiceProvider == null) {
            return VeteranRewardsService.getInstance();
        }
        return veteranRewardsServiceProvider.getIfAvailable(VeteranRewardsService::getInstance);
    }
}
