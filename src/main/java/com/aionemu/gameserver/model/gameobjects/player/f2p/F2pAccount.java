package com.aionemu.gameserver.model.gameobjects.player.f2p;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * F2p 账号游戏对象。
 * F2p Account game object.
 */

public class F2pAccount implements IExpirable {
	private int deleteTime = 0;
	private boolean active = false;

	public F2pAccount(int deletionTime) {
		deleteTime = deletionTime;
	}

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		if (deleteTime == 0) {
			return 0;
		}
		return deleteTime - (int) (System.currentTimeMillis() / 1000L);
	}

	/** 获取过期时间。 / Returns the expire time. */
	public int getExpireTime() {
		return deleteTime;
	}

	/** 返回当前 / Returns the active */
	public boolean getActive() {
		return active;
	}

	/** 设置 active / Sets the active */
	public void setActive(boolean active) {
		this.active = active;
	}

	/** 到期结束 / Expire End */
	public void expireEnd(Player player) {
		setActive(false);
		player.getF2p().remove();
		PacketSendUtility.sendBrightYellowMessageOnCenter(player, "<F2p Pack> is expired!!!");
	}

	/** 是否立即过期 / Whether expire now */
	public boolean canExpireNow() {
		return true;
	}

	/** 过期消息。 / Expire Message. */
	public void expireMessage(Player player, int time) {
		PacketSendUtility.sendBrightYellowMessageOnCenter(player, "<F2p Pack> end!!!");
	}
}
