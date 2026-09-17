package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Blocked 玩家游戏对象。
 * Blocked Player game object.
 *
 * @author Ben
 */
@Getter
@AllArgsConstructor
public class BlockedPlayer {

	PlayerCommonData pcd;
	/** 返回 reason / Returns the reason */
	String reason;

	public BlockedPlayer(PlayerCommonData pcd) {
		this(pcd, "");
	}

	/** 返回对象 ID / Returns the obj id */
	public int getObjId() {
		return pcd.getPlayerObjId();
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return pcd.getName();
	}

	/** 设置 reason / Sets the reason */
	public synchronized void setReason(String reason) {
		this.reason = reason;
	}
}
