package com.aionemu.gameserver.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;

/**
 * 军团数据访问对象。
 * Legion data access object.
 */
public abstract class LegionDAO implements IDFactoryAwareDAO {
	/**
	 * 检查军团名是否已被使用。
	 * Checks whether a legion name is already used.
	 *
	 * @param name 军团名称 / legion name
	 * @return 是否已使用 / whether used
	 */
	public abstract boolean isNameUsed(String name);

	/**
	 * 保存新军团。
	 * Saves a new legion.
	 *
	 * @param legion 军团 / legion
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean saveNewLegion(Legion legion);

	/**
	 * 存储军团数据。
	 * Stores legion data.
	 *
	 * @param legion 军团 / legion
	 */
	public abstract void storeLegion(Legion legion);

	/**
	 * 按名称加载军团。
	 * Loads a legion by name.
	 *
	 * @param legionName 军团名称 / legion name
	 * @return 军团 / legion
	 */
	public abstract Legion loadLegion(String legionName);

	/**
	 * 按 ID 加载军团。
	 * Loads a legion by ID.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 军团 / legion
	 */
	public abstract Legion loadLegion(int legionId);

	/**
	 * 删除军团。
	 * Deletes a legion.
	 *
	 * @param legionId 军团 ID / legion ID
	 */
	public abstract void deleteLegion(int legionId);

	/**
	 * 加载军团公告列表。
	 * Loads the legion announcement list.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 时间戳到公告内容的映射 / map of timestamp to announcement message
	 */
	public abstract TreeMap<Timestamp, String> loadAnnouncementList(int legionId);

	/**
	 * 保存新的军团公告。
	 * Saves a new legion announcement.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param currentTime 当前时间 / current time
	 * @param message 公告消息 / announcement message
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean saveNewAnnouncement(int legionId, Timestamp currentTime, String message);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public final String getClassName() {
		return LegionDAO.class.getName();
	}

	/**
	 * 存储军团徽章。
	 * Stores a legion emblem.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param legionEmblem 军团徽章 / legion emblem
	 */
	public abstract void storeLegionEmblem(int legionId, LegionEmblem legionEmblem);

	/**
	 * 删除指定时间的公告。
	 * Removes an announcement by timestamp key.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param key 公告时间戳 / announcement timestamp
	 */
	public abstract void removeAnnouncement(int legionId, Timestamp key);

	/**
	 * 加载军团徽章。
	 * Loads a legion emblem.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 军团徽章 / legion emblem
	 */
	public abstract LegionEmblem loadLegionEmblem(int legionId);

	/**
	 * 加载军团仓库。
	 * Loads a legion warehouse.
	 *
	 * @param legion 军团 / legion
	 * @return 军团仓库 / legion warehouse
	 */
	public abstract LegionWarehouse loadLegionStorage(Legion legion);

	/**
	 * 加载军团历史。
	 * Loads legion history.
	 *
	 * @param legion 军团 / legion
	 */
	public abstract void loadLegionHistory(Legion legion);

	/**
	 * 保存新的军团历史记录。
	 * Saves a new legion history entry.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param legionHistory 军团历史 / legion history
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean saveNewLegionHistory(int legionId, LegionHistory legionHistory);

	/**
	 * 更新军团描述。
	 * Updates the legion description.
	 *
	 * @param legion 军团 / legion
	 */
	public abstract void updateLegionDescription(Legion legion);

	/**
	 * 存储军团加入申请。
	 * Stores a legion join request.
	 *
	 * @param legionJoinRequest 加入申请 / join request
	 */
	public abstract void storeLegionJoinRequest(LegionJoinRequest legionJoinRequest);

	/**
	 * 加载军团的全部加入申请。
	 * Loads all join requests for a legion.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @return 加入申请列表 / join request list
	 */
	public abstract List<LegionJoinRequest> loadLegionJoinRequests(int legionId);

	/**
	 * 删除指定玩家对军团的加入申请。
	 * Deletes a legion join request for a player.
	 *
	 * @param legionId 军团 ID / legion ID
	 * @param playerId 玩家 ID / player ID
	 */
	public abstract void deleteLegionJoinRequest(int legionId, int playerId);

	/**
	 * 删除军团加入申请。
	 * Deletes a legion join request.
	 *
	 * @param legionJoinRequest 加入申请 / join request
	 */
	public abstract void deleteLegionJoinRequest(LegionJoinRequest legionJoinRequest);

	/**
	 * 获取拥有领地的军团 ID 集合。
	 * Gets IDs of legions that own territories.
	 *
	 * @return 军团 ID 集合 / legion ID collection
	 */
	public abstract Collection<Integer> getLegionIdsWithTerritories();
}
