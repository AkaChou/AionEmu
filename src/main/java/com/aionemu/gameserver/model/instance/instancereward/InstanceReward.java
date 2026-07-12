package com.aionemu.gameserver.model.instance.instancereward;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;

import java.util.ArrayList;
import java.util.List;

/**
 * 副本奖励模型。
 * Instance Reward model.
 */
@Slf4j

public class InstanceReward<T extends InstancePlayerReward> {
	protected List<T> instanceRewards = new ArrayList<T>();
	private InstanceScoreType instanceScoreType = InstanceScoreType.START_PROGRESS;
	protected Integer mapId;
	protected int instanceId;
	public InstanceReward(Integer mapId, int instanceId) {
		this.mapId = mapId;
		this.instanceId = instanceId;
	}

	/** 获取副本奖励。 / Returns the instance rewards. */
	public List<T> getInstanceRewards() {
		return instanceRewards;
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
		if (instanceRewards.contains(reward)) {
			instanceRewards.remove(reward);
		}
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

	/** 设置 instance score type / Sets the instance score type */
	public void setInstanceScoreType(InstanceScoreType instanceScoreType) {
		this.instanceScoreType = instanceScoreType;
	}

	/** 返回 instance score type / Returns the instance score type */
	public InstanceScoreType getInstanceScoreType() {
		return instanceScoreType;
	}

	/** 返回映射 ID / Returns the map id */
	public Integer getMapId() {
		return mapId;
	}

	/** 返回副本 ID / Returns the instance id */
	public int getInstanceId() {
		return instanceId;
	}

	/**
	 * @return Whether rewarded
	 */
	public boolean isRewarded() {
		return instanceScoreType.isEndProgress();
	}

	/**
	 * @return Whether preparing
	 */
	public boolean isPreparing() {
		return instanceScoreType.isPreparing();
	}

	/**
	 * @return Whether start progress
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
	 * @param log 是否发送日志。 / Send log
	  */
	public void sendLog(String log) {
		this.log.info(log);
	}
}
