package com.aionemu.gameserver.model.guide;


/**
 * 指南模型。
 * Guide model.
 *
 * @param title 获取称号。 / Returns the title.
 * @author xTz
 */
public record Guide(int guide_id, int player_id, String title) {

	/**
	 * 返回引导 ID / Returns the guide id
	 */
	public int getGuideId() {
		return guide_id;
	}

	/**
	 * 返回玩家 ID / Returns the player id
	 */
	public int getPlayerId() {
		return player_id;
	}
}
