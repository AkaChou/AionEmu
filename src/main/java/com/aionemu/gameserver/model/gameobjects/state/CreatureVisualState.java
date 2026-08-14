package com.aionemu.gameserver.model.gameobjects.state;

/**
 * 生物 Visual 状态枚举。
 * Creature Visual State enumeration.
 *
 * @author Sweetkr
 */
public enum CreatureVisualState {
	/** 可见。 / Visible. */
	VISIBLE(0), // 正常 / Normal
	/** 隐身 I / Hide I */
	HIDE1(1), // 隐身 I / Hide I
	/** 隐身 II / Hide II */
	HIDE2(2), // 隐身 II / Hide II
	/** 隐身 III / Hide III */
	HIDE3(3), // 由神器隐藏？ / Hide by Artifact?
	/** 隐身 5 / Hide 5 */
	HIDE5(5), // 未知效果 / No idea :D
	/** 隐身 10 / Hide 10 */
	HIDE10(10), // 对 NPC 隐身？ / Hide from Npc?
	/** 隐身 13 / Hide 13 */
	HIDE13(13), // 对 NPC 隐身？ / Hide from Npc?
	/** 隐身 20 / Hide 20 */
	HIDE20(20), // 对 NPC 隐身？ / Hide from Npc?
	/** 闪烁 / Blinking. */
	BLINKING(64); // 进入区域时闪烁 / Blinking when entering to zone

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
