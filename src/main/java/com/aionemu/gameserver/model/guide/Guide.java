package com.aionemu.gameserver.model.guide;

/**
 * 指南模型。
 * Guide model.
 *
 * @author xTz
 */
public class Guide {

	private int guide_id;
	private int player_id;
	private String title;

	public Guide(int guide_id, int player_id, String title) {
		this.guide_id = guide_id;
		this.player_id = player_id;
		this.title = title;
	}

	/** 返回引导 ID / Returns the guide id */
	public int getGuideId() {
		return guide_id;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return player_id;
	}

	/** 获取称号。 / Returns the title. */
	public String getTitle() {
		return title;
	}
}
