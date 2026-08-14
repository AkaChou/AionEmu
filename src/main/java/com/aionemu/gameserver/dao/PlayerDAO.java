package com.aionemu.gameserver.dao;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;

/**
 * 玩家核心数据访问对象，负责角色创建、加载、删除与在线状态等。
 * Core player data access object responsible for character creation, loading, deletion and online state.
 */
public abstract class PlayerDAO implements IDFactoryAwareDAO {

	/**
	 * 判断角色名是否已被使用。
	 * Checks whether the character name is already used.
	 *
	 * @param name 角色名 / character name
	 * @return 是否已使用 / true if used
	 */
	public abstract boolean isNameUsed(String name);

	/**
	 * 批量获取玩家对象 ID 对应的角色名。
	 * Returns a map of player object IDs to character names.
	 *
	 * @param playerObjectIds 玩家对象 ID 集合 / collection of player object ids
	 * @return ID 到名称映射 / map of id to name
	 */
	public abstract Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds);

	/**
	 * 将玩家数据写回数据库。
	 * Stores player data back to the database.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void storePlayer(Player player);

	/**
	 * 在调用方事务内持久化玩家公共数据的任务货币列（dp/creativity_point/abyss_favor）。
	 * Persists the quest-currency columns of a player's common data inside the
	 * caller-owned transaction, so currency rewards commit atomically with the
	 * quest state. The caller must own and commit/rollback the connection.
	 *
	 * @param connection 调用方事务连接 / caller-owned transaction connection
	 * @param playerId 玩家 object id / player object id
	 * @param pcd 玩家公共数据 / player common data
	 */
	public void storeInTransaction(Connection connection, int playerId, PlayerCommonData pcd) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * 保存新创建的角色。
	 * Saves a newly created character.
	 *
	 * @param pcd 角色公共数据 / player common data
	 * @param accountId 账号 ID / account id
	 * @param accountName 账号名 / account name
	 * @return 若成功则为 true / true if successful
	 */
	public abstract boolean saveNewPlayer(PlayerCommonData pcd, int accountId, String accountName);

	/**
	 * 按对象 ID 加载角色公共数据。
	 * Loads player common data by object ID.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 公共数据 / common data
	 */
	public abstract PlayerCommonData loadPlayerCommonData(int playerObjId);

	/**
	 * 删除角色。
	 * Deletes a player character.
	 *
	 * @param playerId 玩家 ID / player id
	 */
	public abstract void deletePlayer(int playerId);

	/**
	 * 更新角色删除时间。
	 * Updates the character deletion timestamp.
	 *
	 * @param objectId 对象 ID / object id
	 * @param deletionDate 删除时间戳 / deletion timestamp
	 */
	public abstract void updateDeletionTime(int objectId, Timestamp deletionDate);

	/**
	 * 存储角色创建时间。
	 * Stores the character creation timestamp.
	 *
	 * @param objectId 对象 ID / object id
	 * @param creationDate 创建时间戳 / creation timestamp
	 */
	public abstract void storeCreationTime(int objectId, Timestamp creationDate);

	/**
	 * 为账号数据填充创建/删除时间。
	 * Fills creation/deletion times into account data.
	 *
	 * @param acData 账号角色数据 / player account data
	 */
	public abstract void setCreationDeletionTime(PlayerAccountData acData);

	/**
	 * 获取账号下全部角色对象 ID。
	 * Returns all player object IDs on the account.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 对象 ID 列表 / list of object ids
	 */
	public abstract List<Integer> getPlayerOidsOnAccount(int accountId);

	/**
	 * 存储角色最后在线时间。
	 * Stores the character's last online time.
	 *
	 * @param objectId 对象 ID / object id
	 * @param lastOnline 最后在线时间 / last online timestamp
	 */
	public abstract void storeLastOnlineTime(final int objectId, final Timestamp lastOnline);

	/**
	 * 设置角色在线状态。
	 * Sets the player's online flag.
	 *
	 * @param player 玩家 / player
	 * @param online 在线状态 / online flag
	 */
	public abstract void onlinePlayer(final Player player, final boolean online);

	/**
	 * 批量设置全部角色在线状态（通常用于启动/关闭）。
	 * Sets the online flag for all players (typically on startup/shutdown).
	 *
	 * @param online 在线状态 / online flag
	 */
	public abstract void setPlayersOffline(final boolean online);

	/**
	 * 按角色名加载公共数据。
	 * Loads player common data by character name.
	 *
	 * @param name 角色名 / character name
	 * @return 公共数据 / common data
	 */
	public abstract PlayerCommonData loadPlayerCommonDataByName(String name);

	/**
	 * 按角色名获取账号 ID。
	 * Returns the account ID for the given character name.
	 *
	 * @param name 角色名 / character name
	 * @return 账号 ID / account id
	 */
	public abstract int getAccountIdByName(final String name);

	/**
	 * 按对象 ID 获取角色名。
	 * Returns the character name for the given object ID.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 角色名 / character name
	 */
	public abstract String getPlayerNameByObjId(final int playerObjId);

	/**
	 * 按角色名获取玩家 ID。
	 * Returns the player ID for the given character name.
	 *
	 * @param playerName 角色名 / character name
	 * @return 玩家 ID / player id
	 */
	public abstract int getPlayerIdByName(final String playerName);

	/**
	 * 存储角色名（如改名后写回）。
	 * Stores the player name (e.g. after a rename).
	 *
	 * @param recipientCommonData 角色公共数据 / player common data
	 */
	public abstract void storePlayerName(PlayerCommonData recipientCommonData);

	/**
	 * 获取账号下角色数量。
	 * Returns the character count on the account.
	 *
	 * @param accountId 账号 ID / account id
	 * @return 角色数量 / character count
	 */
	public abstract int getCharacterCountOnAccount(final int accountId);

	/**
	 * 获取指定种族的角色数量。
	 * Returns the character count for the given race.
	 *
	 * @param race 阵营 / race
	 * @return 角色数量 / character count
	 */
	public abstract int getCharacterCountForRace(Race race);

	/**
	 * 获取当前在线玩家数量。
	 * Returns the current online player count.
	 *
	 * @return 在线数量 / online count
	 */
	public abstract int getOnlinePlayerCount();

	/**
	 * 获取待删除角色 ID 列表。
	 * Returns the list of player IDs pending deletion.
	 *
	 * @param paramInt1 延迟参数 1 / delay parameter 1
	 * @param paramInt2 延迟参数 2 / delay parameter 2
	 * @return 玩家 ID 列表 / list of player ids
	 */
	public abstract List<Integer> getPlayersToDelete(int paramInt1, int paramInt2);

	/**
	 * 设置角色最后转服时间。
	 * Sets the player's last server-transfer time.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param time 时间戳 / timestamp
	 */
	public abstract void setPlayerLastTransferTime(final int playerId, final long time);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerDAO.class.getName();
	}

	/**
	 * 获取角色创建时间。
	 * Returns the character creation timestamp.
	 *
	 * @param obj 对象 ID / object id
	 * @return 创建时间戳 / creation timestamp
	 */
	public abstract Timestamp getCharacterCreationDateId(final int obj);

	/**
	 * 更新军团加入申请状态。
	 * Updates the legion join-request state.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param state 申请状态 / join-request state
	 */
	public abstract void updateLegionJoinRequestState(int playerId, LegionJoinRequestState state);

	/**
	 * 清除军团加入申请。
	 * Clears the legion join request.
	 *
	 * @param playerId 玩家 ID / player id
	 */
	public abstract void clearJoinRequest(final int playerId);

	/**
	 * 读取并填充玩家的军团加入申请状态。
	 * Loads and fills the player's legion join-request state.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void getJoinRequestState(Player player);

	/**
	 * 按对象 ID 获取玩家月神消费量。
	 * Returns the player's Luna consume amount by object ID.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 月神消费量 / Luna consume amount
	 */
	public abstract int getPlayerLunaConsumeByObjId(final int playerObjId);
}
