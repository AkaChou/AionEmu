package com.aionemu.gameserver.model.gameobjects.player.emotion;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 表情游戏对象。
 * Emotion game object.
 *
 * @author MrPoke
 */
public class Emotion implements IExpirable {
	private int id;
	private int dispearTime;

	/**
	 * @param id
	 * @param dispearTime
	 */
	public Emotion(int id, int dispearTime) {
		this.id = id;
		this.dispearTime = dispearTime;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

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
