package com.aionemu.gameserver.model.gameobjects.player;

/**
 * InRoll 游戏对象。
 * In Roll game object.
 *
 * @author xTz
 */
public class InRoll {

	private int npcId;
	private int itemId;
	private int rollType;
	private int index;

	public InRoll(int npcId, int itemId, int index, int rollType) {
		this.npcId = npcId;
		this.itemId = itemId;
		this.index = index;
		this.rollType = rollType;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 返回索引 / Returns the index*/
	public int getIndex() {
		return index;
	}

	/** 返回 roll type / Returns the roll type */
	public int getRollType() {
		return rollType;
	}

	/** 设置 npc id / Sets the npc id */
	public void setNpcId(int npcId) {
		this.npcId = npcId;
	}

	/** 设置物品 ID / Sets the item id */
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	/** 设置 indexd / Sets the indexd */
	public void setIndexd(int index) {
		this.index = itemId;
	}

	/** 设置 roll type / Sets the roll type */
	public void setRollType(int rollType) {
		this.rollType = rollType;
	}
}
