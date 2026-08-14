package com.aionemu.gameserver.model;

/**
 * 传送动画枚举。
 * Teleport Animation enumeration.
 */

public enum TeleportAnimation {
	/** 无动画 / No Animation */
	NO_ANIMATION(0, 0),
	/** 光束动画 / Beam Animation */
	BEAM_ANIMATION(1, 3),
	/** 跳跃动画 / Jump Animation */
	JUMP_ANIMATION(3, 10),
	/** 跳跃动画 2 / Jump Animation 2 */
	JUMP_ANIMATION_2(4, 10),
	/** 火焰动画 / Fire Animation */
	FIRE_ANIMATION(4, 0x0B), // 5.0
	/** 跳跃动画 3 / Jump Animation 3 */
	JUMP_ANIMATION_3(8, 3),
	/** 法师动画 / Mage Animation */
	MAGE_ANIMATION(8, 10);

	private int startAnimation;
	private int endAnimation;

	private TeleportAnimation(int startAnimation, int endAnimation) {
		this.startAnimation = startAnimation;
		this.endAnimation = endAnimation;
	}

	/** 返回开始动画 ID / Returns the start animation id */
	public int getStartAnimationId() {
		return startAnimation;
	}

	/** 返回结束动画 ID / Returns the end animation id */
	public int getEndAnimationId() {
		return endAnimation;
	}

	/** 是否无动画 / Whether no animation */
	public boolean isNoAnimation() {
		return this.getStartAnimationId() == 0;
	}
}
