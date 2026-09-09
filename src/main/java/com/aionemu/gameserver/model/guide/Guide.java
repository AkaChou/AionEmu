package com.aionemu.gameserver.model.guide;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 指南模型。
 * Guide model.
 *
 * @author xTz
 */
@AllArgsConstructor
public class Guide {

	private final int guide_id;
	private final int player_id;
	/** 获取称号。 / Returns the title. */
	@Getter
	private final String title;

	/** 返回引导 ID / Returns the guide id */
	public int getGuideId() {
		return guide_id;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return player_id;
	}
}
