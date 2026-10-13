package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemStone;
import com.aionemu.gameserver.model.items.ItemStone.ItemStoneType;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.entity.ItemStonePo;
import com.aionemu.gameserver.persistence.mapper.ItemStoneMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 物品镶嵌石（Mana/God/Fusion/Idian 等）列表数据访问实现。
 * Data access implementation for item stone lists (Mana/God/Fusion/Idian).
 */
@Slf4j
@Service
@Lazy
public class ItemStoneDaoImpl extends ServiceImpl<ItemStoneMapper, ItemStonePo> implements ItemStoneDao {

	/**
	 * 筛选需新增的镶嵌石 / Filter stones to insert
	 */
	private static final Predicate<ItemStone> itemStoneAddPredicate =
		itemStone -> itemStone != null && PersistentState.NEW == itemStone.getPersistentState();

	/**
	 * 筛选需删除的镶嵌石 / Filter stones to delete
	 */
	private static final Predicate<ItemStone> itemStoneDeletedPredicate =
		itemStone -> itemStone != null && PersistentState.DELETED == itemStone.getPersistentState();

	/**
	 * 筛选需更新的镶嵌石 / Filter stones to update
	 */
	private static final Predicate<ItemStone> itemStoneUpdatePredicate =
		itemStone -> itemStone != null && PersistentState.UPDATE_REQUIRED == itemStone.getPersistentState();

