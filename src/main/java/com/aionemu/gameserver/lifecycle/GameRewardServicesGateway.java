package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WeddingsConfig;
import com.aionemu.gameserver.services.WeddingService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import org.springframework.stereotype.Component;

@Component
public class GameRewardServicesGateway {

    public void start() {
        if (CustomConfig.ENABLE_REWARD_SERVICE) {
            RewardService.getInstance();
        }
        if (WeddingsConfig.WEDDINGS_ENABLE) {
            WeddingService.getInstance();
        }
        if (VeteranRewardConfig.VETERANREWARDS_ENABLED) {
            VeteranRewardsService.getInstance();
        }
    }
}
