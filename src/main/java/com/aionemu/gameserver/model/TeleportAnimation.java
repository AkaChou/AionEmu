package com.aionemu.gameserver.model;

public enum TeleportAnimation {
	/** No Animation / No Animation */
	NO_ANIMATION(0, 0), BEAM_ANIMATION(1, 3), JUMP_ANIMATION(3, 10), JUMP_ANIMATION_2(4, 10), FIRE_ANIMATION(4, 0x0B), // 5.0
	/** Jump Animation 3 / Jump Animation 3 */
	JUMP_ANIMATION_3(8, 3), MAGE_ANIMATION(8, 10), DIRECT_PORTAL(0x0E, 0x0D), INVASION_PORTAL(0x0E, 0x12);

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
