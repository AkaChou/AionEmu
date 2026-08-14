package com.aionemu.gameserver.dao;

import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.Petition;

/**
 * 玩家请愿/申诉数据访问对象。
 * Player petition data access object.
 *
 * @author zdead
 */
public abstract class PetitionDAO implements DAO {

	/**
	 * 获取下一个可用的请愿 ID。
	 * Returns the next available petition ID.
	 *
	 * @return 下一个可用 ID / next available id
	 */
	public abstract int getNextAvailableId();

	/**
	 * 插入一条请愿记录。
	 * Inserts a petition record.
	 *
	 * @param p 请愿对象 / petition
	 */
	public abstract void insertPetition(Petition p);

	/**
	 * 按玩家对象 ID 删除请愿。
	 * Deletes petitions by player object ID.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 */
	public abstract void deletePetition(int playerObjId);

	/**
	 * 获取全部请愿集合。
	 * Returns the set of all petitions.
	 *
	 * @return 申诉集合 / set of petitions
	 */
	public abstract Set<Petition> getPetitions();

	/**
	 * 按请愿 ID 获取请愿。
	 * Returns a petition by its ID.
	 *
	 * @param petitionId 申诉 ID / petition id
	 * @return 申诉 / petition
	 */
	public abstract Petition getPetitionById(int petitionId);

	/**
	 * 将请愿标记为已回复。
	 * Marks the petition as replied.
	 *
	 * @param petitionId 申诉 ID / petition id
	 */
	public abstract void setReplied(int petitionId);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PetitionDAO.class.getName();
	}
}
