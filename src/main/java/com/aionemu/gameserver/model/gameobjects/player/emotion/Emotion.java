package com.aionemu.gameserver.model.gameobjects.player.emotion;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 表情游戏对象。
 * Emotion game object.
 * @author MrPoke
 */
@Getter
@AllArgsConstructor
public class Emotion implements IExpirable {
	/**
	 * 获取表情 ID。
	 * Returns the emotion id.
	 */
	private final int id;
	private final int dispearTime;

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		if (dispearTime == 0) {
			return 0;
		}
		return dispearTime - (int) (System.currentTimeMillis() / 1000);
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return dispearTime;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		player.getEmotions().remove(id);
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
