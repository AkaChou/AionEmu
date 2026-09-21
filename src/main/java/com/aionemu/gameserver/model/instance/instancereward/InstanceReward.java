package com.aionemu.gameserver.model.instance.instancereward;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 副本奖励模型。
 * Instance Reward model.
 */
@Slf4j

@Getter
@Setter
public class InstanceReward<T extends InstancePlayerReward> {
	/** 获取副本奖励。 / Returns the instance rewards. */
	protected List<T> instanceRewards = new ArrayList<>();
	/** 设置 instance score type / Sets the instance score type */
	private InstanceScoreType instanceScoreType = InstanceScoreType.START_PROGRESS;
	/** 返回映射 ID / Returns the map id */
	protected Integer mapId;
	/** 返回副本 ID / Returns the instance id */
	protected int instanceId;
	public InstanceReward(Integer mapId, int instanceId) {
		this.mapId = mapId;
		this.instanceId = instanceId;
	}

	/** 包含玩家 / Contain Player */
	public boolean containPlayer(Integer object) {
		for (InstancePlayerReward instanceReward : instanceRewards) {
			if (instanceReward.getOwner().equals(object)) {
				return true;
			}
		}
		return false;
	}

	/** 移除玩家奖励。 / Removes player reward. */
	public void removePlayerReward(T reward) {
		instanceRewards.remove(reward);
	}

	/** 获取玩家奖励。 / Returns the player reward. */
	public InstancePlayerReward getPlayerReward(Integer object) {
		for (InstancePlayerReward instanceReward : instanceRewards) {
			if (instanceReward.getOwner().equals(object)) {
				return instanceReward;
			}
		}
		return null;
	}

	/** 添加玩家奖励。 / Adds player reward. */
	public void addPlayerReward(T reward) {
		instanceRewards.add(reward);
	}

	/**
	 * @return 是否已结算 / Whether rewarded
	 */
	public boolean isRewarded() {
		return instanceScoreType.isEndProgress();
	}

	/**
	 * @return 是否准备中 / Whether preparing
	 */
	public boolean isPreparing() {
		return instanceScoreType.isPreparing();
	}

	/**
	 * @return 是否开始进度 / Whether start progress
	 */
	public boolean isStartProgress() {
		return instanceScoreType.isStartProgress();
	}

	/** 设置 instance start time / Sets the instance start time */
	public void setInstanceStartTime() {
		System.currentTimeMillis();
	}

	/** 清空。 / Clear. */
	public void clear() {
		instanceRewards.clear();
	}

	protected InstanceReward<?> getInstanceReward() {
		return this;
	}

	/**
	 * @param log 日志消息 / log message
	 */
	public void sendLog(String log) {
		InstanceReward.log.info(log);
	}
}
