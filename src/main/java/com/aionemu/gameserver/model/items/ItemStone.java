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
		MANASTONE, GODSTONE, FUSIONSTONE, IDIANSTONE;
	}

	/**
	 * @param itemObjId
	 * @param itemId
	 * @param slot
	 * @param persistentState
	 */
	public ItemStone(int itemObjId, int itemId, int slot, PersistentState persistentState) {
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.slot = slot;
		this.persistentState = persistentState;
	}

	/**
	 * @param slot the slot to set
	 */
	public void setSlot(int slot) {
		this.slot = slot;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @param persistentState
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
