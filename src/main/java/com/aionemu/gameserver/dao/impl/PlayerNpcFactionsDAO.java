package com.aionemu.gameserver.dao.impl;

import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 NPC 阵营 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerNpcFactionsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author MrPoke
 */
@Slf4j
public class PlayerNpcFactionsDAO extends com.aionemu.gameserver.dao.PlayerNpcFactionsDAO {

	/** 查询 NPC 势力 SQL / Select NPC factions SQL*/
	private static final String SELECT_QUERY = "SELECT `faction_id`, `active`, `time`, `state`, `quest_id` FROM player_npc_factions WHERE `player_id`=?";
	/** 插入 NPC 势力 SQL / Insert NPC faction SQL*/
	private static final String INSERT_QUERY = "INSERT INTO player_npc_factions (`player_id`, `faction_id`, `active`, `time`, `state`, `quest_id`) VALUES (?,?,?,?,?,?)";
	/** 更新 NPC 势力 SQL / Update NPC faction SQL*/
	private static final String UPDATE_QUERY = "UPDATE player_npc_factions SET `active`=?, `time`=?, `state`=?, `quest_id`=? WHERE `player_id`=? AND `faction_id`=?";

	private final JdbcClient jdbcClient;

	public PlayerNpcFactionsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	public PlayerNpcFactionsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家 NPC 阵营数据。
	 * Loads NPC faction data for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadNpcFactions(Player player) {
		NpcFactions factions = new NpcFactions(player);

		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.query((rset, rowNum) -> {
					int faction_id = rset.getInt("faction_id");
					boolean active = rset.getBoolean("active");
					int time = rset.getInt("time");
					int questId = rset.getInt("quest_id");
					ENpcFactionQuestState state = ENpcFactionQuestState.valueOf(rset.getString("state"));
					NpcFaction faction = new NpcFaction(faction_id, time, active, state, questId);
					faction.setPersistentState(PersistentState.UPDATED);
					factions.addNpcFaction(faction);
					return null;
				})
				.list();

			player.setNpcFactions(factions);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.165b89344e7f", player.getObjectId(), " from DB"), e);
		}
	}

	/**
	 * 按持久化状态保存玩家 NPC 阵营。
	 * Stores player NPC factions according to their persistent state.
	 * @param player 玩家 / player
	 */
	@Override
	public void storeNpcFactions(Player player) {
		for (NpcFaction npcFaction : player.getNpcFactions().getNpcFactions()) {
			switch (npcFaction.getPersistentState()) {
				case NEW:
					insertNpcFaction(player.getObjectId(), npcFaction);
					break;
				case UPDATE_REQUIRED:
					updateNpcFaction(player.getObjectId(), npcFaction);
					break;
				default:
					continue;
			}
			npcFaction.setPersistentState(PersistentState.UPDATED);
		}
	}

	/**
	 * 插入 NPC 阵营记录。
	 * Inserts an NPC faction record.
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @param faction 阵营 / faction
	 */
	private void insertNpcFaction(int playerObjectId, NpcFaction faction) {
		try {
			jdbcClient.sql(INSERT_QUERY)
				.param(playerObjectId)
				.param(faction.getId())
				.param(faction.isActive())
				.param(faction.getTime())
				.param(faction.getState().name())
				.param(faction.getQuestId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.e4fe3ce16d8e", playerObjectId, " from DB"), e);
		}
	}

	/**
	 * 更新 NPC 阵营记录。
	 * Updates an NPC faction record.
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @param faction 阵营 / faction
	 */
	private void updateNpcFaction(int playerObjectId, NpcFaction faction) {
		try {
			jdbcClient.sql(UPDATE_QUERY)
				.param(faction.isActive())
				.param(faction.getTime())
				.param(faction.getState().name())
				.param(faction.getQuestId())
				.param(playerObjectId)
				.param(faction.getId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.494a98a72b0b", playerObjectId, " from DB"), e);
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