	/**
	 * 为武器/防具加载镶嵌石（Mana/God/Fusion/Idian）。
	 * Loads item stones (Mana/God/Fusion/Idian) for weapons and armor.
	 *
	 * @param items 物品集合 / item collection
	 */
	@Override
	public void load(final Collection<Item> items) {
		if (items == null || items.isEmpty()) {
			return;
		}

		try {
			for (Item item : items) {
				if (item.getItemTemplate() == null) {
					continue;
				}

				if (!item.getItemTemplate().isArmor() && !item.getItemTemplate().isWeapon()) {
					continue;
				}

				for (ItemStonePo po : mapper.selectListByQuery(
						QueryWrapper.create().where(ItemStonePo::getItemUniqueId).eq(item.getObjectId()))) {
					int itemId = po.getItemId();
					int slot = po.getSlot();
					int stoneType = po.getCategory();

					switch (stoneType) {
						case 0: // 魔石。 / ManaStone
							if (item.getSockets(false) <= item.getItemStonesSize()) {
								if (EnchantsConfig.CLEAN_STONE) {
									deleteItemStone(item.getObjectId(), slot, stoneType);
								}
								continue;
							}
							item.getItemStones()
								.add(new ManaStone(item.getObjectId(), itemId, slot, PersistentState.UPDATED));
							break;

						case 1: // 神石。 / GodStone
							item.setGodStone(new GodStone(item.getObjectId(), itemId, po.getProcCount(),
								PersistentState.UPDATED));
							break;

						case 2: // 融合石。 / FusionStone
							if (item.getSockets(true) <= item.getFusionStonesSize()) {
								if (EnchantsConfig.CLEAN_STONE) {
									deleteItemStone(item.getObjectId(), slot, stoneType);
								}
								continue;
							}
							item.getFusionStones()
								.add(new ManaStone(item.getObjectId(), itemId, slot, PersistentState.UPDATED));
							break;

						case 3: // IdianStone
							item.setIdianStone(new IdianStone(itemId, PersistentState.UPDATE_REQUIRED, item,
								intValue(po.getPolishNumber()), intValue(po.getPolishCharge())));
							break;

						default:
							log.warn(I18n.get("log.92b064ec3143", stoneType, item.getObjectId()));
					}
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.112e060c3cda"), e);
		}
	}

	/**
	 * 保存物品上的全部镶嵌石变更。
	 * Saves all item-stone changes for the given items.
	 *
	 * @param items 物品列表 / item list
	 */
	@Override
	public void save(List<Item> items) {
		if (GenericValidator.isBlankOrNull(items)) {
			return;
		}

		Set<ManaStone> manaStones = Sets.newHashSet();
		Set<ManaStone> fusionStones = Sets.newHashSet();
		Set<GodStone> godStones = Sets.newHashSet();
		Set<IdianStone> idianStones = Sets.newHashSet();
		int phantomNullItems = 0;

		for (Item item : items) {
			if (item == null) {
				phantomNullItems++;
				continue;
			}
			if (item.hasManaStones()) {
				manaStones.addAll(item.getItemStones());
			}
			if (item.hasFusionStones()) {
				fusionStones.addAll(item.getFusionStones());
			}

			GodStone godStone = item.getGodStone();
			if (godStone != null) {
				godStones.add(godStone);
			}

			IdianStone idianStone = item.getIdianStone();
			if (idianStone != null) {
				idianStones.add(idianStone);
			}
		}

		if (phantomNullItems > 0) {
			log.warn(I18n.get("log.item_stone_save_skip_null_items", phantomNullItems, items.size(),
				Thread.currentThread().getName()));
		}

		store(manaStones, ItemStoneType.MANASTONE);
		store(fusionStones, ItemStoneType.FUSIONSTONE);
		store(godStones, ItemStoneType.GODSTONE);
		store(idianStones, ItemStoneType.IDIANSTONE);
	}

	/**
	 * 持久化 Mana 石集合。
	 * Persists a set of mana stones.
	 *
	 * @param manaStones 魔石集合 / mana stone set
	 */
	@Override
	public void storeManaStones(Set<ManaStone> manaStones) {
		store(manaStones, ItemStoneType.MANASTONE);
	}

	/**
	 * 持久化融合石集合。
	 * Persists a set of fusion stones.
	 *
	 * @param fusionStones 融合石集合 / fusion stone set
	 */
	@Override
	public void storeFusionStones(Set<ManaStone> fusionStones) {
		store(fusionStones, ItemStoneType.FUSIONSTONE);
	}

	/**
	 * 持久化单颗 Idian 石。
	 * Persists a single Idian stone.
	 *
	 * @param idianStone Idian stone
	 */
	@Override
	public void storeIdianStones(IdianStone idianStone) {
		store(Collections.singleton(idianStone), ItemStoneType.IDIANSTONE);
	}

	/**
	 * 按持久化状态批量增删改镶嵌石。
	 * Batch inserts/updates/deletes stones according to persistent state.
	 *
	 * @param stones 镶嵌石集合 / stone set
	 * @param ist    镶嵌石类型 / item stone type
	 */
	private void store(Set<? extends ItemStone> stones, ItemStoneType ist) {
		if (GenericValidator.isBlankOrNull(stones)) {
			return;
		}

		Set<? extends ItemStone> stonesToAdd = Sets.filter(stones, itemStoneAddPredicate);
		Set<? extends ItemStone> stonesToDelete = Sets.filter(stones, itemStoneDeletedPredicate);
		Set<? extends ItemStone> stonesToUpdate = Sets.filter(stones, itemStoneUpdatePredicate);

		if (stonesToAdd.isEmpty() && stonesToDelete.isEmpty() && stonesToUpdate.isEmpty()) {
			return;
		}

		try {
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				deleteItemStones(stonesToDelete, ist);
				addItemStones(stonesToAdd, ist);
				updateItemStones(stonesToUpdate, ist);
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.441d0f828363"), e);
			return;
		}

		for (ItemStone is : stones) {
			is.setPersistentState(PersistentState.UPDATED);
		}
	}

	/**
	 * 批量插入镶嵌石。
	 * Batch-inserts item stones.
	 *
	 * @param itemStones 镶嵌石集合 / stone collection
	 * @param ist        镶嵌石类型 / item stone type
	 */
	private void addItemStones(Collection<? extends ItemStone> itemStones, ItemStoneType ist) {
		if (GenericValidator.isBlankOrNull(itemStones)) {
			return;
		}

		try {
			List<ItemStonePo> rows = new ArrayList<>(itemStones.size());
			for (ItemStone is : itemStones) {
				ItemStonePo po = new ItemStonePo();
				po.setItemUniqueId(is.getItemObjId());
				po.setItemId(is.getItemId());
				po.setSlot(is.getSlot());
				po.setCategory(ist.ordinal());
				po.setPolishNumber(is instanceof IdianStone stone ? stone.getPolishNumber() : 0);
				po.setPolishCharge(is instanceof IdianStone stone ? stone.getPolishCharge() : 0);
				po.setProcCount(is instanceof GodStone godStone ? godStone.getActivatedCount() : 0);
				rows.add(po);
			}
			mapper.insertBatch(rows);
		} catch (RuntimeException e) {
			log.error(I18n.get("log.ff24b8461da0"), e);
			throw e;
		}
	}

	/**
	 * 批量更新镶嵌石。
	 * Batch-updates item stones.
	 *
	 * @param itemStones 镶嵌石集合 / stone collection
	 * @param ist        镶嵌石类型 / item stone type
	 */
	private void updateItemStones(Collection<? extends ItemStone> itemStones, ItemStoneType ist) {
		if (GenericValidator.isBlankOrNull(itemStones)) {
			return;
		}

		try {
			for (ItemStone is : itemStones) {
				// 原 UPDATE 的 WHERE 仅含 item_unique_id + category，且显式写 slot；故 slot 必须留在 SET 中。
				// The legacy UPDATE keyed only on item_unique_id + category and wrote slot explicitly,
				// so slot must stay in the SET list.
				UpdateChain.of(ItemStonePo.class)
					.set(ItemStonePo::getItemId, is.getItemId())
					.set(ItemStonePo::getSlot, is.getSlot())
					.set(ItemStonePo::getPolishNumber,
						is instanceof IdianStone stone ? stone.getPolishNumber() : 0)
					.set(ItemStonePo::getPolishCharge,
						is instanceof IdianStone stone ? stone.getPolishCharge() : 0)
					.set(ItemStonePo::getProcCount,
						is instanceof GodStone godStone ? godStone.getActivatedCount() : 0)
					.where(ItemStonePo::getItemUniqueId).eq(is.getItemObjId())
					.and(ItemStonePo::getCategory).eq(ist.ordinal())
					.update();
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.9292d96bede8"), e);
			throw e;
		}
	}

	/**
	 * 批量删除镶嵌石。
	 * Batch-deletes item stones.
	 *
	 * @param itemStones 镶嵌石集合 / stone collection
	 * @param ist        镶嵌石类型 / item stone type
	 */
	private void deleteItemStones(Collection<? extends ItemStone> itemStones, ItemStoneType ist) {
		if (GenericValidator.isBlankOrNull(itemStones)) {
			return;
		}

		try {
			for (ItemStone is : itemStones) {
				mapper.deleteByQuery(QueryWrapper.create()
					.where(ItemStonePo::getItemUniqueId).eq(is.getItemObjId())
					.and(ItemStonePo::getSlot).eq(is.getSlot())
					.and(ItemStonePo::getCategory).eq(ist.ordinal()));
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.e2ee88575c70"), e);
			throw e;
		}
	}

	/**
	 * 删除单颗镶嵌石（用于超限清理）。
	 * Deletes a single item stone (used when sockets overflow).
	 *
	 * @param uid      物品唯一 ID / item unique id
	 * @param slot     槽位 / slot
	 * @param category 类别 / category
	 */
	private void deleteItemStone(int uid, int slot, int category) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(ItemStonePo::getItemUniqueId).eq(uid)
				.and(ItemStonePo::getSlot).eq(slot)
				.and(ItemStonePo::getCategory).eq(category));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.48e07380c52b"), e);
			throw e;
		}
	}

	/**
	 * 空值安全的整型转换（库内可为 NULL 的整数列按遗留 {@code getInt} 语义取 0）。
	 * Null-safe integer conversion (nullable integer columns yield 0, matching the legacy {@code getInt}).
	 * @param value 原始值 / raw value
	 * @return 非空整数 / non-null integer
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
