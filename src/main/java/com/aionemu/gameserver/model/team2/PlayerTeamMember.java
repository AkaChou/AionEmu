package com.aionemu.gameserver.model.team2;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家团队 Member，用于团队2相关逻辑。
 * Player Team Member for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerTeamMember implements TeamMember<Player> {

	final Player player;
	private long lastOnlineTime;

	public PlayerTeamMember(Player player) {
		this.player = player;
	}

	/** 返回对象 ID / Returns the object id */
	@Override
	public Integer getObjectId() {
		return player.getObjectId();
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return player.getName();
	}

	/** 获取对象。 / Returns the object. */
	@Override
	public Player getObject() {
		return player;
	}

	/** 返回最后在线时间 / Returns the last online time */
	public long getLastOnlineTime() {
		return lastOnlineTime;
	}

	/** 更新最后在线时间 / Update last online time */
	public void updateLastOnlineTime() {
		lastOnlineTime = System.currentTimeMillis();
	}

	/** 是否在线。 / Whether Online. */
	public boolean isOnline() {
		return player.isOnline();
	}

	/** 返回 X 坐标 / Returns the x */
	public float getX() {
		return player.getX();
	}

	/** 返回 Y 坐标 / Returns the y */
	public float getY() {
		return player.getY();
	}

	/** 返回 Z 坐标 / Returns the z */
	public float getZ() {
		return player.getZ();
	}

	/** 返回朝向 / Returns the heading */
	public byte getHeading() {
		return player.getHeading();
	}

	/** 获取等级。 / Returns the level. */
	public byte getLevel() {
		return player.getLevel();
	}
}
