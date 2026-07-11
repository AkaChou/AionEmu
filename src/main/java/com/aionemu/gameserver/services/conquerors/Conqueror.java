package com.aionemu.gameserver.services.conquerors;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 征服者（Serial Killer）运行时状态，绑定玩家及其击杀等级。
 * Conqueror (serial killer) runtime state bound to a player with killer rank.
 */
public class Conqueror {
	private Player owner;
	private int killerRank;
	public int victims;

	/**
	 * 创建指定所有者的征服者状态。
	 * Creates conqueror state for the given owner.
	 *
	 * @param owner 所属玩家 / owning player
	 */
	public Conqueror(Player owner) {
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
	 * 设置击杀等级。
	 * Sets the killer rank.
	 *
	 * @param rank 击杀等级 / killer rank
	 */
	public void setRank(int rank) {
		killerRank = rank;
	}

	/**
	 * 获取击杀等级。
	 * Returns the killer rank.
	 *
	 * killer rank
	 */
	public int getRank() {
		return killerRank;
	}
}
