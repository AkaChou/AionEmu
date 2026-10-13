package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 奖励服务网关：按配置解析并启动常规奖励与老兵奖励服务。
 * Reward-services gateway: resolves and starts regular and veteran reward services by config.
 */
@Component
public class GameRewardServicesGateway {

    /**
     * 奖励服务提供者。
     * Reward service provider.
     */
    private ObjectProvider<RewardService> rewardServiceProvider;

    /**
     * 老兵奖励服务提供者。
     * Veteran-rewards service provider.
     */
    private ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider;


    /**
     * 可选注入奖励服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of reward service.
     * @param rewardServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setRewardServiceProvider(ObjectProvider<RewardService> rewardServiceProvider) {
        this.rewardServiceProvider = rewardServiceProvider;
    }

    /**
     * 可选注入老兵奖励服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of veteran-rewards service.
     * @param veteranRewardsServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setVeteranRewardsServiceProvider(ObjectProvider<VeteranRewardsService> veteranRewardsServiceProvider) {
        this.veteranRewardsServiceProvider = veteranRewardsServiceProvider;
    }


    /**
     * 启动奖励服务：按配置解析常规奖励与老兵奖励。
     * Start reward services: resolve regular and veteran rewards by config.
     */
    public void start() {
        if (CustomConfig.ENABLE_REWARD_SERVICE) {
            rewardService();
        }
        if (VeteranRewardConfig.VETERANREWARDS_ENABLED) {
            veteranRewardsService();
        }
    }

    /**
     * 解析奖励服务：优先 Spring，否则经运行时桥回退。
     * Resolve reward service: prefer Spring, otherwise fall back via runtime bridge.
     */
    private void rewardService() {
        rewardServiceProvider.getObject();
    }

    /**
     * 解析老兵奖励服务：优先 Spring，否则经运行时桥回退。
     * Resolve veteran-rewards service: prefer Spring, otherwise fall back via runtime bridge.
     */
    private void veteranRewardsService() {
        veteranRewardsServiceProvider.getObject();
    }

}
