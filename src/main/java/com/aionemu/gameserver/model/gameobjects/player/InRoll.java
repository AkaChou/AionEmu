package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Getter;
import lombok.Setter;

/**
 * InRoll 游戏对象。
 * In Roll game object.
 *
 * @author xTz
 */
public class InRoll {

	/** 返回 NPC ID / Returns the npc id */
	@Getter
	@Setter
	private int npcId;
	/** 返回物品 ID / Returns the item id */
	@Getter
	@Setter
	private int itemId;
	/** 返回 roll type / Returns the roll type */
	@Getter
	@Setter
	private int rollType;
	/** 返回索引 / Returns the index*/
	@Getter
	private int index;

	public InRoll(int npcId, int itemId, int index, int rollType) {
		this.npcId = npcId;
		this.itemId = itemId;
		this.index = index;
		this.rollType = rollType;
	}

	/** 设置 indexd / Sets the indexd */
	public void setIndexd(int index) {
		this.index = itemId;
	}
}
