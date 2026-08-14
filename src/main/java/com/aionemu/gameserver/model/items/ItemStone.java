package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import lombok.Getter;

/**
 * 物品 Stone 模型。
 * Item Stone model.
 *
 * @author ATracer modified by Wakizashi
 */
@Getter
public class ItemStone implements StatOwner {

	private int itemObjId;

	private int itemId;

	private int slot;

	private PersistentState persistentState;

	public static enum ItemStoneType {
		/** 魔石 / Manastone */
		MANASTONE,
		/** 神石 / Godstone */
		GODSTONE,
		/** 融合石 / Fusionstone */
		FUSIONSTONE,
		/** 伊迪安石 / Idian stone */
		IDIANSTONE;
	}

	/**
	 * 构造物品镶嵌石。
	 * Constructs an item stone.
	 *
	 * @param itemObjId 物品对象 ID / item object id
	 * @param itemId 镶嵌石模板 ID / stone template id
	 * @param slot 槽位 / slot
	 * @param persistentState 持久化状态 / persistent state
	 */
	public ItemStone(int itemObjId, int itemId, int slot, PersistentState persistentState) {
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.slot = slot;
		this.persistentState = persistentState;
	}

	/**
	 * 设置槽位。
	 * Sets the slot.
	 *
	 * @param slot 槽位 / slot to set
	 */
	public void setSlot(int slot) {
		this.slot = slot;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 设置持久化状态（NEW 状态下不可降级为 DELETED）。
	 * Sets the persistent state (NEW cannot be downgraded to DELETED).
	 *
	 * @param persistentState 持久化状态 / persistent state
	 */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				this.persistentState = PersistentState.NOACTION;
			} else {
				this.persistentState = PersistentState.DELETED;
			}
			break;
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}
}
