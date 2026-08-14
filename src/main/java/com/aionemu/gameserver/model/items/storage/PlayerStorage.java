package com.aionemu.gameserver.model.items.storage;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

/**
 * 玩家仓库，用于物品相关逻辑。
 * Player Storage for items logic.
 *
 * @author ATracer
 */
public class PlayerStorage extends Storage {

	private Player actor;

	/**
	 * @param storageType
	 */
	public PlayerStorage(StorageType storageType) {
		super(storageType);
	}

	/** 设置所有者。 / Sets the owner. */
	@Override
	public final void setOwner(Player actor) {
		this.actor = actor;
	}

	/** 加载回调。 / On load handler. */
	public void onLoadHandler(Item item) {
		if (item.isEquipped()) {
			actor.getEquipment().onLoadHandler(item);
		} else {
			super.onLoadHandler(item);
		}
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount) {
		increaseKinah(amount, actor);
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount, ItemUpdateType updateType) {
		increaseKinah(amount, updateType, actor);
	}

	/** 尝试减少基纳。 / Try to decrease kinah. */
	@Override
	public boolean tryDecreaseKinah(long amount) {
		return tryDecreaseKinah(amount, actor);
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount) {
		decreaseKinah(amount, actor);
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount, ItemUpdateType updateType) {
		decreaseKinah(amount, updateType, actor);
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count) {
		return increaseItemCount(item, count, actor);
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count, ItemUpdateType updateType) {
		return increaseItemCount(item, count, updateType, actor);
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count) {
		return decreaseItemCount(item, count, actor);
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count, ItemUpdateType updateType) {
		return decreaseItemCount(item, count, updateType, actor);
	}

	/** 添加。 / Add. */
	@Override
	public Item add(Item item) {
		return add(item, actor);
	}

	/** 放入。 / Put. */
	@Override
	public Item put(Item item) {
		return put(item, actor);
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item) {
		return delete(item, actor);
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item, ItemDeleteType deleteType) {
		return delete(item, deleteType, actor);
	}

	/** 按物品 ID 减少 / Decrease by item id */
	@Override
	public boolean decreaseByItemId(int itemId, long count) {
		return decreaseByItemId(itemId, count, actor);
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count) {
		return decreaseByObjectId(itemObjId, count, actor);
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count, ItemUpdateType updateType) {
		return decreaseByObjectId(itemObjId, count, updateType, actor);
	}
}
