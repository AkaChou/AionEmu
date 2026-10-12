package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.persistence.entity.TownPo;
import com.mybatisflex.core.service.IService;

/**
 * 城镇数据访问层。
 * Data access layer for town persistence by race.
 */
public interface TownDao extends IService<TownPo> {

	/**
	 * 按种族加载城镇数据。
	 * Loads towns for the given race.
	 * @param race 阵营 / race
	 * @return 城镇 ID 到城镇对象的映射 / map of town id to town
	 */
	Map<Integer, Town> load(Race race);

	/**
	 * 保存城镇数据。
	 * Stores a town.
	 * @param town 城镇 / town
	 */
	void store(Town town);
}
