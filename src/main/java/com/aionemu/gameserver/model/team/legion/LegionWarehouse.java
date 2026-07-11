package com.aionemu.gameserver.model.team.legion;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

/**
 * 军团仓库，用于团队相关逻辑。
 * Legion Warehouse for team logic.
 *
 * @author Simple
 */
public class LegionWarehouse extends Storage {

	private Legion legion;
	private int curentWhUser;

	public LegionWarehouse(Legion legion) {
		super(StorageType.LEGION_WAREHOUSE);
		this.legion = legion;
		this.setLimit(legion.getWarehouseSlots());
	}

	/** 获取军团。 / Returns the legion. */
	public Legion getLegion() {
		return this.legion;
	}

	/** 设置所有者军团 / Sets the owner legion*/
	public void setOwnerLegion(Legion legion) {
		this.legion = legion;
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 增加基纳。 / Increase kinah. */
	@Override
	public void increaseKinah(long amount, ItemUpdateType updateType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** Try 减少 Kinah / Try Decrease Kinah */
	@Override
	public boolean tryDecreaseKinah(long amount) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 减少基纳。 / Decrease kinah. */
	@Override
	public void decreaseKinah(long amount, ItemUpdateType updateType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 增加物品计数。 / Increase item count. */
	@Override
	public long increaseItemCount(Item item, long count, ItemUpdateType updateType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 减少物品计数。 / Decrease item count. */
	@Override
	public long decreaseItemCount(Item item, long count, ItemUpdateType updateType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 添加。 / Add. */
	@Override
	public Item add(Item item) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 放入。 / Put. */
	@Override
	public Item put(Item item) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 删除。 / Delete. */
	@Override
	public Item delete(Item item, ItemDeleteType deleteType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 按物品 ID 减少 / Decrease by item id */
	@Override
	public boolean decreaseByItemId(int itemId, long count) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 按对象 ID 减少 / Decrease by object id */
	@Override
	public boolean decreaseByObjectId(int itemObjId, long count, ItemUpdateType updateType) {
		throw new UnsupportedOperationException("LWH should be used behind proxy");
	}

	/** 设置所有者 / Sets the owner*/
	@Override
	public void setOwner(Player player) {
		throw new UnsupportedOperationException("LWH doesnt have owner");
	}

	/** 设置 wh user / Sets the wh user */
	public void setWhUser(int curentWhUser) {
		this.curentWhUser = curentWhUser;
	}

	/** 返回 wh user / Returns the wh user */
	public int getWhUser() {
		return curentWhUser;
	}
}
