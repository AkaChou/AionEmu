package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 奖励服务生命周期：按配置解析并启动常规奖励与老兵奖励服务，加载状态由基类承载。
 * Reward-services lifecycle: resolves and starts regular and veteran reward services by config;
 * load state is hosted by the base class.
 */
@Component
public class GameRewardServicesLifecycle extends GameStartupStage {

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
     * 启动本阶段：按配置解析常规奖励与老兵奖励。
     * Start this stage: resolve regular and veteran rewards by config.
     */
    @Override
    protected void runStage() {
        if (isRewardServiceEnabled()) {
            rewardServiceProvider.getObject();
        }
        if (isVeteranRewardsEnabled()) {
            veteranRewardsServiceProvider.getObject();
        }
    }

    /**
     * 常规奖励服务是否已启用。
     * Whether the regular reward service is enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isRewardServiceEnabled() {
        return CustomConfig.ENABLE_REWARD_SERVICE;
    }

    /**
     * 老兵奖励是否已启用。
     * Whether veteran rewards are enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isVeteranRewardsEnabled() {
        return VeteranRewardConfig.VETERANREWARDS_ENABLED;
    }
}
