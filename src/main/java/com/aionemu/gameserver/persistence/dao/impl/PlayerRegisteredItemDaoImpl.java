package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.persistence.dao.PlayerRegisteredItemDao;
import com.aionemu.gameserver.persistence.entity.PlayerRegisteredItemPo;
import com.aionemu.gameserver.persistence.mapper.PlayerRegisteredItemMapper;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.item.HouseObjectFactory;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

/**
 * 玩家已注册房屋物品数据访问实现。
 * Data access implementation for player house registered items.
 */
@Slf4j
@Service
@Lazy
public class PlayerRegisteredItemDaoImpl extends ServiceImpl<PlayerRegisteredItemMapper, PlayerRegisteredItemPo>
		implements PlayerRegisteredItemDao {

	/**
	 * 获取已占用的物品唯一 ID 列表，供 ID 工厂回收/分配使用。
	 * Returns used item unique ids for ID factory allocation.
	 * @return 已占用 ID 数组；出错时返回空数组 / used id array, or empty on error.
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<PlayerRegisteredItemPo> rows = mapper.selectListByQuery(
				QueryWrapper.create().select(PlayerRegisteredItemPo::getItemUniqueId)
					.where(PlayerRegisteredItemPo::getItemUniqueId).ne(0));

			int[] result = new int[rows.size()];
			for (int i = 0; i < rows.size(); i++) {
				result[i] = rows.get(i).getItemUniqueId();
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0bebcd82021c"), e);
			return new int[0];
		}
	}

	/**
	 * 加载玩家房屋注册物品与装饰部件到房屋注册表。
	 * Loads the player's house registered items and decorations into the registry.
	 * @param playerId 玩家对象 ID / player object id
	 */
	@Override
	public void loadRegistry(int playerId) {
		House house = HousingService.getInstance().getPlayerStudio(playerId);
		if (house == null) {
			int address = HousingService.getInstance().getPlayerAddress(playerId);
			house = HousingService.getInstance().getHouseByAddress(address);
		}

		if (house == null) {
			log.warn(I18n.get("log.17502383de83", playerId));
			return;
		}

		HouseRegistry registry = house.getRegistry();

		try {
			HashMap<PartType, List<HouseDecoration>> usedParts = new HashMap<>();

			for (PlayerRegisteredItemPo po : mapper.selectListByQuery(
					QueryWrapper.create().where(PlayerRegisteredItemPo::getPlayerId).eq(playerId))) {
				String area = po.getArea();
				if ("DECOR".equals(area)) {
					HouseDecoration dec = createDecoration(po);
					registry.putCustomPart(dec);

					if (dec.isUsed()) {
						if (house.getHouseType() != HouseType.PALACE && dec.getFloor() > 0) {
							dec.setFloor(0);
						}

						List<HouseDecoration> usedForType =
							usedParts.computeIfAbsent(dec.getTemplate().getType(), _ -> new ArrayList<>());
						usedForType.add(dec);
					}

					dec.setPersistentState(PersistentState.UPDATED);
				} else {
					HouseObject<?> obj = constructObject(registry, house, po);
					registry.putObject(obj);
					obj.setPersistentState(PersistentState.UPDATED);
				}
			}

			for (PartType partType : PartType.values()) {
				if (usedParts.containsKey(partType)) {
					for (HouseDecoration usedDeco : usedParts.get(partType)) {
						registry.setPartInUse(usedDeco, usedDeco.getFloor());
					}
					continue;
				}

				int floorCount = 1;
				if (house.getHouseType() == HouseType.PALACE &&
					(partType == PartType.INFLOOR_ANY || partType == PartType.INWALL_ANY)) {
					floorCount = 6;
				}

				for (int i = 0; i < floorCount; i++) {
					HouseDecoration def = registry.getDefaultPartByType(partType, i);
					if (def != null) {
						registry.setPartInUse(def, i);
					}
				}
			}

			registry.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.98d5b9d41325", playerId), e);
		}
	}

	/**
	 * 由注册物品行构建房屋对象。
	 * Builds a house object from a registered item row.
	 * @param registry 房屋注册表 / house registry
	 * @param house 房屋 / house
	 * @param po 注册物品行 / registered item row
	 * @return 房屋对象 / house object
	 */
	private HouseObject<?> constructObject(final HouseRegistry registry, House house, PlayerRegisteredItemPo po) {
		int itemUniqueId = po.getItemUniqueId();
		VisibleObject visObj = World.getInstance().findVisibleObject(itemUniqueId);
		HouseObject<?> obj;

		if (visObj != null) {
			if (visObj instanceof HouseObject<?>) {
				obj = (HouseObject<?>) visObj;
			} else {
				throw new IllegalAccessError("Invalid object type for house object id: " + itemUniqueId);
			}
		} else {
			obj = registry.getObjectByObjId(itemUniqueId);
			if (obj == null) {
				obj = HouseObjectFactory.createNew(house, itemUniqueId, po.getItemId());
			}
		}

		obj.setOwnerUsedCount(intValue(po.getOwnerUseCount()));
		obj.setVisitorUsedCount(intValue(po.getVisitorUseCount()));
		obj.setX(po.getX());
		obj.setY(po.getY());
		obj.setZ(po.getZ());
		obj.setHeading((byte) intValue(po.getH()));
		obj.setColor(intValue(po.getColor()));
		obj.setColorExpireEnd(intValue(po.getColorExpires()));

		if (obj.getObjectTemplate().getUseDays() > 0) {
			obj.setExpireTime(intValue(po.getExpireTime()));
		}

		return obj;
	}

	/**
	 * 由注册物品行构建装饰部件。
	 * Builds a decoration part from a registered item row.
	 * @param po 注册物品行 / registered item row
	 * @return 装饰部件 / decoration part
	 */
	private HouseDecoration createDecoration(PlayerRegisteredItemPo po) {
		HouseDecoration decor = new HouseDecoration(po.getItemUniqueId(), po.getItemId(), po.getFloor());
		decor.setUsed(intValue(po.getOwnerUseCount()) > 0);
		return decor;
	}

	/**
	 * 按持久化状态批量保存房屋注册表（增删改），事务提交后更新内存状态并释放已删 ID。
	 * Persists the house registry by persistent state (insert/update/delete), then updates in-memory state
	 * and releases deleted ids.
	 * @param registry 房屋注册表 / house registry
	 * @param playerId 玩家对象 ID / player object id
	 * @return 是否保存成功 / whether store succeeded
	 */
	@Override
	public boolean store(HouseRegistry registry, int playerId) {
		List<HouseObject<?>> objects = registry.getObjects();
		List<HouseDecoration> decors = registry.getAllParts();

		Collection<HouseObject<?>> objectsToAdd = new ArrayList<>();
		Collection<HouseObject<?>> objectsToUpdate = new ArrayList<>();
		Collection<HouseObject<?>> objectsToDelete = new ArrayList<>();

		Collection<HouseDecoration> partsToAdd = new ArrayList<>();
		Collection<HouseDecoration> partsToUpdate = new ArrayList<>();
		Collection<HouseDecoration> partsToDelete = new ArrayList<>();

		// 过滤对象 / Filter objects
		for (HouseObject<?> obj : objects) {
			if (obj != null) {
				PersistentState state = obj.getPersistentState();
				if (state == PersistentState.NEW) {
					objectsToAdd.add(obj);
				} else if (state == PersistentState.UPDATE_REQUIRED) {
					objectsToUpdate.add(obj);
				} else if (state == PersistentState.DELETED) {
					objectsToDelete.add(obj);
				}
			}
		}

		// 过滤装饰 / Filter decorations
		for (HouseDecoration dec : decors) {
			if (dec != null) {
				PersistentState state = dec.getPersistentState();
				if (state == PersistentState.NEW) {
					partsToAdd.add(dec);
				} else if (state == PersistentState.UPDATE_REQUIRED) {
					partsToUpdate.add(dec);
				} else if (state == PersistentState.DELETED) {
					partsToDelete.add(dec);
				}
			}
		}

		try {
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				deleteObjects(objectsToDelete);
				deleteParts(partsToDelete);
				storeObjects(objectsToUpdate, playerId, false);
				storeParts(partsToUpdate, playerId, false);
				storeObjects(objectsToAdd, playerId, true);
				storeParts(partsToAdd, playerId, true);
			});
			registry.setPersistentState(PersistentState.UPDATED);
		} catch (RuntimeException e) {
			log.error(I18n.get("log.7c25eacf0aac", playerId), e);
			return false;
		}

		// 更新状态 / Update states
		for (HouseObject<?> obj : objects) {
			if (obj != null) {
				if (obj.getPersistentState() == PersistentState.DELETED) {
					registry.discardObject(obj.getObjectId());
				} else {
					obj.setPersistentState(PersistentState.UPDATED);
				}
			}
		}

		for (HouseDecoration decor : decors) {
			if (decor != null) {
				if (decor.getPersistentState() == PersistentState.DELETED) {
					registry.discardPart(decor);
				} else {
					decor.setPersistentState(PersistentState.UPDATED);
				}
			}
		}

		// 释放 ID / Release IDs
		if (!objectsToDelete.isEmpty()) {
			for (HouseObject<?> obj : objectsToDelete) {
				if (obj != null && obj.getObjectId() != 0) {
					IDFactory.getInstance().releaseId(obj.getObjectId());
				}
			}
		}

		if (!partsToDelete.isEmpty()) {
			for (HouseDecoration part : partsToDelete) {
				if (part != null && part.getObjectId() != 0) {
					IDFactory.getInstance().releaseId(part.getObjectId());
				}
			}
		}

		return true;
	}

	/**
	 * 批量写入房屋对象的摆放与归属数据（调用方连接执行）。
	 * Batch-writes house object placement and ownership data on the caller's connection.
	 * @param objects 房屋对象集合 / house object collection
	 * @param playerId 玩家对象 ID / player object id
	 * @param isNew 是否新增（否则为更新） / true to insert, false to update
	 */
	private void storeObjects(Collection<HouseObject<?>> objects, int playerId, boolean isNew) {
		if (GenericValidator.isBlankOrNull(objects)) {
			return;
		}

		List<PlayerRegisteredItemPo> rows = new ArrayList<>(objects.size());
		for (HouseObject<?> obj : objects) {
			if (obj == null) {
				continue;
			}
			rows.add(toPo(obj, playerId));
		}
		if (isNew) {
			if (!rows.isEmpty()) {
				mapper.insertBatch(rows);
			}
			return;
		}
		for (PlayerRegisteredItemPo row : rows) {
			// 原 UPDATE 只写 11 个非主键列，且显式写入 NULL；UpdateChain 逐列还原该 SET 列表。
			// The legacy UPDATE wrote only the 11 non-key columns and wrote NULLs explicitly;
			// UpdateChain reproduces that SET list column by column.
			UpdateChain.of(PlayerRegisteredItemPo.class)
				.set(PlayerRegisteredItemPo::getExpireTime, row.getExpireTime())
				.set(PlayerRegisteredItemPo::getColor, row.getColor())
				.set(PlayerRegisteredItemPo::getColorExpires, row.getColorExpires())
				.set(PlayerRegisteredItemPo::getOwnerUseCount, row.getOwnerUseCount())
				.set(PlayerRegisteredItemPo::getVisitorUseCount, row.getVisitorUseCount())
				.set(PlayerRegisteredItemPo::getX, row.getX())
				.set(PlayerRegisteredItemPo::getY, row.getY())
				.set(PlayerRegisteredItemPo::getZ, row.getZ())
				.set(PlayerRegisteredItemPo::getH, row.getH())
				.set(PlayerRegisteredItemPo::getArea, row.getArea())
				.set(PlayerRegisteredItemPo::getFloor, row.getFloor())
				.where(PlayerRegisteredItemPo::getPlayerId).eq(row.getPlayerId())
				.and(PlayerRegisteredItemPo::getItemUniqueId).eq(row.getItemUniqueId())
				.and(PlayerRegisteredItemPo::getItemId).eq(row.getItemId())
				.update();
		}
	}

	/**
	 * 由房屋对象构造注册物品实体。
	 * Builds the registered-item entity from a house object.
	 */
	private static PlayerRegisteredItemPo toPo(HouseObject<?> obj, int playerId) {
		PlayerRegisteredItemPo po = new PlayerRegisteredItemPo();
		po.setExpireTime(obj.getExpireTime() > 0 ? obj.getExpireTime() : null);
		po.setColor(obj.getColor() == null ? null : obj.getColor());
		po.setColorExpires(obj.getColorExpireEnd());
		po.setOwnerUseCount(obj.getOwnerUsedCount());
		po.setVisitorUseCount(obj.getVisitorUsedCount());
		po.setX(obj.getX());
		po.setY(obj.getY());
		po.setZ(obj.getZ());
		po.setH((int) obj.getHeading());
		po.setArea(obj.getX() > 0 || obj.getY() > 0 || obj.getZ() > 0 ? obj.getPlaceArea().toString() : "NONE");
		po.setFloor((byte) 0);
		po.setPlayerId(playerId);
		po.setItemUniqueId(obj.getObjectId());
		po.setItemId(obj.getObjectTemplate().getTemplateId());
		return po;
	}

	/**
	 * 批量写入房屋装饰部件数据（调用方连接执行）。
	 * Batch-writes house decoration part data on the caller's connection.
	 * @param parts 装饰部件集合 / decoration part collection
	 * @param playerId 玩家对象 ID / player object id
	 * @param isNew 是否新增（否则为更新） / true to insert, false to update
	 */
	private void storeParts(Collection<HouseDecoration> parts, int playerId, boolean isNew) {
		if (GenericValidator.isBlankOrNull(parts)) {
			return;
		}

		List<PlayerRegisteredItemPo> rows = new ArrayList<>(parts.size());
		for (HouseDecoration part : parts) {
			if (part == null) {
				continue;
			}
			rows.add(toPo(part, playerId));
		}
		if (isNew) {
			if (!rows.isEmpty()) {
				mapper.insertBatch(rows);
			}
			return;
		}
		for (PlayerRegisteredItemPo row : rows) {
			UpdateChain.of(PlayerRegisteredItemPo.class)
				.set(PlayerRegisteredItemPo::getExpireTime, row.getExpireTime())
				.set(PlayerRegisteredItemPo::getColor, row.getColor())
				.set(PlayerRegisteredItemPo::getColorExpires, row.getColorExpires())
				.set(PlayerRegisteredItemPo::getOwnerUseCount, row.getOwnerUseCount())
				.set(PlayerRegisteredItemPo::getVisitorUseCount, row.getVisitorUseCount())
				.set(PlayerRegisteredItemPo::getX, row.getX())
				.set(PlayerRegisteredItemPo::getY, row.getY())
				.set(PlayerRegisteredItemPo::getZ, row.getZ())
				.set(PlayerRegisteredItemPo::getH, row.getH())
				.set(PlayerRegisteredItemPo::getArea, row.getArea())
				.set(PlayerRegisteredItemPo::getFloor, row.getFloor())
				.where(PlayerRegisteredItemPo::getPlayerId).eq(row.getPlayerId())
				.and(PlayerRegisteredItemPo::getItemUniqueId).eq(row.getItemUniqueId())
				.and(PlayerRegisteredItemPo::getItemId).eq(row.getItemId())
				.update();
		}
	}

	/**
	 * 由装饰部件构造注册物品实体。
	 * Builds the registered-item entity from a decoration part.
	 */
	private static PlayerRegisteredItemPo toPo(HouseDecoration part, int playerId) {
		PlayerRegisteredItemPo po = new PlayerRegisteredItemPo();
		po.setExpireTime(null);
		po.setColor(null);
		po.setColorExpires(0);
		po.setOwnerUseCount(part.isUsed() ? 1 : 0);
		po.setVisitorUseCount(0);
		po.setX(0f);
		po.setY(0f);
		po.setZ(0f);
		po.setH(0);
		po.setArea("DECOR");
		po.setFloor(part.getFloor());
		po.setPlayerId(playerId);
		po.setItemUniqueId(part.getObjectId());
		po.setItemId(part.getTemplate().getId());
		return po;
	}

	/**
	 * 批量删除房屋对象（调用方连接执行）。
	 * Batch-deletes house objects on the caller's connection.
	 * @param objects 房屋对象集合 / house object collection
	 */
	private void deleteObjects(Collection<HouseObject<?>> objects) {
		if (GenericValidator.isBlankOrNull(objects)) {
			return;
		}
		for (HouseObject<?> obj : objects) {
			if (obj == null) {
				continue;
			}
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerRegisteredItemPo::getItemUniqueId).eq(obj.getObjectId()));
		}
	}

	/**
	 * 批量删除房屋装饰部件（调用方连接执行）。
	 * Batch-deletes house decoration parts on the caller's connection.
	 * @param parts 装饰部件集合 / decoration part collection
	 */
	private void deleteParts(Collection<HouseDecoration> parts) {
		if (GenericValidator.isBlankOrNull(parts)) {
			return;
		}
		for (HouseDecoration part : parts) {
			if (part == null) {
				continue;
			}
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerRegisteredItemPo::getItemUniqueId).eq(part.getObjectId()));
		}
	}

	/**
	 * 重置玩家非装饰注册物品的摆放坐标与区域。
	 * Resets placement coordinates and area for the player's non-decoration registered items.
	 * @param playerId 玩家对象 ID / player object id
	 */
	@Override
	public void resetRegistry(int playerId) {
		try {
			log.info(I18n.get("log.5c21c4ee04a7", playerId));
			UpdateChain.of(PlayerRegisteredItemPo.class)
				.set(PlayerRegisteredItemPo::getX, 0f)
				.set(PlayerRegisteredItemPo::getY, 0f)
				.set(PlayerRegisteredItemPo::getZ, 0f)
				.set(PlayerRegisteredItemPo::getH, 0)
				.set(PlayerRegisteredItemPo::getArea, "NONE")
				.where(PlayerRegisteredItemPo::getPlayerId).eq(playerId)
				.and(PlayerRegisteredItemPo::getArea).ne("DECOR")
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7f671b6e18ee", playerId), e);
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
