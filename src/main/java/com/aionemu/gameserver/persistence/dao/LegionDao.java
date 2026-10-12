package com.aionemu.gameserver.persistence.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.persistence.entity.LegionPo;
import com.mybatisflex.core.service.IService;

/**
 * 军团数据访问层：聚合 legions 主表与公告、徽章、历史、加入申请、仓库等从表。
 * Legion data access layer: aggregates the legions main table with its announcement, emblem, history, join-request
 * and warehouse child data.
 */
public interface LegionDao extends IService<LegionPo> {

	/**
	 * 检查军团名是否已被使用。
	 * Checks whether a legion name is already used.
	 *
	 * @param name 军团名称 / legion name
	 * @return 是否已使用 / whether used
	 */
	boolean isNameUsed(String name);

	/**
	 * 保存新军团。
	 * Saves a new legion.
	 *
	 * @param legion 军团 / legion
	 */
	void saveNewLegion(Legion legion);

	/**
	 * 存储军团数据。
	 * Stores legion data.
	 *
	 * @param legion 军团 / legion
	 */
	void storeLegion(Legion legion);

	/**
	 * 按名称加载军团。
	 * Loads a legion by name.
	 *
	 * @param legionName 军团名称 / legion name
	 * @return 军团 / legion
	 */
	Legion loadLegion(String legionName);

	/**
	 * 按 ID 加载军团。
	 * Loads a legion by ID.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 军团 / legion
	 */
	Legion loadLegion(int legionId);

	/**
	 * 删除军团。
	 * Deletes a legion.
	 *
	 * @param legionId 军团 ID / legion ID
	 */
	void deleteLegion(int legionId);

	/**
	 * 加载军团公告列表。
	 * Loads the legion announcement list.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 时间戳到公告内容的映射 / map of timestamp to announcement message
	 */
	TreeMap<Timestamp, String> loadAnnouncementList(int legionId);

	/**
	 * 保存新的军团公告。
	 * Saves a new legion announcement.
	 *
	 * @param legionId    军团 ID / legion ID
	 * @param currentTime 当前时间 / current time
	 * @param message     公告消息 / announcement message
	 */
	void saveNewAnnouncement(int legionId, Timestamp currentTime, String message);

	/**
	 * 存储军团徽章。
	 * Stores a legion emblem.
	 *
	 * @param legionId     军团 ID / legion ID
	 * @param legionEmblem 军团徽章 / legion emblem
	 */
	void storeLegionEmblem(int legionId, LegionEmblem legionEmblem);

	/**
	 * 删除指定时间的公告。
	 * Removes an announcement by timestamp key.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param key      公告时间戳 / announcement timestamp
	 */
	void removeAnnouncement(int legionId, Timestamp key);

	/**
	 * 加载军团徽章。
	 * Loads a legion emblem.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 军团徽章 / legion emblem
	 */
	LegionEmblem loadLegionEmblem(int legionId);

	/**
	 * 加载军团仓库。
	 * Loads a legion warehouse.
	 *
	 * @param legion 军团 / legion
	 * @return 军团仓库 / legion warehouse
	 */
	LegionWarehouse loadLegionStorage(Legion legion);

	/**
	 * 加载军团历史。
	 * Loads legion history.
	 *
	 * @param legion 军团 / legion
	 */
	void loadLegionHistory(Legion legion);

	/**
	 * 保存新的军团历史记录。
	 * Saves a new legion history entry.
	 *
	 * @param legionId      军团 ID / legion ID
	 * @param legionHistory 军团历史 / legion history
	 */
	void saveNewLegionHistory(int legionId, LegionHistory legionHistory);

	/**
	 * 更新军团描述。
	 * Updates the legion description.
	 *
	 * @param legion 军团 / legion
	 */
	void updateLegionDescription(Legion legion);

	/**
	 * 存储军团加入申请。
	 * Stores a legion join request.
	 *
	 * @param legionJoinRequest 加入申请 / join request
	 */
	void storeLegionJoinRequest(LegionJoinRequest legionJoinRequest);

	/**
	 * 加载军团的全部加入申请。
	 * Loads all join requests for a legion.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 加入申请列表 / join request list
	 */
	List<LegionJoinRequest> loadLegionJoinRequests(int legionId);

	/**
	 * 删除指定玩家对军团的加入申请。
	 * Deletes a legion join request for a player.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param playerId 玩家 ID / player ID
	 */
	void deleteLegionJoinRequest(int legionId, int playerId);

	/**
	 * 获取拥有领地的军团 ID 集合。
	 * Gets IDs of legions that own territories.
	 *
	 * @return 军团 ID 集合 / legion ID collection
	 */
	Collection<Integer> getLegionIdsWithTerritories();

	/**
	 * 返回本 DAO 已使用的全部军团 ID。
	 * Returns all legion IDs used by this DAO.
	 *
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
