package com.aionemu.gameserver.model.instance.playerreward;

/**
 * Crucible 玩家奖励，用于副本相关逻辑。
 * Crucible Player Reward for instance logic.
 */

public class CruciblePlayerReward extends InstancePlayerReward {
	public static final int PARTICIPATION_NONE = 0;
	public static final int PARTICIPATION_PLAYING = 1;
	public static final int PARTICIPATION_WAITING = 2;
	public static final int PARTICIPATION_FINISHED = 3;

	private int insignia;
	private int spawnPosition;
	private boolean isRewarded = false;
	private boolean isPlayerLeave = false;
	private int participationState;

	public CruciblePlayerReward(Integer object) {
		super(object);
	}

	/**
	 * @return Whether rewarded
	 */
	public boolean isRewarded() {
		return isRewarded;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 设置 insignia / Sets the insignia */
	public void setInsignia(int insignia) {
		this.insignia = insignia;
	}

	/** 返回 insignia / Returns the insignia */
	public int getInsignia() {
		return insignia;
	}

	/** 设置刷新点坐标。 / Sets the spawn position. */
	public void setSpawnPosition(int spawnPosition) {
		this.spawnPosition = spawnPosition;
	}

	/** 获取刷新点坐标。 / Returns the spawn position. */
	public int getSpawnPosition() {
		return spawnPosition;
	}

	/** 是否玩家离开 / Whether player leave*/
	public boolean isPlayerLeave() {
		return isPlayerLeave;
	}

	/** 设置玩家离开 / Sets the player leave*/
	public void setPlayerLeave() {
		isPlayerLeave = true;
	}

	public synchronized int getParticipationState() {
		return participationState;
	}

	public synchronized void setParticipationState(int participationState) {
		if (participationState < PARTICIPATION_NONE || participationState > PARTICIPATION_FINISHED) {
			throw new IllegalArgumentException("Invalid Crucible participation state " + participationState);
		}
		if (this.participationState != PARTICIPATION_FINISHED) {
			this.participationState = participationState;
		}
	}
}
