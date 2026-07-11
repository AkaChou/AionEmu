package com.aionemu.gameserver.services.protectors;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 守护者（Serial Guard）运行时状态，绑定玩家及其守护等级与类型。
 * Protector (serial guard) runtime state bound to a player with rank and type.
 */
public class Protector {
	public int victims;
	private Player owner;
	private int guardType;
	private int guardRank;

	/**
	 * 创建指定所有者的守护者状态。
	 * Creates protector state for the given owner.
	 *
	 * @param owner 所属玩家 / owning player
	 */
	public Protector(Player owner) {
		this.owner = owner;
	}

	/**
	 * 刷新所有者引用（例如重登后）。
	 * Refreshes the owner reference (e.g. after re-login).
	 *
	 * @param player 新的玩家实例 / new player instance
	 */
	public void refreshOwner(Player player) {
		owner = player;
	}

	/**
	 * 获取所属玩家。
	 * Returns the owning player.
	 *
	 * owner
	 */
	public Player getOwner() {
		return owner;
	}

	/**
	 * 设置守护等级。
	 * Sets the protector rank.
	 *
	 * @param rank 守护等级 / guard rank
	 */
	public void setRank(int rank) {
		guardRank = rank;
	}

	/**
	 * 获取守护等级。
	 * Returns the protector rank.
	 *
	 * guard rank
	 */
	public int getRank() {
		return guardRank;
	}

	/**
	 * 设置守护类型。
	 * Sets the protector type.
	 *
	 * @param type 守护类型 / guard type
	 */
	public void setType(int type) {
		guardType = type;
	}

	/**
	 * 获取守护类型。
	 * Returns the protector type.
	 *
	 * guard type
	 */
	public int getType() {
		return guardType;
	}
}
