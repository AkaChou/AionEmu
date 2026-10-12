package com.aionemu.gameserver.persistence.dao;

import java.util.Collection;
import java.util.Map;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import com.aionemu.gameserver.persistence.entity.HousePo;
import com.mybatisflex.core.service.IService;

/**
 * 房屋数据访问层：负责存储与加载 houses 数据。
 * Houses data access layer: stores/loads houses data.
 */
public interface HouseDao extends IService<HousePo> {

	/**
	 * 存储房屋。
	 * Stores a house.
	 *
	 * @param house 房屋 / house
	 */
	void storeHouse(House house);

	/**
	 * 加载房屋。
	 * Loads houses.
	 *
	 * @param lands   地块集合 / housing lands
	 * @param studios 是否为工作室 / whether studios
	 * @return 房屋 ID 到房屋的映射 / map of house ID to house
	 */
	Map<Integer, House> loadHouses(Collection<HousingLand> lands, boolean studios);

	/**
	 * 删除玩家的房屋。
	 * Deletes a player's house.
	 *
	 * @param playerId 玩家 ID / player ID
	 */
	void deleteHouse(int playerId);

	/**
	 * 返回本 DAO 已使用的全部房屋 ID。
	 * Returns all house IDs used by this DAO.
	 *
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
