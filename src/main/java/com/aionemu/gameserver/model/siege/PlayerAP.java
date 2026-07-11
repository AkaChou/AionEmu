package com.aionemu.gameserver.model.siege;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家欧比斯点数，用于要塞相关逻辑。
 * Player AP for siege logic.
 *
 * @author antness
 */
public class PlayerAP implements Comparable<PlayerAP> {

	private Player player;
	private Race race;
	private int ap;

	public PlayerAP(Player player) {
		this.player = player;
		this.race = player.getRace();
		this.ap = 0;
	}

	/** 获取玩家。 / Returns the player. */
	public Player getPlayer() {
		return this.player;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return this.race;
	}

	/** 获取欧比斯点数。 / Returns the ap. */
	public int getAP() {
		return this.ap;
	}

	/** 增加欧比斯点数。 / Increase ap. */
	public void increaseAP(int ap) {
		this.ap += ap;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(PlayerAP pl) {
		return this.ap - pl.ap;
	}
}
