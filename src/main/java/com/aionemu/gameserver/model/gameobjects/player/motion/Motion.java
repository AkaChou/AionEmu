package com.aionemu.gameserver.model.gameobjects.player.motion;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Motion 游戏对象。
 * Motion game object.
 */

public class Motion implements IExpirable {
	/** 动作 ID 到槽位类型的映射表。 / Motion ID to slot type mapping. */
	static final Map<Integer, Integer> motionType = new HashMap<Integer, Integer>();
	static {
		motionType.put(1, 1);
		motionType.put(2, 2);
		motionType.put(3, 3);
		motionType.put(4, 4);
		////////////////////
		motionType.put(5, 1);
		motionType.put(6, 2);
		motionType.put(7, 3);
		motionType.put(8, 4);
		////////////////////
		// 动作 3.9 / Motion 3.9
		motionType.put(10, 1);
		motionType.put(19, 2);
		////////////////////
		// 动作 4.5 / Motion 4.5
		motionType.put(11, 1);
		motionType.put(12, 2);
		motionType.put(13, 3);
		motionType.put(14, 4);
		////////////////////
		// 动作 4.7 / Motion 4.7
		motionType.put(15, 1);
		motionType.put(16, 2);
		motionType.put(17, 3);
		motionType.put(18, 4);
		motionType.put(20, 1);
		////////////////////
		motionType.put(21, 1);
		motionType.put(22, 2);
		////////////////////
		// 动作 4.8 / Motion 4.8
		motionType.put(23, 1);
		motionType.put(24, 2);
		motionType.put(25, 3);
		motionType.put(26, 4);
		////////////////////
		// 动作 5.0 / Motion 5.0
		motionType.put(27, 1);
		motionType.put(28, 2);
		motionType.put(29, 3);
		motionType.put(30, 4);
		// 商店 2 / Shop 2
		motionType.put(31, 1);
		// CHN Vip Shop 5.1
		motionType.put(32, 1);
		// KR 万圣节 5.3 / KR Halloween 5.3
		motionType.put(33, 1);
		// 滑冰 5.3 / Skating 5.3
		motionType.put(34, 1);
		motionType.put(35, 2);
		motionType.put(36, 3);
		motionType.put(37, 4);
		// 睡衣 5.6 / Pajamas 5.6
		motionType.put(38, 1);
	}

	private int id;
	private int deletionTime = 0;
	private boolean active = false;

	public Motion(int id, int deletionTime, boolean isActive) {
		this.id = id;
		this.deletionTime = deletionTime;
		this.active = isActive;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		if (deletionTime == 0) {
			return 0;
		}
		return deletionTime - (int) (System.currentTimeMillis() / 1000);
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return active;
	}

	/** 设置 active / Sets the active */
	public void setActive(boolean active) {
		this.active = active;
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return deletionTime;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		player.getMotions().remove(id);
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
