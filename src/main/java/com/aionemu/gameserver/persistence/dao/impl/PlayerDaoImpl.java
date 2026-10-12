package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData;
import com.aionemu.gameserver.dataholders.PlayerInitialData.LocationData;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerUpgradeArcade;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.model.templates.portal.InstanceExit;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.entity.PlayerPo;
import com.aionemu.gameserver.persistence.mapper.PlayerMapper;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家核心数据访问实现。
 * Core player data access implementation.
 */
@Slf4j
@Service
@Lazy
public class PlayerDaoImpl extends ServiceImpl<PlayerMapper, PlayerPo> implements PlayerDao {

	/** 按对象 ID 缓存的玩家公共数据 / Player common data cache by object ID */
	private final Map<Integer, PlayerCommonData> playerCommonData = new ConcurrentHashMap<>();
	/** 按角色名（小写）缓存的玩家公共数据 / Player common data cache by name (lowercase) */
	private final Map<String, PlayerCommonData> playerCommonDataByName = new ConcurrentHashMap<>();

	/**
	 * 检查角色名是否已使用。
	 * Checks whether a character name is already used.
	 *
	 * @param name 角色名 / character name
	 * @return 若已使用为 true；查询异常时也返回 true / true if used, or on query error
	 */
	@Override
	public boolean isNameUsed(final String name) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create().eq(PlayerPo::getName, name)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b38241da2c4e", name), e);
			return true;
		}
	}

	/**
	 * 按对象 ID 集合批量查询角色名。
	 * Loads player names for the given object IDs.
	 *
	 * @param playerObjectIds 玩家对象 ID 集合 / collection of player object IDs
	 * @return ID 到角色名的映射；入参为空时返回空 Map / map of id to name, empty if input blank
	 */
	@Override
	public Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds) {
		if (GenericValidator.isBlankOrNull(playerObjectIds)) {
			return Collections.emptyMap();
		}

		Map<Integer, String> result = new HashMap<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().select("id", "name").where(PlayerPo::getId).in(playerObjectIds))
				.forEach(po -> result.put(po.getId(), po.getName()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.25f109fb2408"), e);
		}

		return result;
	}

	/**
	 * 将在线玩家的完整数据持久化到数据库，并按配置更新缓存。
	 * Persists a player's full data to the database and refreshes caches when enabled.
	 *
	 * @param player 待保存的玩家 / player to store
	 */
	@Override
	public void storePlayer(final Player player) {
		try {
			log.debug("Storing player {} {}", player.getObjectId(), player.getName());
			PlayerCommonData pcd = player.getCommonData();

			Mailbox mailBox = player.getMailbox();
			int mails = mailBox != null ? mailBox.size() : pcd.getMailboxLetters();

			PlayerUpgradeArcade pua = player.getUpgradeArcade();
			int frenzyPoints = pua != null ? pua.getFrenzyPoints() : 0;
			int frenzyCount = pua != null ? pua.getFrenzyCount() : 0;

			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getName, player.getName())
				.set(PlayerPo::getExp, pcd.getExp())
				.set(PlayerPo::getRecoverexp, pcd.getExpRecoverable())
				.set(PlayerPo::getX, player.getX())
				.set(PlayerPo::getY, player.getY())
				.set(PlayerPo::getZ, player.getZ())
				.set(PlayerPo::getHeading, player.getHeading())
				.set(PlayerPo::getWorldId, player.getWorldId())
				.set(PlayerPo::getGender, player.getGender().toString())
				.set(PlayerPo::getRace, player.getRace().toString())
				.set(PlayerPo::getPlayerClass, pcd.getPlayerClass().toString())
				.set(PlayerPo::getLastOnline, pcd.getLastOnline())
				.set(PlayerPo::getQuestExpands, player.getQuestExpands())
				.set(PlayerPo::getNpcExpands, player.getNpcExpands())
				.set(PlayerPo::getAdvencedStigmaSlotSize, pcd.getAdvancedStigmaSlotSize())
				.set(PlayerPo::getWarehouseSize, player.getWarehouseSize())
				.set(PlayerPo::getNote, pcd.getNote())
				.set(PlayerPo::getTitleId, pcd.getTitleId())
				.set(PlayerPo::getBonusTitleId, pcd.getBonusTitleId())
				.set(PlayerPo::getDp, pcd.getDp())
				.set(PlayerPo::getSoulSickness, pcd.getDeathCount())
				.set(PlayerPo::getMailboxLetters, mails)
				.set(PlayerPo::getReposteEnergy, pcd.getCurrentReposteEnergy())
				.set(PlayerPo::getMentorFlagTime, pcd.getMentorFlagTime())
				.set(PlayerPo::getWorldOwner, player.getPosition().getWorldMapInstance().getOwnerId())
				.set(PlayerPo::getStamps, pcd.getPassportStamps())
				.set(PlayerPo::getRewardedPass, pcd.getPassportReward())
				.set(PlayerPo::getLastStamp, pcd.getLastStamp())
				.set(PlayerPo::getPassportTime, pcd.getPassportTime())
				.set(PlayerPo::getArchdaeva, pcd.isArchDaeva())
				.set(PlayerPo::getCreativityPoint, pcd.getCreativityPoint())
				.set(PlayerPo::getAuraOfGrowth, pcd.getAuraOfGrowth())
				.set(PlayerPo::getJoinLegionId, pcd.getJoinRequestLegionId())
				.set(PlayerPo::getJoinState, pcd.getJoinRequestState().toString())
				.set(PlayerPo::getBerdinStar, pcd.getBerdinStar())
				.set(PlayerPo::getAbyssFavor, pcd.getAbyssFavor())
				.set(PlayerPo::getLunaConsume, pcd.getLunaConsumePoint())
				.set(PlayerPo::getMuniKeys, pcd.getMuniKeys())
				.set(PlayerPo::getLunaConsumeCount, pcd.getLunaConsumeCount())
				.set(PlayerPo::getWardrobeSlot, pcd.getWardrobeSlot())
				.set(PlayerPo::getFrenzyPoints, frenzyPoints)
				.set(PlayerPo::getFrenzyCount, frenzyCount)
				.set(PlayerPo::getTocFloor, pcd.getFloor())
				.set(PlayerPo::getStoneCp, pcd.getStoneCreativityPoint())
				.set(PlayerPo::getGoldenDice, pcd.getGoldenDice())
				.set(PlayerPo::getSweepReset, pcd.getResetBoard())
				.set(PlayerPo::getMinionSkillPoints, pcd.getMinionSkillPoints())
				.set(PlayerPo::getMinionFunctionTime, pcd.getMinionFunctionTime())
				.where(PlayerPo::getId).eq(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.143d7f893e9c", player.getObjectId(), player.getName()), e);
		}

		if (CacheConfig.CACHE_COMMONDATA) {
			playerCommonData.put(player.getObjectId(), player.getCommonData());
			playerCommonDataByName.put(player.getName().toLowerCase(), player.getCommonData());
		}
	}

	@Override
	public void storeInTransaction(Connection connection, int playerId, PlayerCommonData pcd) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(PlayerMapper.UPDATE_QUEST_CURRENCIES_QUERY_JDBC)) {
			stmt.setLong(1, pcd.getExp());
			stmt.setInt(2, pcd.getDp());
			stmt.setInt(3, pcd.getCreativityPoint());
			stmt.setLong(4, pcd.getAbyssFavor());
			stmt.setLong(5, pcd.getAuraOfGrowth());
			stmt.setInt(6, playerId);
			if (stmt.executeUpdate() != 1) {
				throw new SQLException("No player row changed for player " + playerId);
			}
		}
	}

	@Override
	public void promoteArchDaevaInTransaction(Connection connection, int playerId, long minimumExp) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(PlayerMapper.PROMOTE_ARCHDAEVA_QUERY_JDBC)) {
			statement.setLong(1, minimumExp);
			statement.setInt(2, playerId);
			if (statement.executeUpdate() != 1) {
				throw new SQLException("No player row changed for player " + playerId);
			}
		}
	}

	/**
	 * 插入新创建的角色记录，并按配置写入缓存。
	 * Inserts a newly created character and updates caches when enabled.
	 *
	 * @param pcd 新角色公共数据 / new player common data
	 * @param accountId 账号 ID / account id
	 * @param accountName 账号名 / account name
	 * @return 成功为 true，失败为 false / true on success, false on failure
	 */
	@Override
	public boolean saveNewPlayer(final PlayerCommonData pcd, final int accountId, final String accountName) {
		try {
			log.debug("Saving new player: {} {}", pcd.getPlayerObjId(), pcd.getName());

			PlayerPo po = new PlayerPo();
			po.setId(pcd.getPlayerObjId());
			po.setName(pcd.getName());
			po.setAccountId(accountId);
			po.setAccountName(accountName);
			po.setX(pcd.getPosition().getX());
			po.setY(pcd.getPosition().getY());
			po.setZ(pcd.getPosition().getZ());
			po.setHeading(pcd.getPosition().getHeading());
			po.setWorldId(pcd.getPosition().getMapId());
			po.setGender(pcd.getGender().toString());
			po.setRace(pcd.getRace().toString());
			po.setPlayerClass(pcd.getPlayerClass().toString());
			po.setQuestExpands(pcd.getQuestExpands());
			po.setNpcExpands(pcd.getNpcExpands());
			po.setWarehouseSize(pcd.getWarehouseSize());
			po.setBonusTitleId(pcd.getBonusTitleId());
			po.setArchdaeva(pcd.isArchDaeva());
			po.setWardrobeSlot(2);
			po.setOnline(false);
			po.setStamps(0);
			po.setRewardedPass(1);
			mapper.insertSelective(po);

			if (CacheConfig.CACHE_COMMONDATA) {
				playerCommonData.put(pcd.getPlayerObjId(), pcd);
				playerCommonDataByName.put(pcd.getName().toLowerCase(), pcd);
			}

			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fc97fe191787", pcd.getPlayerObjId(), pcd.getName()), e);
			return false;
		}
	}

	/**
	 * 按角色名加载玩家公共数据（优先世界在线与缓存）。
	 * Loads player common data by name (world online and cache first).
	 * @param name 角色名 / character name
	 * @return 公共数据，未找到时为 null / common data, or null if not found
	 */
	@Override
	public PlayerCommonData loadPlayerCommonDataByName(final String name) {
		Player player = GameWorldBootstrapServices.world().findPlayer(name);
		if (player != null) {
			return player.getCommonData();
		}

		PlayerCommonData pcd = playerCommonDataByName.get(name.toLowerCase());
		if (pcd != null) {
			return pcd;
		}

		int playerObjId = 0;
		try {
			PlayerPo po = mapper.selectOneByQuery(QueryWrapper.create().select("id").eq(PlayerPo::getName, name));
			playerObjId = po == null ? 0 : intValue(po.getId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.cbf76d90931d", name), e);
		}

		return playerObjId > 0 ? loadPlayerCommonData(playerObjId) : null;
	}

	/**
	 * 按对象 ID 加载玩家公共数据（优先缓存，必要时回退出生点）。
	 * Loads player common data by object ID (cache first; may fall back to spawn).
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 公共数据，未找到时为 null / common data, or null if not found
	 */
	@Override
	public PlayerCommonData loadPlayerCommonData(final int playerObjId) {
		PlayerCommonData cached = playerCommonData.get(playerObjId);
		if (cached != null) {
			log.debug("PlayerCommonData for id: {} obtained from cache", playerObjId);
			return cached;
		}

		try {
			log.debug("Loading player from db: {}", playerObjId);

			PlayerPo po = mapper.selectOneById(playerObjId);
			if (po == null) {
				return null;
			}

			PlayerCommonData cd = new PlayerCommonData(playerObjId);
			fillCommonData(cd, po);

			if (CacheConfig.CACHE_COMMONDATA) {
				playerCommonData.put(playerObjId, cd);
				playerCommonDataByName.put(cd.getName().toLowerCase(), cd);
			}

			return cd;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ccf8db4780c8", playerObjId), e);
		}

		return null;
	}

	/**
	 * 将角色行数据填充到公共数据对象，并在坐标无效时回退到副本出口或种族出生点。
	 * Fills a common-data object from the player row, falling back to an instance exit or the race spawn
	 * when the stored coordinates are not in any region.
	 *
	 * @param cd 待填充的公共数据 / common data to fill
	 * @param po 角色行 / player row
	 */
	private void fillCommonData(PlayerCommonData cd, PlayerPo po) {
		int playerObjId = cd.getPlayerObjId();
		cd.setName(po.getName());

		try {
			cd.setPlayerClass(PlayerClass.valueOf(po.getPlayerClass()));
		} catch (IllegalArgumentException e) {
			log.warn(I18n.get("log.40c87a64d2f0", playerObjId, po.getPlayerClass()));
			cd.setPlayerClass(PlayerClass.WARRIOR);
		}

		cd.setExp(longValue(po.getExp()), false);
		cd.setRecoverableExp(longValue(po.getRecoverexp()));

		try {
			cd.setRace(Race.valueOf(po.getRace()));
		} catch (IllegalArgumentException e) {
			log.warn(I18n.get("log.1106f1274fe9", playerObjId, po.getRace()));
			cd.setRace(Race.ELYOS);
		}

		try {
			cd.setGender(Gender.valueOf(po.getGender()));
		} catch (IllegalArgumentException e) {
			log.warn(I18n.get("log.2eff1e6d34b2", playerObjId, po.getGender()));
			cd.setGender(Gender.MALE);
		}

		cd.setLastOnline(po.getLastOnline());
		cd.setNote(po.getNote());
		cd.setQuestExpands(intValue(po.getQuestExpands()));
		cd.setNpcExpands(intValue(po.getNpcExpands()));
		cd.setAdvancedStigmaSlotSize(intValue(po.getAdvencedStigmaSlotSize()));
		cd.setTitleId(intValue(po.getTitleId()));
		cd.setBonusTitleId(intValue(po.getBonusTitleId()));
		cd.setWarehouseSize(intValue(po.getWarehouseSize()));
		cd.setOnline(booleanValue(po.getOnline()));
		cd.setMailboxLetters(intValue(po.getMailboxLetters()));
		cd.setDp(intValue(po.getDp()));
		cd.setDeathCount(intValue(po.getSoulSickness()));
		cd.setCurrentReposteEnergy(longValue(po.getReposteEnergy()));

		float x = floatValue(po.getX());
		float y = floatValue(po.getY());
		float z = floatValue(po.getZ());
		byte heading = byteValue(po.getHeading());
		int worldId = intValue(po.getWorldId());

		PlayerInitialData playerInitialData = DataManager.PLAYER_INITIAL_DATA;
		WorldMapInstance mainInstance = GameWorldBootstrapServices.world().getWorldMap(worldId)
			.getMainWorldMapInstance();
		MapRegion mr = mainInstance == null ? null : mainInstance.getRegion(x, y, z);

		if (mr == null) {
			InstanceExit exit = DataManager.INSTANCE_EXIT_DATA == null ? null
				: DataManager.INSTANCE_EXIT_DATA.getInstanceExit(worldId, cd.getRace());
			if (exit != null) {
				x = exit.getX();
				y = exit.getY();
				z = exit.getZ();
				heading = exit.getH();
				worldId = exit.getExitWorld();
			} else if (playerInitialData != null) {
				LocationData ld = playerInitialData.getSpawnLocation(cd.getRace());
				if (ld != null) {
					x = ld.getX();
					y = ld.getY();
					z = ld.getZ();
					heading = ld.getHeading();
					worldId = ld.getMapId();
				}
			}
		}

		WorldPosition position = GameWorldBootstrapServices.world().createPosition(worldId, x, y, z, heading, 0);
		cd.setPosition(position);

		cd.setWorldOwnerId(intValue(po.getWorldOwner()));
		cd.setMentorFlagTime(intValue(po.getMentorFlagTime()));
		cd.setLastTransferTime(longValue(po.getLastTransferTime()));
		cd.setPassportStamps(intValue(po.getStamps()));
		cd.setPassportReward(intValue(po.getRewardedPass()));
		cd.setLastStamp(po.getLastStamp());
		cd.setPassportTime(intValue(po.getPassportTime()));
		cd.setArchDaeva(booleanValue(po.getArchdaeva()));
		cd.setCreativityPoint(intValue(po.getCreativityPoint()));
		cd.addAuraOfGrowth(longValue(po.getAuraOfGrowth()));
		cd.setJoinRequestLegionId(intValue(po.getJoinLegionId()));

		try {
			cd.setJoinRequestState(LegionJoinRequestState.valueOf(po.getJoinState()));
		} catch (IllegalArgumentException e) {
			log.warn(I18n.get("log.2ab35f9897c1", playerObjId, po.getJoinState()));
			cd.setJoinRequestState(LegionJoinRequestState.NONE);
		}

		cd.addBerdinStar(longValue(po.getBerdinStar()));
		cd.addAbyssFavor(longValue(po.getAbyssFavor()));
		cd.setLunaConsumePoint(intValue(po.getLunaConsume()));
		cd.setMuniKeys(intValue(po.getMuniKeys()));
		cd.setLunaConsumeCount(intValue(po.getLunaConsumeCount()));
		cd.setWardrobeSlot(intValue(po.getWardrobeSlot()));

		PlayerUpgradeArcade pua = new PlayerUpgradeArcade();
		pua.setFrenzyPoints(intValue(po.getFrenzyPoints()));
		pua.setFrenzyCount(intValue(po.getFrenzyCount()));

		cd.setFloor(intValue(po.getTocFloor()));
		cd.setStoneCreativityPoint(intValue(po.getStoneCp()));
		cd.setGoldenDice(intValue(po.getGoldenDice()));
		cd.setResetBoard(intValue(po.getSweepReset()));
		cd.setMinionSkillPoints(intValue(po.getMinionSkillPoints()));
		cd.setMinionFunctionTime(po.getMinionFunctionTime());
	}

	/**
	 * 按对象 ID 删除角色，并同步清理缓存。
	 * Deletes a player by object ID and clears related caches.
	 * @param playerId 玩家对象 ID / player object id
	 */
	@Override
	public void deletePlayer(int playerId) {
		try {
			mapper.deleteById(playerId);

			if (CacheConfig.CACHE_COMMONDATA) {
				PlayerCommonData pcd = playerCommonData.remove(playerId);
				if (pcd != null) {
					playerCommonDataByName.remove(pcd.getName().toLowerCase());
				}
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bb5fb346ba48", playerId), e);
		}
	}

	/**
	 * 查询指定账号下全部角色对象 ID。
	 * Returns all player object IDs belonging to an account.
	 * @param accountId 账号 ID / account id
	 * @return 角色 ID 列表；查询异常时为 null / list of player ids, or null on error
	 */
	@Override
	public List<Integer> getPlayerOidsOnAccount(final int accountId) {
		try {
			return mapper.selectListByQuery(QueryWrapper.create().select("id").eq(PlayerPo::getAccountId, accountId))
				.stream()
				.map(po -> intValue(po.getId()))
				.toList();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e310f2d2c9cd", accountId), e);
			return null;
		}
	}

	/**
	 * 从数据库读取并填充账号角色的创建/删除时间。
	 * Loads creation and deletion timestamps into account character data.
	 * @param acData 账号角色数据 / player account data
	 */
	@Override
	public void setCreationDeletionTime(final PlayerAccountData acData) {
		final int playerObjId = acData.getPlayerCommonData().getPlayerObjId();
		try {
			PlayerPo po = mapper.selectOneByQuery(QueryWrapper.create().select("creation_date", "deletion_date")
				.eq(PlayerPo::getId, playerObjId));
			if (po != null) {
				acData.setDeletionDate(po.getDeletionDate());
				acData.setCreationDate(po.getCreationDate());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8949342fe4a2", playerObjId), e);
		}
	}

	/**
	 * 更新角色的删除时间。
	 * Updates a character's deletion timestamp.
	 * @param objectId 玩家对象 ID / player object id
	 * @param deletionDate 删除时间戳 / deletion timestamp
	 */
	@Override
	public void updateDeletionTime(final int objectId, final Timestamp deletionDate) {
		try {
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getDeletionDate, deletionDate)
				.where(PlayerPo::getId).eq(objectId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6528985a7d33", objectId), e);
		}
	}

	/**
	 * 保存角色的创建时间。
	 * Stores a character's creation timestamp.
	 * @param objectId 玩家对象 ID / player object id
	 * @param creationDate 创建时间戳 / creation timestamp
	 */
	@Override
	public void storeCreationTime(final int objectId, final Timestamp creationDate) {
		try {
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getCreationDate, creationDate)
				.where(PlayerPo::getId).eq(objectId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bf6a6ac028ab", objectId), e);
		}
	}

	/**
	 * 获取数据库中所有已使用的角色对象 ID。
	 * Returns all player object IDs currently used in the database.
	 * @return 已用 ID 数组；查询异常时为空数组 / array of used ids, empty on error
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<PlayerPo> players = mapper.selectListByQuery(QueryWrapper.create().select("id"));

			int[] result = new int[players.size()];
			for (int i = 0; i < players.size(); i++) {
				result[i] = intValue(players.get(i).getId());
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ce9d363cd21c"), e);
			return new int[0];
		}
	}

	/**
	 * 更新单个玩家的在线状态。
	 * Updates a single player's online status flag.
	 * @param player 玩家 / player
	 * @param online 是否在线 / whether online
	 */
	@Override
	public void onlinePlayer(final Player player, final boolean online) {
		try {
			log.debug("Setting online status {} {}", player.getObjectId(), player.getName());
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getOnline, online)
				.where(PlayerPo::getId).eq(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.91f6d0189d5c", player.getObjectId()), e);
		}
	}

	/**
	 * 批量设置全部玩家的在线状态（通常用于服务器启动/关闭）。
	 * Sets the online flag for all players (typically on server start/shutdown).
	 * @param online 目标在线状态 / target online flag
	 */
	@Override
	public void setPlayersOffline(final boolean online) {
		try {
			mapper.setAllOnline(online);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d115134bf89a"), e);
		}
	}

	/**
	 * 按对象 ID 查询角色名。
	 * Looks up a character name by object ID.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 角色名，未找到时为 null / name, or null if not found
	 */
	@Override
	public String getPlayerNameByObjId(final int playerObjId) {
		try {
			PlayerPo po = mapper.selectOneByQuery(QueryWrapper.create().select("name").eq(PlayerPo::getId, playerObjId));
			return po == null ? null : po.getName();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0b502d634f86", playerObjId), e);
			return null;
		}
	}

	/**
	 * 按角色名查询对象 ID。
	 * Looks up a player object ID by character name.
	 * @param playerName 角色名 / character name
	 * @return 对象 ID，未找到或异常时为 0 / object id, or 0 if missing/error
	 */
	@Override
	public int getPlayerIdByName(final String playerName) {
		try {
			PlayerPo po = mapper.selectOneByQuery(QueryWrapper.create().select("id").eq(PlayerPo::getName, playerName));
			return po == null ? 0 : intValue(po.getId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f273cc47e33d", playerName), e);
			return 0;
		}
	}

	/**
	 * 按角色名查询所属账号 ID。
	 * Looks up the account ID for a character name.
	 * @param name 角色名 / character name
	 * @return 账号 ID，未找到或异常时为 0 / account id, or 0 if missing/error
	 */
	@Override
	public int getAccountIdByName(final String name) {
		try {
			PlayerPo po = mapper.selectOneByQuery(QueryWrapper.create().select("account_id").eq(PlayerPo::getName, name));
			return po == null ? 0 : intValue(po.getAccountId());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b4979008052c", name), e);
			return 0;
		}
	}

	/**
	 * 持久化角色名变更。
	 * Persists a player name change.
	 * @param recipientCommonData 含新名称的公共数据 / common data holding the new name
	 */
	@Override
	public void storePlayerName(final PlayerCommonData recipientCommonData) {
		try {
			log.debug("Storing playerName {} {}", recipientCommonData.getPlayerObjId(), recipientCommonData.getName());

			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getName, recipientCommonData.getName())
				.where(PlayerPo::getId).eq(recipientCommonData.getPlayerObjId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ecfd38bef3fc", recipientCommonData.getPlayerObjId(),
					recipientCommonData.getName()), e);
		}
	}

	/**
	 * 统计账号下未删除（或删除时间未到）的角色数量。
	 * Counts non-deleted (or not-yet-deleted) characters on an account.
	 * @param accountId 账号 ID / account id
	 * @return 角色数量 / character count
	 */
	@Override
	public int getCharacterCountOnAccount(final int accountId) {
		try {
			return mapper.countCharactersOnAccount(accountId);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fe988f5216b0", accountId), e);
			return 0;
		}
	}

	/**
	 * 按种族统计达到最低等级经验阈值的去重账号数。
	 * Counts distinct accounts of a race that meet the minimum level exp threshold.
	 * @param race 阵营 / race
	 * @return 账号数量 / account count
	 */
	@Override
	public int getCharacterCountForRace(Race race) {
		try {
			return mapper.countAccountsForRace(race.name(),
				DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(GSConfig.RATIO_MIN_REQUIRED_LEVEL));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6a4af16dff6d", race), e);
			return 0;
		}
	}

	/**
	 * 统计当前标记为在线的玩家数量。
	 * Counts players currently marked as online.
	 * @return 在线玩家数 / online player count
	 */
	@Override
	public int getOnlinePlayerCount() {
		try {
			return (int) mapper.selectCountByQuery(QueryWrapper.create().eq(PlayerPo::getOnline, true));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7cb1629e7919"), e);
			return 0;
		}
	}

	/**
	 * 查询因长时间未登录而待删除的角色 ID 列表。
	 * Lists player IDs inactive long enough to be deleted.
	 * @param daysOfInactivity 不活跃天数阈值 / inactivity threshold in days
	 * @param limitation 最大返回条数，&lt;=0 表示不限制 / max rows, &lt;=0 for no limit
	 * @return 待删除角色 ID 列表 / list of player ids to delete
	 */
	@Override
	public List<Integer> getPlayersToDelete(final int daysOfInactivity, int limitation) {
		try {
			return limitation > 0 ? mapper.selectPlayersToDeleteLimited(daysOfInactivity, limitation)
				: mapper.selectPlayersToDelete(daysOfInactivity);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e40ff813df6d"), e);
			return new ArrayList<>();
		}
	}

	/**
	 * 设置玩家上次转服时间。
	 * Sets a player's last server-transfer time.
	 * @param playerId 玩家对象 ID / player object id
	 * @param time 转服时间戳 / transfer timestamp
	 */
	@Override
	public void setPlayerLastTransferTime(final int playerId, final long time) {
		try {
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getLastTransferTime, time)
				.where(PlayerPo::getId).eq(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.557725f5c465", playerId), e);
		}
	}

	/**
	 * 更新玩家的军团加入申请状态。
	 * Updates a player's legion join-request state.
	 * @param playerId 玩家对象 ID / player object id
	 * @param state 申请状态 / join request state
	 */
	@Override
	public void updateLegionJoinRequestState(final int playerId, final LegionJoinRequestState state) {
		try {
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getJoinState, state.name())
				.where(PlayerPo::getId).eq(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ae369018dad0", playerId), e);
		}
	}

	/**
	 * 清除玩家的军团加入申请（军团 ID 置 0，状态置 NONE）。
	 * Clears a player's legion join request (legion id 0, state NONE).
	 * @param playerId 玩家对象 ID / player object id
	 */
	@Override
	public void clearJoinRequest(final int playerId) {
		try {
			UpdateChain.of(PlayerPo.class)
				.set(PlayerPo::getJoinLegionId, 0)
				.set(PlayerPo::getJoinState, "NONE")
				.where(PlayerPo::getId).eq(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c8dcb65f0a0c", playerId), e);
		}
	}

	/**
	 * 从数据库读取并写入玩家的军团加入申请状态。
	 * Loads the legion join-request state from DB into the player.
	 * @param player 目标玩家 / target player
	 */
	@Override
	public void getJoinRequestState(final Player player) {
		try {
			PlayerPo po = mapper.selectOneByQuery(
				QueryWrapper.create().select("join_state").eq(PlayerPo::getId, player.getObjectId()));
			if (po == null) {
				return;
			}

			try {
				player.getCommonData().setJoinRequestState(LegionJoinRequestState.valueOf(po.getJoinState()));
			} catch (IllegalArgumentException e) {
				log.warn(I18n.get("log.2ab35f9897c1", player.getObjectId(), po.getJoinState()));
				player.getCommonData().setJoinRequestState(LegionJoinRequestState.NONE);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.01aadaeb4af5", player.getObjectId()), e);
		}
	}

	/**
	 * 可空整数列的空值归一化，保持原 rs.getInt 读取语义（NULL 视为 0）。
	 * Normalizes nullable integer columns on read, keeping the legacy rs.getInt semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的整数 / normalized int
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}

	/**
	 * 可空长整型列的空值归一化，保持原 rs.getLong 读取语义（NULL 视为 0）。
	 * Normalizes nullable long columns on read, keeping the legacy rs.getLong semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的长整型 / normalized long
	 */
	private static long longValue(Long value) {
		return value == null ? 0L : value;
	}

	/**
	 * 可空浮点列的空值归一化，保持原 rs.getFloat 读取语义（NULL 视为 0）。
	 * Normalizes nullable float columns on read, keeping the legacy rs.getFloat semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的浮点数 / normalized float
	 */
	private static float floatValue(Float value) {
		return value == null ? 0f : value;
	}

	/**
	 * 可空字节列的空值归一化，保持原 rs.getByte 读取语义（NULL 视为 0）。
	 * Normalizes nullable byte columns on read, keeping the legacy rs.getByte semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的字节 / normalized byte
	 */
	private static byte byteValue(Byte value) {
		return value == null ? 0 : value;
	}

	/**
	 * 可空布尔列的空值归一化，保持原 rs.getBoolean 读取语义（NULL 视为 false）。
	 * Normalizes nullable boolean columns on read, keeping the legacy rs.getBoolean semantics (NULL becomes false).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的布尔 / normalized boolean
	 */
	private static boolean booleanValue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}
}
