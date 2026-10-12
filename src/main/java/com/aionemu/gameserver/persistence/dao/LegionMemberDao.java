package com.aionemu.gameserver.persistence.dao;

import java.util.ArrayList;

import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.persistence.entity.LegionMemberPo;
import com.mybatisflex.core.service.IService;

/**
 * 军团成员数据访问层：负责存储与加载 legion_members 数据。
 * Legion member data access layer: stores/loads legion_members data.
 */
public interface LegionMemberDao extends IService<LegionMemberPo> {

	/**
	 * 检查玩家对象 ID 是否已作为军团成员使用。
	 * Returns true if the ID is used, false otherwise.
	 *
	 * @param playerObjId 玩家对象 ID / player object ID
	 * @return 是否已使用 / whether used
	 */
	boolean isIdUsed(int playerObjId);

	/**
	 * 在数据库中创建新军团成员。
	 * Creates a legion member in the DB.
	 *
	 * @param legionMember 军团成员 / legion member
	 */
	void saveNewLegionMember(LegionMember legionMember);

	/**
	 * 将军团成员存储到数据库。
	 * Stores a legion member to the DB.
	 *
	 * @param playerObjId  玩家对象 ID / player object ID
	 * @param legionMember 军团成员 / legion member
	 */
	void storeLegionMember(int playerObjId, LegionMember legionMember);

	/**
	 * 加载军团成员。
	 * Loads a legion member.
	 *
	 * @param playerObjId 玩家对象 ID / player object ID
	 * @return 军团成员 / legion member
	 */
	LegionMember loadLegionMember(int playerObjId);

	/**
	 * 按 ID 加载离线军团成员扩展信息。
	 * Loads an offline legion member by ID.
	 *
	 * @param playerObjId 玩家对象 ID / player object ID
	 * @return 军团成员扩展信息 / extended legion member
	 */
	LegionMemberEx loadLegionMemberEx(int playerObjId);

	/**
	 * 按名称加载离线军团成员扩展信息。
	 * Loads an offline legion member by name.
	 *
	 * @param playerName 玩家名称 / player name
	 * @return 军团成员扩展信息 / extended legion member
	 */
	LegionMemberEx loadLegionMemberEx(String playerName);

	/**
	 * 加载军团的全部成员对象 ID。
	 * Loads all legion member object IDs of a legion.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 成员对象 ID 列表 / member object ID list
	 */
	ArrayList<Integer> loadLegionMembers(int legionId);

	/**
	 * 删除军团成员及相关数据（依赖数据库 CASCADE 删除）。
	 * Removes a legion member and all related data (done by CASCADE deletion).
	 *
	 * @param playerObjId 要删除的军团成员玩家对象 ID / player object ID of the legion member to delete
	 */
	void deleteLegionMember(int playerObjId);

	/**
	 * 返回本 DAO 已使用的全部成员玩家 ID。
	 * Returns all member player IDs used by this DAO.
	 *
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
