package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.town.Town;

/**
 * 城镇数据访问抽象层。
 * DAO for town persistence by race.
 */
public abstract class TownDAO implements DAO {

	/**
	 * 按种族加载城镇数据。
	 * Loads towns for the given race.
	 *
	 * 阵营 / race
	 * @return 城镇 ID 到城镇对象的映射 / map of town id to town
	 */
	public abstract Map<Integer, Town> load(Race race);

	/**
	 * 保存城镇数据。
	 * Stores a town.
	 *
	 * town
	 */
	public abstract void store(Town town);

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return TownDAO.class.getName();
	}
}
