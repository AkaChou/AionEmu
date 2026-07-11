package com.aionemu.gameserver.model.gameobjects.state;

/**
 * 生物 Visual 状态枚举。
 * Creature Visual State enumeration.
 *
 * @author Sweetkr
 */
public enum CreatureVisualState {
	/** 可见。 / Visible. */
	VISIBLE(0), // Normal
	/** Hide1 / Hide1 */
	HIDE1(1), // Hide I
	/** Hide2 / Hide2 */
	HIDE2(2), // Hide II
	/** Hide3 / Hide3 */
	HIDE3(3), // Hide by Artifact?
	/** Hide5 / Hide5 */
	HIDE5(5), // No idea :D
	/** Hide10 / Hide10 */
	HIDE10(10), // Hide from Npc?
	/** Hide13 / Hide13 */
	HIDE13(13), // Hide from Npc?
	/** Hide20 / Hide20 */
	HIDE20(20), // Hide from Npc?
	/** 闪烁 / Blinking. */
	BLINKING(64); // Blinking when entering to zone

	private int id;

	private CreatureVisualState(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
