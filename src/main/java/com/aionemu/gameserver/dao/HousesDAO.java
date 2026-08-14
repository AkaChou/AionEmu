package com.aionemu.gameserver.dao;

import java.util.Collection;
import java.util.Map;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingLand;

/**
 * 房屋数据访问对象。
 * Houses data access object.
 *
 * @author Rolandas
 */
public abstract class HousesDAO implements IDFactoryAwareDAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return HousesDAO.class.getName();
	}

	/**
	 * 检查是否支持指定数据库版本。
	 * Checks whether the given database version is supported.
	 *
	 * @param databaseName 数据库名称 / database name
	 * @param majorVersion 主版本号 / major version
	 * @param minorVersion 次版本号 / minor version
	 * @return 是否支持 / whether supported
	 */
	public abstract boolean supports(String databaseName, int majorVersion, int minorVersion);

	/**
	 * 检查房屋对象 ID 是否已被使用。
	 * Checks whether a house object ID is already used.
	 *
	 * @param houseObjectId 房屋对象 ID / house object ID
	 * @return 是否已使用 / whether used
	 */
	public abstract boolean isIdUsed(int houseObjectId);

	/**
	 * 存储房屋。
	 * Stores a house.
	 *
	 * @param house 房屋 / house
	 */
	public abstract void storeHouse(House house);

	/**
	 * 加载房屋。
	 * Loads houses.
	 *
	 * @param lands 地块集合 / housing lands
	 * @param studios 是否为工作室 / whether studios
	 * @return 房屋 ID 到房屋的映射 / map of house ID to house
	 */
	public abstract Map<Integer, House> loadHouses(Collection<HousingLand> lands, boolean studios);

	/**
	 * 删除玩家的房屋。
	 * Deletes a player's house.
	 *
	 * @param playerId 玩家 ID / player ID
	 */
	public abstract void deleteHouse(int playerId);
}
