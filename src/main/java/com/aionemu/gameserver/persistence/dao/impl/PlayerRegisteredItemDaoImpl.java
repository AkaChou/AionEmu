package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
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
import com.aionemu.gameserver.services.item.HouseObjectFactory;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

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
		House house = GameHousingServices.housingService().getPlayerStudio(playerId);
		if (house == null) {
			int address = GameHousingServices.housingService().getPlayerAddress(playerId);
			house = GameHousingServices.housingService().getHouseByAddress(address);
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
		VisibleObject visObj = GameWorldBootstrapServices.world().findVisibleObject(itemUniqueId);
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
				try (Connection con = DatabaseAccess.getConnection()) {
					deleteObjects(con, objectsToDelete);
					deleteParts(con, partsToDelete);
					storeObjects(con, objectsToUpdate, playerId, false);
					storeParts(con, partsToUpdate, playerId, false);
					storeObjects(con, objectsToAdd, playerId, true);
					storeParts(con, partsToAdd, playerId, true);
				} catch (SQLException e) {
					throw new DataAccessException("Failed to store registered items", e) {};
				}
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
					GameWorldBootstrapServices.idFactory().releaseId(obj.getObjectId());
				}
			}
		}

		if (!partsToDelete.isEmpty()) {
			for (HouseDecoration part : partsToDelete) {
				if (part != null && part.getObjectId() != 0) {
					GameWorldBootstrapServices.idFactory().releaseId(part.getObjectId());
				}
			}
		}

		return true;
	}

	/**
	 * 批量写入房屋对象的摆放与归属数据（调用方连接执行）。
	 * Batch-writes house object placement and ownership data on the caller's connection.
	 * @param con 调用方连接 / caller connection
	 * @param objects 房屋对象集合 / house object collection
	 * @param playerId 玩家对象 ID / player object id
	 * @param isNew 是否新增（否则为更新） / true to insert, false to update
	 * @throws SQLException 写入失败时 / when the write fails
	 */
	private void storeObjects(Connection con, Collection<HouseObject<?>> objects, int playerId, boolean isNew)
			throws SQLException {
		if (GenericValidator.isBlankOrNull(objects)) {
			return;
		}

		String query = isNew
			? PlayerRegisteredItemMapper.INSERT_QUERY_JDBC
			: PlayerRegisteredItemMapper.UPDATE_QUERY_JDBC;

		try (PreparedStatement stmt = con.prepareStatement(query)) {
			for (HouseObject<?> obj : objects) {
				if (obj == null) {
					continue;
				}

				if (obj.getExpireTime() > 0) {
					stmt.setInt(1, obj.getExpireTime());
				} else {
					stmt.setNull(1, Types.INTEGER);
				}

				if (obj.getColor() == null) {
					stmt.setNull(2, Types.INTEGER);
				} else {
					stmt.setInt(2, obj.getColor());
				}

				stmt.setInt(3, obj.getColorExpireEnd());
				stmt.setInt(4, obj.getOwnerUsedCount());
				stmt.setInt(5, obj.getVisitorUsedCount());
				stmt.setFloat(6, obj.getX());
				stmt.setFloat(7, obj.getY());
				stmt.setFloat(8, obj.getZ());
				stmt.setInt(9, obj.getHeading());

				if (obj.getX() > 0 || obj.getY() > 0 || obj.getZ() > 0) {
					stmt.setString(10, obj.getPlaceArea().toString());
				} else {
					stmt.setString(10, "NONE");
				}

				stmt.setByte(11, (byte) 0);
				stmt.setInt(12, playerId);
				stmt.setInt(13, obj.getObjectId());
				stmt.setInt(14, obj.getObjectTemplate().getTemplateId());
				stmt.addBatch();
			}

			stmt.executeBatch();
		}
	}

	/**
	 * 批量写入房屋装饰部件数据（调用方连接执行）。
	 * Batch-writes house decoration part data on the caller's connection.
	 * @param con 调用方连接 / caller connection
	 * @param parts 装饰部件集合 / decoration part collection
	 * @param playerId 玩家对象 ID / player object id
	 * @param isNew 是否新增（否则为更新） / true to insert, false to update
	 * @throws SQLException 写入失败时 / when the write fails
	 */
	private void storeParts(Connection con, Collection<HouseDecoration> parts, int playerId, boolean isNew)
			throws SQLException {
		if (GenericValidator.isBlankOrNull(parts)) {
			return;
		}

		String query = isNew
			? PlayerRegisteredItemMapper.INSERT_QUERY_JDBC
			: PlayerRegisteredItemMapper.UPDATE_QUERY_JDBC;

		try (PreparedStatement stmt = con.prepareStatement(query)) {
			for (HouseDecoration part : parts) {
				if (part == null) {
					continue;
				}

				stmt.setNull(1, Types.INTEGER);
				stmt.setNull(2, Types.INTEGER);
				stmt.setInt(3, 0);
				stmt.setInt(4, part.isUsed() ? 1 : 0);
				stmt.setInt(5, 0);
				stmt.setFloat(6, 0);
				stmt.setFloat(7, 0);
				stmt.setFloat(8, 0);
				stmt.setInt(9, 0);
				stmt.setString(10, "DECOR");
				stmt.setByte(11, part.getFloor());
				stmt.setInt(12, playerId);
				stmt.setInt(13, part.getObjectId());
				stmt.setInt(14, part.getTemplate().getId());
				stmt.addBatch();
			}

			stmt.executeBatch();
		}
	}

	/**
	 * 批量删除房屋对象（调用方连接执行）。
	 * Batch-deletes house objects on the caller's connection.
	 * @param con 调用方连接 / caller connection
	 * @param objects 房屋对象集合 / house object collection
	 * @throws SQLException 删除失败时 / when the delete fails
	 */
	private void deleteObjects(Connection con, Collection<HouseObject<?>> objects) throws SQLException {
		if (GenericValidator.isBlankOrNull(objects)) {
			return;
		}

		try (PreparedStatement stmt = con.prepareStatement(PlayerRegisteredItemMapper.DELETE_QUERY_JDBC)) {
			for (HouseObject<?> obj : objects) {
				if (obj == null) {
					continue;
				}
				stmt.setInt(1, obj.getObjectId());
				stmt.addBatch();
			}

			stmt.executeBatch();
		}
	}

	/**
	 * 批量删除房屋装饰部件（调用方连接执行）。
	 * Batch-deletes house decoration parts on the caller's connection.
	 * @param con 调用方连接 / caller connection
	 * @param parts 装饰部件集合 / decoration part collection
	 * @throws SQLException 删除失败时 / when the delete fails
	 */
	private void deleteParts(Connection con, Collection<HouseDecoration> parts) throws SQLException {
		if (GenericValidator.isBlankOrNull(parts)) {
			return;
		}

		try (PreparedStatement stmt = con.prepareStatement(PlayerRegisteredItemMapper.DELETE_QUERY_JDBC)) {
			for (HouseDecoration part : parts) {
				if (part == null) {
					continue;
				}
				stmt.setInt(1, part.getObjectId());
				stmt.addBatch();
			}

			stmt.executeBatch();
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
