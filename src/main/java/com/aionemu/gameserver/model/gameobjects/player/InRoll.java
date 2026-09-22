package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Data;

/**
 * InRoll 游戏对象。
 * In Roll game object.
 * @author xTz
 */
@Data
public class InRoll {

	/** 返回 NPC ID / Returns the npc id */
	private int npcId;
	/** 返回物品 ID / Returns the item id */
	private int itemId;
	/** 返回 roll type / Returns the roll type */
	private int rollType;
	/** 返回索引 / Returns the index*/
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
