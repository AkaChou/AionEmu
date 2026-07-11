package com.aionemu.gameserver.model.gameobjects.player;

/**
 * Blocked 玩家游戏对象。
 * Blocked Player game object.
 *
 * @author Ben
 */
public class BlockedPlayer {

	PlayerCommonData pcd;
	String reason;

	public BlockedPlayer(PlayerCommonData pcd) {
		this(pcd, "");
	}

	public BlockedPlayer(PlayerCommonData pcd, String reason) {
		this.pcd = pcd;
		this.reason = reason;
	}

	/** 返回对象 ID / Returns the obj id */
	public int getObjId() {
		return pcd.getPlayerObjId();
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return pcd.getName();
	}

	/** 返回 reason / Returns the reason */
	public String getReason() {
		return reason;
	}

	/** 设置 reason / Sets the reason */
	public synchronized void setReason(String reason) {
		this.reason = reason;
	}
}
