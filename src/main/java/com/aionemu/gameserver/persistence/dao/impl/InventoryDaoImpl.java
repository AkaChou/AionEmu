package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.aionemu.gameserver.persistence.mapper.InventoryMapper;
import com.aionemu.gameserver.services.item.ItemService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家背包与装备数据访问实现。
 * Data access implementation for player inventory and equipment.
 * @author ATracer
 */
@Slf4j
@Service
@Lazy
public class InventoryDaoImpl extends ServiceImpl<InventoryMapper, InventoryPo> implements InventoryDao {

	/**
	 * 加载指定存储类型的物品栏；账号仓库会先解析为账号 ID。
	 * Loads a storage of the given type for a player; account warehouse resolves to account id first.
	 *
	 * @param playerId    玩家 ID / player id
	 * @param storageType 存储类型 / storage type
	 * @return 存储实例 / storage instance
	 */
	@Override
	public Storage loadStorage(int playerId, StorageType storageType) {
		final Storage inventory = new PlayerStorage(storageType);
		final int storage = storageType.getId();
		final int equipped = 0;

		if (storageType == StorageType.ACCOUNT_WAREHOUSE) {
			playerId = loadPlayerAccountId(playerId);
		}

		final int owner = playerId;

		try {
			for (InventoryPo po : mapper.selectListByQuery(QueryWrapper.create()
					.where(InventoryPo::getItemOwner).eq(owner)
					.and(InventoryPo::getItemLocation).eq(storage)
					.and(InventoryPo::getEquiped).eq(equipped))) {
				Item item = constructItem(storage, po);
				if (item.getItemTemplate() == null) {
					log.error(I18n.get("log.9a2f5be30d7d", owner, item.getObjectId()));
				} else {
					item.setPersistentState(PersistentState.UPDATED);
					inventory.onLoadHandler(item);
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.52f3a07ead8f", owner), e);
		}

		return inventory;
	}

	/**
	 * 直接加载指定存储位置的物品列表（不封装为 Storage）。
	 * Loads items of a storage type as a plain list (no Storage wrapper).
	 *
	 * @param playerId    玩家 ID / player id
	 * @param storageType 存储类型 / storage type
	 * @return 物品列表 / item list
	 */
	@Override
	public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
		List<Item> list = new ArrayList<>();
		final int storage = storageType.getId();

		if (storageType == StorageType.ACCOUNT_WAREHOUSE) {
			playerId = loadPlayerAccountId(playerId);
		}

		final int owner = playerId;

		try {
			for (InventoryPo po : mapper.selectListByQuery(QueryWrapper.create()
					.where(InventoryPo::getItemOwner).eq(owner)
					.and(InventoryPo::getItemLocation).eq(storage))) {
				list.add(constructItem(storage, po));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.96d353298d09", owner), e);
		}

		return list;
	}

	/**
	 * 加载玩家已装备物品到装备栏对象。
	 * Loads equipped items into an Equipment instance for the player.
	 *
	 * @param player 玩家 / player
	 * @return 装备 / equipment
	 */
	@Override
	public Equipment loadEquipment(Player player) {
		final Equipment equipment = new Equipment(player);
		final int playerId = player.getObjectId();
		final int storage = 0;
		final int equipped = 1;

		try {
			for (InventoryPo po : mapper.selectListByQuery(QueryWrapper.create()
					.where(InventoryPo::getItemOwner).eq(playerId)
					.and(InventoryPo::getItemLocation).eq(storage)
					.and(InventoryPo::getEquiped).eq(equipped))) {
				Item item = constructItem(storage, po);
				item.setPersistentState(PersistentState.UPDATED);
				equipment.onLoadHandler(item);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ce1e87b43e9b", playerId), e);
		}

		return equipment;
	}

	/**
	 * 按玩家 ID 加载已装备物品列表。
	 * Loads equipped items as a list by player id.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 已装备物品列表 / equipped item list
	 */
	@Override
	public List<Item> loadEquipment(int playerId) {
		final List<Item> items = new ArrayList<>();
		final int storage = 0;
		final int equipped = 1;

		try {
			for (InventoryPo po : mapper.selectListByQuery(QueryWrapper.create()
					.where(InventoryPo::getItemOwner).eq(playerId)
					.and(InventoryPo::getItemLocation).eq(storage)
					.and(InventoryPo::getEquiped).eq(equipped))) {
				items.add(constructItem(storage, po));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ce1e87b43e9b", playerId), e);
		}

		return items;
	}

	/**
	 * 由物品行构建物品实例。
	 * Builds an Item instance from an inventory row.
	 *
	 * @param storage 存储位置（item_location） / storage location
	 * @param po      物品行 / inventory row
	 * @return 物品 / item
	 */
	private Item constructItem(final int storage, InventoryPo po) {
		return new Item(po.getItemUniqueId(), po.getItemId(), po.getItemCount(), po.getItemColor(),
			intValue(po.getColorExpires()), po.getItemCreator(), intValue(po.getExpireTime()),
			intValue(po.getActivationCount()), Boolean.TRUE.equals(po.getEquiped()),
			Boolean.TRUE.equals(po.getSoulBound()), po.getSlot(), storage, intValue(po.getEnchant()),
			intValue(po.getEnchantBonus()), intValue(po.getItemSkin()), intValue(po.getFusionedItem()),
			intValue(po.getOptionalSocket()), intValue(po.getOptionalFusionSocket()), intValue(po.getCharge()),
			intValue(po.getRndBonus()), intValue(po.getRndCount()), intValue(po.getWrappableCount()),
			Boolean.TRUE.equals(po.getPacked()), intValue(po.getTemperingLevel()),
			Boolean.TRUE.equals(po.getTopped()), intValue(po.getStrengthenSkill()), intValue(po.getSkinSkill()),
			Boolean.TRUE.equals(po.getLunaReskin()), intValue(po.getReductionLevel()), intValue(po.getSeal()),
			Boolean.TRUE.equals(po.getIsEnhance()), intValue(po.getEnhanceSkillId()),
			intValue(po.getEnhanceSkillEnchant()));
	}

	/**
	 * 按玩家 ID 解析账号 ID（账号仓库归属）。
	 * Resolves the account id for a player (account warehouse ownership).
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 账号 ID；缺失或出错时返回 0 / account id, or 0 if none/error
	 */
	private int loadPlayerAccountId(final int playerId) {
		try {
			Integer accountId = mapper.selectAccountId(playerId);
			return accountId == null ? 0 : accountId;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8cf69332a5ea", playerId), e);
			return 0;
		}
	}

	/**
	 * 按玩家 ID 加载所属军团 ID。
	 * Loads the legion id for the given player.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 军团 ID；未加入或出错时返回 0 / legion id, or 0 if none/error
	 */
	private int loadLegionId(final int playerId) {
		try {
			Integer legionId = mapper.selectLegionId(playerId);
			return legionId == null ? 0 : legionId;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13d51b51a32f", playerId), e);
			return 0;
		}
	}

	/**
	 * 持久化玩家全部脏物品（含账号仓/军团仓归属）。
	 * Persists all dirty items of the player (resolves account/legion ownership).
	 *
	 * @param player 玩家 / player
	 * @return 是否全部成功 / whether all operations succeeded
	 */
	@Override
	public boolean store(Player player) {
		int playerId = player.getObjectId();
		Integer accountId = player.getPlayerAccount() != null ? player.getPlayerAccount().getId() : null;
		Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
		List<Item> allPlayerItems = player.getDirtyItemsToUpdate();
		if (allPlayerItems.isEmpty()) {
			player.markDirtyItemContainersStored();
			return true;
		}
		if (!store(allPlayerItems, playerId, accountId, legionId)) {
			return false;
		}
		player.markDirtyItemContainersStored();
		return true;
	}

	/**
	 * 持久化单个物品（绑定玩家账号与军团）。
	 * Persists a single item bound to the player's account and legion.
	 *
	 * @param item   物品 / item
	 * @param player 玩家 / player
	 * @return 是否成功 / whether succeeded
	 */
	@Override
	public boolean store(Item item, Player player) {
		int playerId = player.getObjectId();
		Integer accountId = player.getPlayerAccount() != null ? player.getPlayerAccount().getId() : null;
		Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
		return store(item, playerId, accountId, legionId);
	}

	/**
	 * 按玩家 ID 持久化物品列表；按需解析账号仓/军团仓归属。
	 * Persists an item list by player id; resolves account/legion ownership when needed.
	 *
	 * @param items    物品列表 / item list
	 * @param playerId 玩家 ID / player id
	 * @return 是否成功 / whether succeeded
	 */
	@Override
	public boolean store(List<Item> items, int playerId) {
		Integer accountId = null;
		Integer legionId = null;

		for (Item item : items) {
			if (accountId == null && item.getItemLocation() == StorageType.ACCOUNT_WAREHOUSE.getId()) {
				accountId = loadPlayerAccountId(playerId);
			}

			if (legionId == null && item.getItemLocation() == StorageType.LEGION_WAREHOUSE.getId()) {
				int localLegionId = loadLegionId(playerId);
				if (localLegionId > 0) {
					legionId = localLegionId;
				}
			}
		}
		return store(items, playerId, accountId, legionId);
	}

	/**
	 * 按持久化状态批量插入/更新/删除物品（事务提交）。
	 * Batch inserts/updates/deletes items by persistent state within a transaction.
	 *
	 * @param items     物品列表 / item list
	 * @param playerId  玩家 ID / player id
	 * @param accountId 账号 ID（账号仓用） / account id (for account warehouse)
	 * @param legionId  军团 ID（军团仓用） / legion id (for legion warehouse)
	 * @return 是否全部成功 / whether all operations succeeded
	 */
	@Override
	public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
		if (items == null || items.isEmpty()) {
			return true;
		}
		try {
			DatabaseAccess.transactionTemplate()
				.executeWithoutResult(_ -> storeInTransaction(items, playerId, accountId, legionId));
		} catch (RuntimeException e) {
			log.error(I18n.get("log.c240e77dc213", playerId), e);
			return false;
		}

		markStored(items);
		return true;
	}

	@Override
	public void storeInTransaction(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
		Collection<Item> itemsToUpdate = new ArrayList<>();
		Collection<Item> itemsToInsert = new ArrayList<>();
		Collection<Item> itemsToDelete = new ArrayList<>();

		for (Item item : items) {
			if (item != null) {
				PersistentState state = item.getPersistentState();
				if (state == PersistentState.NEW) {
					itemsToInsert.add(item);
				} else if (state == PersistentState.UPDATE_REQUIRED) {
					itemsToUpdate.add(item);
				} else if (state == PersistentState.DELETED) {
					itemsToDelete.add(item);
				}
			}
		}

		deleteItems(itemsToDelete);
		insertItems(itemsToInsert, playerId, accountId, legionId);
		updateItems(itemsToUpdate, playerId, accountId, legionId);
	}

	@Override
	public void markStored(Collection<Item> items) {
		for (Item item : items) {
			if (item != null) {
				boolean deleted = item.getPersistentState() == PersistentState.DELETED;
				item.setPersistentState(PersistentState.UPDATED);
				if (deleted) {
					ItemService.releaseItemId(item);
				}
			}
		}
	}

	/**
	 * 解析物品的归属 ID（玩家/账号仓/军团仓）。
	 * Resolves the owning id of an item (player, account warehouse or legion warehouse).
	 *
	 * @param item      物品 / item
	 * @param playerId  玩家 ID / player id
	 * @param accountId 账号 ID / account id
	 * @param legionId  军团 ID / legion id
	 * @return 归属 ID / owning id
	 */
	private int getItemOwnerId(Item item, Integer playerId, Integer accountId, Integer legionId) {
		if (item.getItemLocation() == StorageType.ACCOUNT_WAREHOUSE.getId()) {
			return accountId != null ? accountId : 0;
		}

		if (item.getItemLocation() == StorageType.LEGION_WAREHOUSE.getId()) {
			return legionId != null ? legionId : playerId;
		}

		return playerId != null ? playerId : 0;
	}

	/**
	 * 在调用方连接上批量插入物品。
	 * Batch-inserts items on the caller's connection.
	 *
	 * @param items     物品集合 / item collection
	 * @param playerId  玩家 ID / player id
	 * @param accountId 账号 ID / account id
	 * @param legionId  军团 ID / legion id
	 */
	private void insertItems(Collection<Item> items, Integer playerId, Integer accountId,
			Integer legionId) {

		if (GenericValidator.isBlankOrNull(items)) {
			return;
		}

		List<InventoryPo> rows = new ArrayList<>(items.size());
		for (Item item : items) {
			rows.add(toPo(item, playerId, accountId, legionId));
		}

		try {
			ensureBatchChanged(mapper.insertBatch(rows), rows.size(), "insert");
		} catch (RuntimeException e) {
			log.error(I18n.get("log.f078e8399e22"), e);
			throw e;
		}
	}


	/**
	 * 在调用方连接上批量更新物品。
	 * Batch-updates items on the caller's connection.
	 *
	 * @param items     物品集合 / item collection
	 * @param playerId  玩家 ID / player id
	 * @param accountId 账号 ID / account id
	 * @param legionId  军团 ID / legion id
	 */
	private void updateItems(Collection<Item> items, Integer playerId, Integer accountId,
			Integer legionId) {

		if (GenericValidator.isBlankOrNull(items)) {
			return;
		}

		try {
			for (Item item : items) {
				InventoryPo po = toPo(item, playerId, accountId, legionId);
				// 原 UPDATE 语句刻意不含 item_id；置空后由 ignoreNulls 排除该列。
				// The legacy UPDATE deliberately omitted item_id; nulling it lets ignoreNulls drop the column.
				po.setItemId(null);
				// 逐行断言「这一行确实被改动」，保留原 JDBC batch 的逐行校验语义。
				// Asserts each row actually changed, preserving the legacy JDBC batch's per-row check.
				int changed = mapper.updateByQuery(po,
					QueryWrapper.create().where(InventoryPo::getItemUniqueId).eq(item.getObjectId()));
				ensureBatchChanged(changed, 1, "update");
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.ac4bea7b080f"), e);
			throw e;
		}
	}


	/**
	 * 在调用方连接上批量删除物品。
	 * Batch-deletes items on the caller's connection.
	 *
	 * @param items 物品集合 / item collection
	 */
	private void deleteItems(Collection<Item> items) {
		if (GenericValidator.isBlankOrNull(items)) {
			return;
		}

		List<Integer> ids = new ArrayList<>(items.size());
		for (Item item : items) {
			ids.add(item.getObjectId());
		}

		try {
			mapper.deleteBatchByIds(ids);
		} catch (RuntimeException e) {
			log.error(I18n.get("log.8c6bf238353d"), e);
			throw e;
		}
	}


	/**
	 * 校验受影响行数等于期望值，任何一行未变更即视为失败。
	 * Validates that the affected row count equals the expectation, failing when a row was not changed.
	 *
	 * @param changed   实际受影响行数 / actually affected rows
	 * @param expected  期望条目数 / expected entry count
	 * @param operation 操作名 / operation name
	 */
	private static void ensureBatchChanged(int changed, int expected, String operation) {
		if (changed != expected) {
			throw new DataAccessException("Inventory " + operation + " batch changed " + changed
				+ " of " + expected + " rows") {
			};
		}
	}

	/**
	 * 由领域物品构造库存持久化实体。
	 * Builds the inventory persistence entity from a domain item.
	 */
	private InventoryPo toPo(Item item, Integer playerId, Integer accountId, Integer legionId) {
		InventoryPo po = new InventoryPo();
		po.setItemUniqueId(item.getObjectId());
		po.setItemId(item.getItemTemplate().getTemplateId());
		po.setItemCount(item.getItemCount());
		po.setItemColor(item.getItemColor());
		po.setColorExpires(item.getColorExpireTime());
		po.setItemCreator(item.getItemCreator());
		po.setExpireTime(item.getExpireTime());
		po.setActivationCount(item.getActivationCount());
		po.setItemOwner(getItemOwnerId(item, playerId, accountId, legionId));
		po.setEquiped(item.isEquipped());
		po.setSoulBound(item.isSoulBound());
		po.setSlot(item.getEquipmentSlot());
		po.setItemLocation(item.getItemLocation());
		po.setEnchant(item.getEnchantLevel());
		po.setEnchantBonus(item.getEnchantBonus());
		po.setItemSkin(item.getItemSkinTemplate().getTemplateId());
		po.setFusionedItem(item.getFusionedItemId());
		po.setOptionalSocket(item.getOptionalSocket());
		po.setOptionalFusionSocket(item.getOptionalFusionSocket());
		po.setCharge(item.getChargePoints());
		po.setRndBonus(item.getBonusNumber());
		po.setRndCount(item.getRandomCount());
		po.setWrappableCount(item.getWrappableCount());
		po.setPacked(item.isPacked());
		po.setTemperingLevel(item.getAuthorize());
		po.setTopped(item.isAmplified());
		po.setStrengthenSkill(item.getAmplificationSkill());
		po.setSkinSkill(item.getItemSkinSkill());
		po.setLunaReskin(item.isLunaReskin());
		po.setReductionLevel(item.getReductionLevel());
		po.setSeal(item.getUnSeal());
		po.setIsEnhance(item.isEnhance());
		po.setEnhanceSkillId(item.getEnhanceSkillId());
		po.setEnhanceSkillEnchant(item.getEnhanceEnchantLevel());
		return po;
	}

	/**
	 * 删除玩家非账号仓库的全部物品。
	 * Deletes all non-account-warehouse items owned by the player.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 是否成功 / whether succeeded
	 */
	@Override
	public boolean deletePlayerItems(final int playerId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(InventoryPo::getItemOwner).eq(playerId)
				.and(InventoryPo::getItemLocation).ne(2));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6a58c54be58e", playerId), e);
			return false;
		}
	}

	/**
	 * 删除指定账号的账号仓库物品。
	 * Deletes account warehouse items for the given account id.
	 *
	 * @param accountId 账号 ID / account id
	 */
	@Override
	public void deleteAccountWH(final int accountId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(InventoryPo::getItemOwner).eq(accountId)
				.and(InventoryPo::getItemLocation).eq(2));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.aa5fb04d48d6", accountId), e);
		}
	}

	/**
	 * 获取已占用的物品唯一 ID 列表，供 ID 工厂使用。
	 * Returns used item unique ids for ID factory allocation.
	 *
	 * @return 已占用 ID 数组；出错时返回空数组 / used id array, or empty on error.
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<InventoryPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().select(InventoryPo::getItemUniqueId));

			int[] result = new int[rows.size()];
			for (int i = 0; i < rows.size(); i++) {
				result[i] = rows.get(i).getItemUniqueId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fc3775ae3813"), e);
			return new int[0];
		}
	}

	/**
	 * 空值安全的整型转换（库内可为 NULL 的整数列按遗留 {@code getInt} 语义取 0）。
	 * Null-safe integer conversion (nullable integer columns yield 0, matching the legacy {@code getInt}).
	 *
	 * @param value 原始值 / raw value
	 * @return 非空整数 / non-null integer
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
